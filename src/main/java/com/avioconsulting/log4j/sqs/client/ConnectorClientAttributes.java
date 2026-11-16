package com.avioconsulting.log4j.sqs.client;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;

public class ConnectorClientAttributes {

	private final Region awsRegion;
	private final Integer maxBatchOpenMs ;
	private final Integer maxBatchSize;
	private final Integer maxInflightOutboundBatches;
	private final String s3BucketName;
	private final AwsCredentialsProvider credentialsProvider;
	private final String endpointURL;

	public ConnectorClientAttributes(AwsCredentialsProvider credentialsProvider, Region awsRegion, Integer maxBatchOpenMs,
			Integer maxBatchSize, Integer maxInflightOutboundBatches, String s3BucketName, String endpointURL) {
		this.credentialsProvider = credentialsProvider;
		this.maxBatchOpenMs = maxBatchOpenMs;
		this.maxBatchSize = maxBatchSize;
		this.maxInflightOutboundBatches = maxInflightOutboundBatches;
		this.s3BucketName = s3BucketName;
		this.awsRegion = awsRegion;
		this.endpointURL=endpointURL;
	}

	public Region getAwsRegion() {
		return awsRegion;
	}

	public Integer getMaxBatchOpenMs() {
		return maxBatchOpenMs;
	}

	public Integer getMaxBatchSize() {
		return maxBatchSize;
	}

	/**
	 * Kept for configuration compatibility. SDK v2 has no equivalent setting, so it is not used.
	 */
	public Integer getMaxInflightOutboundBatches() {
		return maxInflightOutboundBatches;
	}

	public String getS3BucketName() {
		return s3BucketName;
	}

	public AwsCredentialsProvider getCredentialsProvider() {
		return credentialsProvider;
	}

	public String getEndpointURL() {
		return this.endpointURL;
	}
}
