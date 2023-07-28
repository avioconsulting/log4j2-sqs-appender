package com.avioconsulting.log4j.sqs.client;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

public class ConnectorClientFactory {

    private ConnectorClientFactory() {
    }

    public static ConnectorClient createConnectorClient(String awsAccessKey, String awsSecretKey, String awsRegion, Integer maxBatchOpenMs, Integer maxBatchSize, Integer maxInflightOutboundBatches, String s3BucketName) {
        AWSStaticCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(awsAccessKey, awsSecretKey));
        Regions r = Regions.fromName(awsRegion);
        Region region = Region.getRegion(r);
        ConnectorClientAttributes attributes = new ConnectorClientAttributes(credentialsProvider, region,
                maxBatchOpenMs, maxBatchSize, maxInflightOutboundBatches, s3BucketName);
        return new ConnectorClient(attributes);
    }
}
