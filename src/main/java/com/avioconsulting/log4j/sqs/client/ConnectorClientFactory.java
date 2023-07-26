package com.avioconsulting.log4j.sqs.client;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConnectorClientFactory {

    private static final Logger logger = LogManager.getLogger(ConnectorClientFactory.class.getName());
    public static ConnectorClient createConnectorClient(String awsAccessKey, String awsSecretKey, String awsRegion, Integer maxBatchOpenMs, Integer maxBatchSize, Integer maxInflightOutboundBatches, String s3BucketName) {

        logger.info(":AWS "+awsRegion+"-"+awsAccessKey+"-"+awsSecretKey);
        AWSStaticCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(awsAccessKey, awsSecretKey));
        Regions r = Regions.fromName(awsRegion);
        Region region = Region.getRegion(r);
        ConnectorClientAttributes attributes = new ConnectorClientAttributes(credentialsProvider, region,
                maxBatchOpenMs, maxBatchSize, maxInflightOutboundBatches, s3BucketName);
        return new ConnectorClient(attributes);
    }
}
