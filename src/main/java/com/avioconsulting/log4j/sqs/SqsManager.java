package com.avioconsulting.log4j.sqs;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.buffered.QueueBufferConfig;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.config.Configuration;

import java.io.IOException;
import java.util.Objects;

public class SqsManager extends AbstractManager {
    private final Configuration configuration;
    private final String awsAccessKey;
    private final String awsSecretKey;
    private final String awsRegion;
    private final String queueName;
    private final Integer maxBatchOpenMs;
    private final Integer maxBatchSize;
    private final Integer maxInflightOutboundBatches;
    private String queueUrl;
    private AmazonSQSBufferedAsyncClient client;


    protected SqsManager(final Configuration configuration,
                         final LoggerContext loggerContext,
                         final String name,
                         final String awsRegion,
                         final String awsAccessKey,
                         final String awsSecretKey,
                         final String queueName,
                         final Integer maxBatchOpenMs,
                         final Integer maxBatchSize,
                         final Integer maxInflightOutboundBatches) {
        super(loggerContext, name);
        this.configuration = Objects.requireNonNull(configuration);
        this.awsAccessKey = awsAccessKey;
        this.awsSecretKey = awsSecretKey;
        this.awsRegion = awsRegion;
        this.queueName = queueName;
        this.maxBatchOpenMs = maxBatchOpenMs == null ? 200 : maxBatchOpenMs;
        this.maxBatchSize = maxBatchSize == null ? 10 : maxBatchSize;
        this.maxInflightOutboundBatches = maxInflightOutboundBatches == null ? 5 : maxInflightOutboundBatches;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void startup() {
        // This default implementation does nothing
    }

    public void send(final Layout<?> layout, final LogEvent event) throws IOException {
        try {
            String message = new String(layout.toByteArray(event), "UTF-8");
            SendMessageRequest request = new SendMessageRequest(this.queueUrl, message);
            this.getClient().sendMessageAsync(request);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to send message to SQS");
        }
    }

     private AmazonSQSBufferedAsyncClient getClient() {
        if(this.client == null) {
            AmazonSQSAsync asyncClient;
            try {
                System.out.println("Initializing SQS Client: " + this.toString());
                AWSStaticCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey));
                AmazonSQSAsyncClientBuilder clientBuilder = AmazonSQSAsyncClientBuilder.standard();
                asyncClient = clientBuilder.withRegion(awsRegion).withCredentials(credentialsProvider).build();
                QueueBufferConfig config = new QueueBufferConfig().withMaxBatchOpenMs(maxBatchOpenMs).withMaxBatchSize(maxBatchSize).withMaxInflightOutboundBatches(maxInflightOutboundBatches);
                this.client = new AmazonSQSBufferedAsyncClient(asyncClient, config);
                this.queueUrl = this.client.getQueueUrl(this.queueName).getQueueUrl();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Failed to initialize SQS Client");
                throw e;
            }
        }

        return this.client;
     }

     public String toString() {
        return "[ region=" + this.awsRegion + ", maxBatchOpenMs=" + this.maxBatchOpenMs + ", maxBatchSize=" + this.maxBatchSize + ", maxInflightOutboundBatches=" + this.maxInflightOutboundBatches + " ]";
     }
}