package com.avioconsulting.log4j.sqs.client;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/** Creates a connector from the appender's AWS configuration attributes. */
public class ConnectorClientFactory {

    private ConnectorClientFactory() {
    }

    /**
     * Creates a connector using static AWS credentials.
     * @param awsAccessKey AWS access key ID
     * @param awsSecretKey secret associated with the access key ID
     * @param awsRegion AWS region identifier
     * @param maxBatchOpenMs flush interval in milliseconds; nonpositive disables batching
     * @param maxBatchSize maximum messages per batch; one disables batching
     * @param maxInflightOutboundBatches legacy batch limit, ignored by the SDK v2 connector
     * @param s3BucketName bucket for S3-backed delivery modes
     * @param endpointURL optional service endpoint override
     * @param largeMessageMode oversized-message strategy configured on the appender
     * @return connector that opens AWS clients on first use
     */
    public static ConnectorClient createConnectorClient(String awsAccessKey,
                                                        String awsSecretKey,
                                                        String awsRegion,
                                                        Integer maxBatchOpenMs,
                                                        Integer maxBatchSize,
                                                        Integer maxInflightOutboundBatches,
                                                        String s3BucketName,
                                                        String endpointURL,
                                                        String largeMessageMode) {
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(awsAccessKey, awsSecretKey));
        ConnectorClientAttributes attributes = new ConnectorClientAttributes(credentialsProvider,
                Region.of(awsRegion),
                maxBatchOpenMs,
                maxBatchSize,
                maxInflightOutboundBatches,
                s3BucketName,
                endpointURL);

        return new AwsConnectorClient(attributes, largeMessageMode);
    }
}
