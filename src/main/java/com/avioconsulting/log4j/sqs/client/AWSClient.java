package com.avioconsulting.log4j.sqs.client;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.buffered.QueueBufferConfig;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class AWSClient {

	private AmazonSQSBufferedAsyncClient client;
	private String awsRegion;
	private String awsAccessKey;
	private String awsSecretKey;
	private long maxBatchOpenMs;
	private int maxBatchSize;
	private int maxInflightOutboundBatches;
	private static final Logger logger = LogManager.getLogger(AWSClient.class);

	public AWSClient(String awsRegion, String awsAccessKey, String awsSecretKey, Integer maxBatchOpenMs, Integer maxBatchSize,
			Integer maxInflightOutboundBatches) {
		this.awsRegion = awsRegion;
		this.awsAccessKey = awsAccessKey;
		this.awsSecretKey = awsSecretKey;
		this.maxBatchOpenMs = maxBatchOpenMs == null ? 200 : maxBatchOpenMs;
		this.maxBatchSize = maxBatchSize == null ? 10 : maxBatchSize;
		this.maxInflightOutboundBatches = maxInflightOutboundBatches == null ? 5 : maxInflightOutboundBatches;
		this.initSQSClient();
	}

	private AmazonSQSBufferedAsyncClient initSQSClient() {
		if (client == null) {
			AmazonSQSAsync asyncClient;
			try {
				logger.debug("Initializing SQS Client: " + this);
				AWSStaticCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey));
				AmazonSQSAsyncClientBuilder clientBuilder = AmazonSQSAsyncClientBuilder.standard();
				asyncClient = clientBuilder.withRegion(awsRegion).withCredentials(credentialsProvider).build();
				QueueBufferConfig config = new QueueBufferConfig().withMaxBatchOpenMs(maxBatchOpenMs).withMaxBatchSize(maxBatchSize).withMaxInflightOutboundBatches(maxInflightOutboundBatches);
				client = new AmazonSQSBufferedAsyncClient(asyncClient, config);
			} catch (Exception e) {
				logger.error("Failed to initialize SQS Client", e);
				throw e;
			}
		}

		return client;
	}

	public String getQueueURL(String queueName){
		return this.client.getQueueUrl(queueName).getQueueUrl();
	}

	public void sendMessages(List<SendMessageRequest> messageList) {
		messageList.forEach(message -> {
		this.client.sendMessageAsync(message);
	});

	}
}
