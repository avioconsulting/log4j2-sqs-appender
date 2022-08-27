package com.avioconsulting.log4j.sqs.client;

public class AvioAWSClientAttributes {

	private String awsRegion;
	private String awsAccessKey;
	private String awsSecretKey;
	private Integer maxBatchOpenMs ;
	private Integer maxBatchSize;
	private Integer maxInflightOutboundBatches;
	private String s3BucketName;

	public AvioAWSClientAttributes(String awsRegion, String awsAccessKey, String awsSecretKey, Integer maxBatchOpenMs,
			Integer maxBatchSize, Integer maxInflightOutboundBatches, String s3BucketName) {
		this.awsRegion = awsRegion;
		this.awsAccessKey = awsAccessKey;
		this.awsSecretKey = awsSecretKey;
		this.maxBatchOpenMs = maxBatchOpenMs;
		this.maxBatchSize = maxBatchSize;
		this.maxInflightOutboundBatches = maxInflightOutboundBatches;
		this.s3BucketName = s3BucketName;
	}

	public String getAwsRegion() {
		return awsRegion;
	}

	public String getAwsAccessKey() {
		return awsAccessKey;
	}

	public String getAwsSecretKey() {
		return awsSecretKey;
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
}
