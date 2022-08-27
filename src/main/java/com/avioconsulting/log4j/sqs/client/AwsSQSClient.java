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

public class AwsSQSClient implements AvioAWSClient{

	private AmazonSQSBufferedAsyncClient client;
	private static final Logger logger = LogManager.getLogger(AwsSQSClient.class);

	public AwsSQSClient(AvioAWSClientAttributes attributes) {
		/*Check this
		this.maxBatchOpenMs = maxBatchOpenMs == null ? 200 : maxBatchOpenMs;
		this.maxBatchSize = maxBatchSize == null ? 10 : maxBatchSize;
		this.maxInflightOutboundBatches = maxInflightOutboundBatches == null ? 5 : maxInflightOutboundBatches;*/
		this.initSQSClient(attributes);
	}

	private void initSQSClient(AvioAWSClientAttributes attributes) {
		if (client == null) {
			AmazonSQSAsync asyncClient;
			try {
				logger.debug("Initializing SQS Client");
				AWSStaticCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(
						new BasicAWSCredentials(attributes.getAwsAccessKey(), attributes.getAwsSecretKey()));
				AmazonSQSAsyncClientBuilder clientBuilder = AmazonSQSAsyncClientBuilder.standard();
				asyncClient = clientBuilder.withRegion(attributes.getAwsRegion())
						.withCredentials(credentialsProvider)
						.build();
				QueueBufferConfig config = new QueueBufferConfig().withMaxBatchOpenMs(attributes.getMaxBatchOpenMs())
						.withMaxBatchSize(attributes.getMaxBatchSize())
						.withMaxInflightOutboundBatches(attributes.getMaxInflightOutboundBatches());
				client = new AmazonSQSBufferedAsyncClient(asyncClient, config);
			} catch (Exception e) {
				logger.error("Failed to initialize SQS Client", e);
				throw e;
			}
		}
	}

	public String getQueueURL(String queueName){
		return this.client.getQueueUrl(queueName).getQueueUrl();
	}

	public void sendMessages(List<SendMessageRequest> messageList) {
		//this.client.sendMessageAsync(messageList.get(0));
		messageList.forEach(message -> this.client.sendMessageAsync(message));
	}
}
