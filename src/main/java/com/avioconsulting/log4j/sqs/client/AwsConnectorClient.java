package com.avioconsulting.log4j.sqs.client;

import com.amazon.sqs.javamessaging.AmazonSQSExtendedClient;
import com.amazon.sqs.javamessaging.ExtendedClientConfiguration;
import com.avioconsulting.log4j.sqs.SqsLimits;
import com.avioconsulting.log4j.sqs.processor.ProcessorType;
import com.avioconsulting.log4j.sqs.wrapper.S3ObjectRequest;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.batchmanager.BatchOverrideConfiguration;
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AWS SDK v2 implementation of {@link ConnectorClient}.
 * <p>
 * Messages up to {@link SqsLimits#MAX_BATCHED_MESSAGE_BYTES} are sent through an SqsAsyncBatchManager.
 * Bigger messages, up to {@link SqsLimits#MAX_MESSAGE_BYTES}, are sent with a direct SendMessage call,
 * because the batch manager still caps a batch at 256 KiB.
 * <p>
 * The AWS clients are created on the first send, not while Log4j is being configured. The SDK HTTP
 * client (Netty) logs through Log4j while it starts, and starting it during configuration can load
 * this appender again before Netty has finished initializing.
 */
public class AwsConnectorClient implements ConnectorClient {

    static final int DEFAULT_MAX_BATCH_OPEN_MS = 200;
    static final int DEFAULT_MAX_BATCH_SIZE = SqsLimits.MAX_BATCH_SIZE;
    private static final long ERROR_REPORT_INTERVAL_MS = 10_000;
    private static final int MAX_ERROR_TEXT_LENGTH = 500;

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final ConnectorClientAttributes attributes;
    private final String largeMessageMode;
    private final Object lock = new Object();
    private volatile Clients clients;
    private volatile boolean closed;
    private final ConcurrentMap<String, String> queueUrls = new ConcurrentHashMap<>();
    private final Set<CompletableFuture<?>> pendingSends = ConcurrentHashMap.newKeySet();
    private final AtomicLong lastErrorReportMs = new AtomicLong();
    private final AtomicLong suppressedErrors = new AtomicLong();

    public AwsConnectorClient(ConnectorClientAttributes attributes, String largeMessageMode) {
        this.attributes = attributes;
        this.largeMessageMode = largeMessageMode;
    }

    @Override
    public void sendMessage(String queueName, SendMessageRequest request) {
        Clients aws = clients();
        SendMessageRequest requestWithUrl = request.toBuilder().queueUrl(getQueueUrl(aws, queueName)).build();
        if (requestWithUrl.messageGroupId() != null) {
            sendFifoMessage(aws, queueName, requestWithUrl);
            return;
        }
        int bodyBytes = requestWithUrl.messageBody().getBytes(StandardCharsets.UTF_8).length;
        CompletableFuture<SendMessageResponse> future = aws.batchManager != null && bodyBytes <= SqsLimits.MAX_BATCHED_MESSAGE_BYTES
                ? aws.batchManager.sendMessage(requestWithUrl)
                : aws.sqsAsyncClient.sendMessage(requestWithUrl);
        track(future, queueName);
    }

    /**
     * Sends one FIFO part and waits until SQS accepts it. Parts of one message share a group id, and SQS keeps
     * that order only when the previous part has been accepted before the next one is sent.
     */
    private void sendFifoMessage(Clients aws, String queueName, SendMessageRequest request) {
        try {
            aws.sqsAsyncClient.sendMessage(request).join();
        } catch (RuntimeException e) {
            reportSendFailure(queueName, e);
            throw e;
        }
    }

    @Override
    public void sendExtendedMessage(String queueName, SendMessageRequest request) {
        Clients aws = clients();
        if (aws.sqsExtendedClient == null) {
            throw new IllegalStateException("SQS Extended Client is only available in EXTENDED largeMessageMode");
        }
        aws.sqsExtendedClient.sendMessage(request.toBuilder().queueUrl(getQueueUrl(aws, queueName)).build());
    }

    @Override
    public void putObject(S3ObjectRequest request) {
        Clients aws = clients();
        if (aws.s3Client == null) {
            throw new IllegalStateException("S3 client is only available in S3 or EXTENDED largeMessageMode");
        }
        aws.s3Client.putObject(request.getPutObjectRequest(), RequestBody.fromString(request.getContent(), StandardCharsets.UTF_8));
    }

    /**
     * Runs the whole shutdown (batch flush, in-flight sends, client cleanup) on a daemon thread, so one
     * deadline bounds it. The SDK batch manager and the Netty client each have their own internal waits.
     */
    @Override
    public boolean close(long timeoutMillis) {
        Clients aws;
        synchronized (lock) {
            closed = true;
            aws = clients;
            clients = null;
        }
        if (aws == null) {
            return true;
        }
        CompletableFuture<Void> shutdown = new CompletableFuture<>();
        Thread thread = new Thread(() -> {
            try {
                shutdown(aws);
                shutdown.complete(null);
            } catch (Throwable t) {
                shutdown.completeExceptionally(t);
            }
        }, "log4j2-sqs-appender-shutdown");
        thread.setDaemon(true);
        thread.start();
        try {
            shutdown.get(timeoutMillis, TimeUnit.MILLISECONDS);
            return true;
        } catch (TimeoutException e) {
            LOGGER.warn("SQS appender did not stop within {} ms. {} log messages may not have been sent.",
                    timeoutMillis, pendingSends.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOGGER.warn("SQS appender failed while stopping: {}", describe(e));
        }
        return false;
    }

    private void shutdown(Clients aws) {
        if (aws.batchManager != null) {
            aws.batchManager.close();
        }
        CompletableFuture<?>[] pending = pendingSends.toArray(new CompletableFuture<?>[0]);
        try {
            CompletableFuture.allOf(pending).join();
        } catch (CompletionException | CancellationException e) {
            // Individual failures are already reported by track().
        }
        aws.close();
    }

    private Clients clients() {
        Clients aws = clients;
        if (aws != null) {
            return aws;
        }
        synchronized (lock) {
            if (closed) {
                throw new IllegalStateException("SQS appender is stopped");
            }
            if (clients == null) {
                clients = new Clients(attributes, largeMessageMode);
            }
            return clients;
        }
    }

    private String getQueueUrl(Clients aws, String queueName) {
        return queueUrls.computeIfAbsent(queueName, name -> aws.sqsAsyncClient
                .getQueueUrl(GetQueueUrlRequest.builder().queueName(name).build())
                .join()
                .queueUrl());
    }

    private void track(CompletableFuture<?> future, String queueName) {
        pendingSends.add(future);
        future.whenComplete((response, throwable) -> {
            pendingSends.remove(future);
            if (throwable != null) {
                reportSendFailure(queueName, throwable);
            }
        });
    }

    /**
     * Reports failed sends to the Log4j StatusLogger, never to a regular logger, so a failure cannot be
     * routed back into this appender. Reports are throttled and never include the message body.
     */
    private void reportSendFailure(String queueName, Throwable throwable) {
        long now = System.currentTimeMillis();
        long last = lastErrorReportMs.get();
        if (now - last < ERROR_REPORT_INTERVAL_MS || !lastErrorReportMs.compareAndSet(last, now)) {
            suppressedErrors.incrementAndGet();
            return;
        }
        LOGGER.error("Failed to send log message to SQS queue {}: {} ({} similar failures suppressed)",
                queueName, describe(throwable), suppressedErrors.getAndSet(0));
    }

    static int batchSize(Integer maxBatchSize) {
        if (maxBatchSize == null) {
            return DEFAULT_MAX_BATCH_SIZE;
        }
        return Math.max(1, Math.min(SqsLimits.MAX_BATCH_SIZE, maxBatchSize));
    }

    static String describe(Throwable throwable) {
        Throwable cause = throwable;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        String text = cause.getClass().getName() + ": " + cause.getMessage();
        return text.length() > MAX_ERROR_TEXT_LENGTH ? text.substring(0, MAX_ERROR_TEXT_LENGTH) + "..." : text;
    }

    private static URI toEndpoint(String endpointURL) {
        if (endpointURL == null || endpointURL.trim().isEmpty()) {
            return null;
        }
        return URI.create(endpointURL.trim());
    }

    /**
     * The AWS SDK clients used by one appender.
     */
    private static final class Clients {
        private final SqsAsyncClient sqsAsyncClient;
        private final SqsAsyncBatchManager batchManager;
        private final ScheduledExecutorService batchExecutor;
        private final S3Client s3Client;
        private final SqsClient sqsSyncClient;
        private final AmazonSQSExtendedClient sqsExtendedClient;

        private Clients(ConnectorClientAttributes attributes, String largeMessageMode) {
            URI endpoint = toEndpoint(attributes.getEndpointURL());

            SqsAsyncClientBuilder sqsAsyncBuilder = SqsAsyncClient.builder()
                    .region(attributes.getAwsRegion())
                    .credentialsProvider(attributes.getCredentialsProvider());
            if (endpoint != null) {
                sqsAsyncBuilder.endpointOverride(endpoint);
            }
            this.sqsAsyncClient = sqsAsyncBuilder.build();

            int maxBatchSize = batchSize(attributes.getMaxBatchSize());
            int maxBatchOpenMs = attributes.getMaxBatchOpenMs() == null ? DEFAULT_MAX_BATCH_OPEN_MS : attributes.getMaxBatchOpenMs();
            if (maxBatchSize > 1 && maxBatchOpenMs > 0) {
                this.batchExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "log4j2-sqs-appender-batch");
                    thread.setDaemon(true);
                    return thread;
                });
                this.batchManager = SqsAsyncBatchManager.builder()
                        .client(sqsAsyncClient)
                        .scheduledExecutor(batchExecutor)
                        .overrideConfiguration(BatchOverrideConfiguration.builder()
                                .maxBatchSize(maxBatchSize)
                                .sendRequestFrequency(Duration.ofMillis(maxBatchOpenMs))
                                .build())
                        .build();
            } else {
                this.batchExecutor = null;
                this.batchManager = null;
            }

            boolean extended = ProcessorType.EXTENDED.name().equals(largeMessageMode);
            if (extended || ProcessorType.S3.name().equals(largeMessageMode)) {
                S3ClientBuilder s3Builder = S3Client.builder()
                        .region(attributes.getAwsRegion())
                        .credentialsProvider(attributes.getCredentialsProvider());
                if (endpoint != null) {
                    // Path-style keeps bucket names out of the host, so one endpoint URL works for
                    // both SQS and S3 (LocalStack, or a private endpoint).
                    s3Builder.endpointOverride(endpoint).forcePathStyle(true);
                }
                this.s3Client = s3Builder.build();
            } else {
                this.s3Client = null;
            }

            if (extended) {
                SqsClientBuilder sqsSyncBuilder = SqsClient.builder()
                        .region(attributes.getAwsRegion())
                        .credentialsProvider(attributes.getCredentialsProvider());
                if (endpoint != null) {
                    sqsSyncBuilder.endpointOverride(endpoint);
                }
                this.sqsSyncClient = sqsSyncBuilder.build();
                // Only messages bigger than maxMessageBytes reach the extended client, so all of them go to S3.
                ExtendedClientConfiguration extendedClientConfiguration = new ExtendedClientConfiguration()
                        .withPayloadSupportEnabled(s3Client, attributes.getS3BucketName())
                        .withAlwaysThroughS3(true);
                this.sqsExtendedClient = new AmazonSQSExtendedClient(sqsSyncClient, extendedClientConfiguration);
            } else {
                this.sqsSyncClient = null;
                this.sqsExtendedClient = null;
            }
        }

        private void close() {
            if (batchExecutor != null) {
                batchExecutor.shutdownNow();
            }
            if (sqsExtendedClient != null) {
                sqsExtendedClient.close();
            }
            if (sqsSyncClient != null) {
                sqsSyncClient.close();
            }
            if (s3Client != null) {
                s3Client.close();
            }
            sqsAsyncClient.close();
        }
    }
}
