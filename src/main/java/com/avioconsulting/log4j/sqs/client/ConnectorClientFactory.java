package com.avioconsulting.log4j.sqs.client;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

public class ConnectorClientFactory {

    private ConnectorClientFactory() {
    }

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
