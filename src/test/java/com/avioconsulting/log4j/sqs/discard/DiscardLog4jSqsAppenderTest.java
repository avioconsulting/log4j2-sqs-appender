package com.avioconsulting.log4j.sqs.discard;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.buffered.QueueBufferConfig;
import com.amazonaws.services.sqs.model.*;
import com.avioconsulting.log4j.sqs.processor.TruncateMessageProcessor;
import com.avioconsulting.log4j.sqs.util.PropertiesProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.avioconsulting.log4j.sqs.util.PropertiesProvider.*;
import static org.junit.Assert.assertEquals;

public class DiscardLog4jSqsAppenderTest {

    private static final Logger logger = LogManager.getLogger(DiscardLog4jSqsAppenderTest.class.getName());
    private final static String MESSAGE_TO_LOG = "TRUNCATE21234567890123456789012345678901234567890123456789012345" +
            "67890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
            "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345" +
            "6789012345678901234567890123456789012345678901234567890END";
    private static AmazonSQSBufferedAsyncClient awsSQSAsyncClient;
    private static String DISCARDED_MESSAGES_QUEUE_URL;

    @BeforeClass
    public static void getProperties() {
        PropertiesProvider.readProperties();
        getConnectionToSQSAsyncClient();
        createQueue();
    }
    @AfterClass
    public static void deleteMessagesFromQueue() {
        deleteMessagesFromSQS();
        deleteQueue();
        awsSQSAsyncClient.shutdown();
    }

    private static void deleteMessagesFromSQS() {
        PurgeQueueRequest purgeQueueRequest = new PurgeQueueRequest(DISCARDED_MESSAGES_QUEUE_URL);
        awsSQSAsyncClient.purgeQueue(purgeQueueRequest);
    }

    private static void getConnectionToSQSAsyncClient() {
            AWSStaticCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(
                    new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY));
            AmazonSQSAsync asyncClient;
            AmazonSQSAsyncClientBuilder clientBuilder = AmazonSQSAsyncClientBuilder.standard();
            asyncClient = clientBuilder.withRegion(String.valueOf(AWS_REGION))
                    .withCredentials(credentialsProvider)
                    .build();
            QueueBufferConfig config = new QueueBufferConfig().withMaxBatchOpenMs(MAX_BATCH_OPEN_MS)
                    .withMaxBatchSize(MAX_BATCH_SIZE)
                    .withMaxInflightOutboundBatches(MAX_INFLIGHT_OUTBOUND_BATCHES);
            awsSQSAsyncClient = new AmazonSQSBufferedAsyncClient(asyncClient, config);
    }

    private static void createQueue() {
        CreateQueueResult result = awsSQSAsyncClient.createQueue(AWS_SQS_QUEUE_NAME + "-discard");
        DISCARDED_MESSAGES_QUEUE_URL = result.getQueueUrl();
    }

    private static void deleteQueue() {
        awsSQSAsyncClient.deleteQueue(DISCARDED_MESSAGES_QUEUE_URL);
    }

    @Test
    public void test_discardedLogMessagesQuantity_in_sqs() throws ExecutionException, InterruptedException {
        logger.info(MESSAGE_TO_LOG);
        logger.info(MESSAGE_TO_LOG);
        awsSQSAsyncClient.flush();
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest()
                .withQueueUrl(DISCARDED_MESSAGES_QUEUE_URL)
                .withWaitTimeSeconds(160)
                .withMaxNumberOfMessages(1);
        awsSQSAsyncClient.flush();
        Future<ReceiveMessageResult> messageResultFuture = awsSQSAsyncClient.receiveMessageAsync(receiveMessageRequest);
        while (!messageResultFuture.isDone()) {
            if (messageResultFuture.isDone()) {
                String queuedMessageBody = messageResultFuture.get().getMessages().get(0).getBody();
                assertEquals(queuedMessageBody, "Can't display log message. Message max size exceeded. max size:256");
            }
        }

    }

    private String truncateLoggedString() {
        //TODO get maxsize from parameters
        return TruncateMessageProcessor.truncateStringByByteLength("[INFO] - " + MESSAGE_TO_LOG, StandardCharsets.UTF_8.name(), 256);
    }

    private Integer getMessagesQuantityFromSQSQueue() {
        GetQueueAttributesResult getQueueAttributesResult =
                awsSQSAsyncClient.getQueueAttributes(DISCARDED_MESSAGES_QUEUE_URL, Collections.singletonList("ApproximateNumberOfMessages"));
        return Integer.valueOf(getQueueAttributesResult.getAttributes().get("ApproximateNumberOfMessages"));
    }

}
