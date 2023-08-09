package com.avioconsulting.log4j.sqs.client;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.regions.Region;

public class ConnectorClientAttributes {

	private final Region awsRegion;
	private final Integer maxBatchOpenMs ;
	private final Integer maxBatchSize;
	private final Integer maxInflightOutboundBatches;
	private final String s3BucketName;
	private final AWSStaticCredentialsProvider credentialsProvider;
	private final String endpointURL;

	public ConnectorClientAttributes(AWSStaticCredentialsProvider credentialsProvider, Region awsRegion, Integer maxBatchOpenMs,
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

	public Integer getMaxInflightOutboundBatches() {
		return maxInflightOutboundBatches;
	}

	public String getS3BucketName() {
		return s3BucketName;
	}

	public AWSStaticCredentialsProvider getCredentialsProvider() {
		return credentialsProvider;
	}

	public String getEndpointURL() {
		return this.endpointURL;
	}
}
