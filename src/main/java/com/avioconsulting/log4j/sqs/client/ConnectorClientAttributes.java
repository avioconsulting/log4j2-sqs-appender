package com.avioconsulting.log4j.sqs.client;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/** AWS connection settings and outbound SQS batching options. */
public class ConnectorClientAttributes {

	private final Region awsRegion;
	private final Integer maxBatchOpenMs ;
	private final Integer maxBatchSize;
	private final Integer maxInflightOutboundBatches;
	private final String s3BucketName;
	private final AwsCredentialsProvider credentialsProvider;
	private final String endpointURL;

	/**
	 * Collects the settings used when the connector first opens its AWS clients.
	 * @param credentialsProvider source of credentials for signing requests
	 * @param awsRegion target AWS region
	 * @param maxBatchOpenMs batch flush interval in milliseconds; null selects 200, nonpositive disables batching
	 * @param maxBatchSize messages per batch, limited to 1–10; null selects 10, 1 disables batching
	 * @param maxInflightOutboundBatches legacy setting retained for compatibility; not enforced
	 * @param s3BucketName bucket for S3-backed delivery modes
	 * @param endpointURL optional service endpoint override
	 */
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

	/** @return region used for AWS requests */
	public Region getAwsRegion() {
		return awsRegion;
	}

	/** @return configured batch interval in milliseconds, or null for the default */
	public Integer getMaxBatchOpenMs() {
		return maxBatchOpenMs;
	}

	/** @return configured message count per batch, before the SQS limit is applied */
	public Integer getMaxBatchSize() {
		return maxBatchSize;
	}

	/**
	 * Kept for configuration compatibility. SDK v2 has no equivalent setting, so it is not used.
	 * @return the legacy in-flight batch limit as configured
	 */
	public Integer getMaxInflightOutboundBatches() {
		return maxInflightOutboundBatches;
	}

	/** @return bucket for S3-backed delivery modes */
	public String getS3BucketName() {
		return s3BucketName;
	}

	/** @return provider used to authenticate AWS requests */
	public AwsCredentialsProvider getCredentialsProvider() {
		return credentialsProvider;
	}

	/** @return the configured endpoint override, or null if none was supplied */
	public String getEndpointURL() {
		return this.endpointURL;
	}
}
