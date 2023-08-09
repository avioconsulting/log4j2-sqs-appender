package com.avioconsulting.log4j.sqs.util;

import java.util.Properties;

public class PropertiesProvider {

    private static Properties testProperties;
    public static String AWS_S3_BUCKET_NAME;
    public static String AWS_ACCESS_KEY;
    public static String AWS_SECRET_KEY;
    public static String AWS_REGION;
    public static String AWS_SQS_QUEUE_NAME;
    public static String AWS_SQS_LARGE_QUEUE_NAME;
    public static Integer MAX_MESSAGE_BYTES;
    public static Integer MAX_BATCH_OPEN_MS;
    public static Integer MAX_BATCH_SIZE;
    public static Integer MAX_INFLIGHT_OUTBOUND_BATCHES;
    public static String ENDPOINT_URL;

    public static void readProperties() {
        testProperties = System.getProperties();
        AWS_S3_BUCKET_NAME = testProperties.getProperty("awsBucketName");
        AWS_ACCESS_KEY = testProperties.getProperty("awsAccessKey");
        AWS_SECRET_KEY = testProperties.getProperty("awsSecretKey");
        AWS_REGION = testProperties.getProperty("awsRegion") != null ? testProperties.getProperty("awsRegion") : "us-west-2";
        AWS_SQS_QUEUE_NAME = testProperties.getProperty("awsQueueName");
        AWS_SQS_LARGE_QUEUE_NAME = testProperties.getProperty("awsLargeMessageQueueName");
        MAX_MESSAGE_BYTES = Integer.valueOf(testProperties.getProperty("maxMessageBytes"));
        ENDPOINT_URL = testProperties.getProperty("awsEndpointURL");
        MAX_BATCH_OPEN_MS = 100;
        MAX_BATCH_SIZE = 1;
        MAX_INFLIGHT_OUTBOUND_BATCHES = 1;
    }

}
