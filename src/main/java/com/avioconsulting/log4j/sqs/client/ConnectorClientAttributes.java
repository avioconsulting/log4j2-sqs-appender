package com.avioconsulting.log4j.sqs.client;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.regions.Region;

public class ConnectorClientAttributes {

	private Region awsRegion;

	private Integer maxBatchOpenMs ;
	private Integer maxBatchSize;
	private Integer maxInflightOutboundBatches;
	private String s3BucketName;
	private AWSStaticCredentialsProvider credentialsProvider;

	public ConnectorClientAttributes(AWSStaticCredentialsProvider credentialsProvider, Region awsRegion, Integer maxBatchOpenMs,
			Integer maxBatchSize, Integer maxInflightOutboundBatches, String s3BucketName) {
		this.credentialsProvider = credentialsProvider;
		this.maxBatchOpenMs = maxBatchOpenMs;
		this.maxBatchSize = maxBatchSize;
		this.maxInflightOutboundBatches = maxInflightOutboundBatches;
		this.s3BucketName = s3BucketName;
		this.awsRegion = awsRegion;
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
}
