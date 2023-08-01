package com.avioconsulting.log4j.sqs.client;

import com.amazon.sqs.javamessaging.AmazonSQSExtendedClient;
import com.amazon.sqs.javamessaging.ExtendedClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
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
	public static final Logger classLogger = LogManager.getLogger(ConnectorClient.class);

	private final ConnectorClientAttributes connectorClientAttributes;

	public ConnectorClient(ConnectorClientAttributes attributes) {
		this.connectorClientAttributes = attributes;
	}

	private void initS3Client(ConnectorClientAttributes attributes) {
		if(awsS3Client == null) {
			try {
				AmazonS3ClientBuilder clientBuilder = AmazonS3ClientBuilder
						.standard()
						.withCredentials(new AWSStaticCredentialsProvider(attributes.getCredentialsProvider().getCredentials()));

				if(!attributes.getEndpointURL().isEmpty()){
					clientBuilder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(attributes.getEndpointURL(),
							String.valueOf(attributes.getAwsRegion())));
				}

				this.awsS3Client = clientBuilder.build();

			} catch (Exception e) {
				classLogger.error("Failed to initialize S3 AWS Client", e);
			}

		}
	}

	private void initSqsExtendedClient(ConnectorClientAttributes attributes) {
		if(awsSQSExtendedClient == null) {
			final ExtendedClientConfiguration extendedClientConfig =
					new ExtendedClientConfiguration()
							.withPayloadSupportEnabled(awsS3Client, attributes.getS3BucketName());

			AmazonSQSAsyncClientBuilder clientBuilder = AmazonSQSAsyncClientBuilder.standard()
					.withCredentials(attributes.getCredentialsProvider());

			if(!attributes.getEndpointURL().isEmpty()){
				clientBuilder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(attributes.getEndpointURL() , String.valueOf(attributes.getAwsRegion())));
			}

			awsSQSExtendedClient =
					new AmazonSQSExtendedClient(clientBuilder.build(), extendedClientConfig);

		}
	}

	private void initSQSClient(ConnectorClientAttributes attributes) {

		if (this.awsSQSAsyncClient == null) {
			try {
				classLogger.debug("Initializing SQS Client");
				AmazonSQSAsyncClientBuilder clientBuilder = AmazonSQSAsyncClientBuilder.standard()
						.withCredentials(attributes.getCredentialsProvider());

				if(!attributes.getEndpointURL().isEmpty()){
					clientBuilder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(attributes.getEndpointURL() , String.valueOf(attributes.getAwsRegion())));
				}

				QueueBufferConfig config = new QueueBufferConfig().withMaxBatchOpenMs(attributes.getMaxBatchOpenMs())
						.withMaxBatchSize(attributes.getMaxBatchSize())
						.withMaxInflightOutboundBatches(attributes.getMaxInflightOutboundBatches());
				awsSQSAsyncClient = new AmazonSQSBufferedAsyncClient(clientBuilder.build(), config);
			} catch (Exception e) {
				classLogger.error("Failed to initialize SQS Client", e);
				throw e;
			}
		}
	}

	public void sendMessages(MessageRequestWrapper messageList, String processorType, String queueName, String largeMessageQueueName) {
		if (ProcessorType.EXTENDED.name().equals(processorType)){
			sendExtendedMessages(messageList,largeMessageQueueName);
		} else if (ProcessorType.S3.name().equals(processorType)) {
			initS3Client(connectorClientAttributes);
			messageList.getPutObjectRequest().forEach(message -> this.awsS3Client.putObject(message));
		} else {
			sendFIFOTruncateDiscardedMessages(messageList,queueName,largeMessageQueueName,processorType);
		}
	}

	private void sendExtendedMessages(MessageRequestWrapper messageList, String queueName) {
		initS3Client(connectorClientAttributes);
		initSqsExtendedClient(connectorClientAttributes);
		String queueUrl = this.awsSQSExtendedClient.getQueueUrl(queueName).getQueueUrl();
		messageList.getSendMessageRequest().forEach(message -> {
			message.setQueueUrl(queueUrl);
			this.awsSQSExtendedClient.sendMessage(message);
		});
	}

	private void sendFIFOTruncateDiscardedMessages(MessageRequestWrapper messageList, String queueName, String largeMessageQueueName, String processorType) {
		initSQSClient(connectorClientAttributes);
		String queueUrl = null;
		if (ProcessorType.DEFAULT.name().equals(processorType)) {
			queueUrl = awsSQSAsyncClient.getQueueUrl(queueName).getQueueUrl();
		} else {
			queueUrl = awsSQSAsyncClient.getQueueUrl(largeMessageQueueName).getQueueUrl();
		}

		String finalQueueUrl = queueUrl;
		messageList.getSendMessageRequest().forEach(message -> {
			message.setQueueUrl(finalQueueUrl);
			this.awsSQSAsyncClient.sendMessageAsync(message);
		});
	}

}
