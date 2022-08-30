package com.avioconsulting.log4j.sqs.client;

import com.amazon.sqs.javamessaging.AmazonSQSExtendedClient;
import com.amazon.sqs.javamessaging.ExtendedClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.buffered.QueueBufferConfig;
import com.avioconsulting.log4j.sqs.processor.ProcessorType;
import com.avioconsulting.log4j.sqs.wrapper.MessageRequestWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConnectorClient {

	private AmazonS3 awsS3Client;
	private AmazonSQS awsSQSExtendedClient;
	private AmazonSQSBufferedAsyncClient awsSQSAsyncClient;
	private String queueUrl;
	private String largeMessageQueueUrl;
	public static final Logger classLogger = LogManager.getLogger(ConnectorClient.class);

	public ConnectorClient(ConnectorClientAttributes attributes) {
		this.initS3Client(attributes);
		this.initSqsExtendedClient(attributes);
		this.initSQSClient(attributes);
	}

	private void initS3Client(ConnectorClientAttributes attributes) {
		if(awsS3Client == null) {
			try {
				this.awsS3Client =  AmazonS3ClientBuilder
						.standard()
						.withRegion(String.valueOf(attributes.getAwsRegion()))
						.withCredentials(new AWSStaticCredentialsProvider(attributes.getCredentialsProvider().getCredentials()))
						.build();
			} catch (Exception e) {
				classLogger.error("Failed to initialize S3 AWS Client", e);
			}

		}
	}

	private void initSqsExtendedClient(ConnectorClientAttributes attributes) {
		if(awsSQSExtendedClient == null) {
			final ExtendedClientConfiguration extendedClientConfig =
					new ExtendedClientConfiguration()
							.withLargePayloadSupportEnabled(awsS3Client, attributes.getS3BucketName());
			awsSQSExtendedClient =
					new AmazonSQSExtendedClient(AmazonSQSAsyncClientBuilder.standard().
							withRegion(String.valueOf(attributes.getAwsRegion())).
							withCredentials(attributes.getCredentialsProvider()).build(),
							extendedClientConfig);
		}
	}

	private void initSQSClient(ConnectorClientAttributes attributes) {
		if (this.awsSQSAsyncClient == null) {
			try {
				AmazonSQSAsync asyncClient;
				classLogger.debug("Initializing SQS Client");
				AmazonSQSAsyncClientBuilder clientBuilder = AmazonSQSAsyncClientBuilder.standard();
				asyncClient = clientBuilder.withRegion(String.valueOf(attributes.getAwsRegion()))
						.withCredentials(attributes.getCredentialsProvider())
						.build();
				QueueBufferConfig config = new QueueBufferConfig().withMaxBatchOpenMs(attributes.getMaxBatchOpenMs())
						.withMaxBatchSize(attributes.getMaxBatchSize())
						.withMaxInflightOutboundBatches(attributes.getMaxInflightOutboundBatches());
				awsSQSAsyncClient = new AmazonSQSBufferedAsyncClient(asyncClient, config);
			} catch (Exception e) {
				classLogger.error("Failed to initialize SQS Client", e);
				throw e;
			}
		}
	}


	public String getQueueURL(String queueName) {
		if(this.queueUrl == null) {
			this.queueUrl = this.awsSQSAsyncClient.getQueueUrl(queueName).getQueueUrl();
		}
		return this.queueUrl;
	}

	public String getLargeQueueUrl(String queueName) {
		if(this.largeMessageQueueUrl == null) {
			this.largeMessageQueueUrl = this.awsSQSAsyncClient.getQueueUrl(queueName).getQueueUrl();
		}
		return this.largeMessageQueueUrl;
	}

	public void sendMessages(MessageRequestWrapper messageList, String processorType) {
		if(ProcessorType.EXTENDED.name().equals(processorType)){
			messageList.getSendMessageRequest().forEach(message -> this.awsSQSExtendedClient.sendMessage(message));
		} else if (ProcessorType.S3.name().equals(processorType)) {
			messageList.getPutObjectRequest().forEach(message -> this.awsS3Client.putObject(message));
		} else {
			messageList.getSendMessageRequest().forEach(message -> this.awsSQSAsyncClient.sendMessageAsync(message));
		}
	}

}
