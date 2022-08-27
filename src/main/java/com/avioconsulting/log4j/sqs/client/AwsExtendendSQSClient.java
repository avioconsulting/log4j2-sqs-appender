package com.avioconsulting.log4j.sqs.client;

import com.amazon.sqs.javamessaging.AmazonSQSExtendedClient;
import com.amazon.sqs.javamessaging.ExtendedClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import java.util.List;

public class AwsExtendendSQSClient implements AvioAWSClient{

	private AmazonSQS sqsExtended;
	public AwsExtendendSQSClient(AvioAWSClientAttributes attributes) {
		initSqsClient(attributes);
	}

	private void initSqsClient(AvioAWSClientAttributes attributes) {
		if(sqsExtended == null) {
			AWSStaticCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(
					new BasicAWSCredentials(attributes.getAwsAccessKey(), attributes.getAwsSecretKey()));
			AmazonS3 s3 = new AmazonS3Client(credentialsProvider);
			Regions r = Regions.fromName(attributes.getAwsRegion());
			s3.setRegion(Region.getRegion(r));

			final ExtendedClientConfiguration extendedClientConfig =
					new ExtendedClientConfiguration()
							.withLargePayloadSupportEnabled(s3, attributes.getS3BucketName());

			this.sqsExtended =
					new AmazonSQSExtendedClient(AmazonSQSAsyncClientBuilder.standard().
							withRegion(Regions.US_EAST_1).
							withCredentials(credentialsProvider).build(),
							extendedClientConfig);
		}
	}

	@Override public String getQueueURL(String queueName) {
		return sqsExtended.getQueueUrl(queueName).getQueueUrl();
	}

	@Override public void sendMessages(List<SendMessageRequest> messageList) {
		messageList.forEach(message -> sqsExtended.sendMessage(message));
	}
}
