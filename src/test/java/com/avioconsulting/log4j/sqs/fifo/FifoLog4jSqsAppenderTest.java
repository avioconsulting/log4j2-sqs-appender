package com.avioconsulting.log4j.sqs.fifo;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.*;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.buffered.QueueBufferConfig;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import com.avioconsulting.log4j.sqs.util.PropertiesProvider;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.avioconsulting.log4j.sqs.util.PropertiesProvider.*;
import static org.junit.Assert.assertEquals;

public class FifoLog4jSqsAppenderTest {

    private static final Logger logger = LogManager.getLogger(FifoLog4jSqsAppenderTest.class.getName());
    public static String FIFO_QUEUE_URL;
    private final static String MESSAGE_TO_LOG = "s321234567890123456789012345678901234567890123456789012345" +
            "67890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
            "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345" +
            "6789012345678901234567890123456789012345678901234567890END";


    public static AmazonSQS awsSqsClient;
    @Before
    public void setUpClass() {
        PropertiesProvider.readProperties();
        getConnectionToAWS();
        createQueue();
        //deleteObjectsFromS3Bucket();
    }
    @After
    public void tearDownClass() {
        //deleteBucket();
        awsSqsClient.shutdown();
    }

    @Test
    public void test_logMessagesQuantity_in_s3() {
        assertEquals(0, awsSqsClient.receiveMessage(FIFO_QUEUE_URL).getMessages().size());
        logger.info(MESSAGE_TO_LOG);
        logger.info(MESSAGE_TO_LOG);
        logger.info(MESSAGE_TO_LOG);
        assertEquals(3, awsSqsClient.receiveMessage(FIFO_QUEUE_URL).getMessages().size());

    }

    private static void getConnectionToAWS() {
        AWSStaticCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY));
        ;
        AmazonSQSClientBuilder clientBuilder = AmazonSQSClientBuilder.standard();
        awsSqsClient = clientBuilder.withRegion(String.valueOf(AWS_REGION))
                .withCredentials(credentialsProvider)
                .build();
            }

    private static void createQueue() {
        //CreateQueueResult result = awsSqsClient.createQueue(AWS_SQS_QUEUE_NAME + "-truncate");
        Map<String, String> attributes = new HashMap<>();
        attributes.put(QueueAttributeName.FifoQueue.name(), Boolean.TRUE.toString());
        CreateQueueRequest request = new CreateQueueRequest()
                .withQueueName("aviofifo.fifo")
                .withAttributes(attributes) ;// Set FIFO_QUEUE attribute to true
        CreateQueueResult result = awsSqsClient.createQueue(request);
        FIFO_QUEUE_URL = result.getQueueUrl();
    }
    /*private static void deleteObjectsFromS3Bucket() {
        Stream<String> keysToDelete = awsSqsClient
                .listObjects(AWS_S3_BUCKET_NAME)
                .getObjectSummaries()
                .stream()
                .map(s3ObjectSummary -> s3ObjectSummary.getKey());
        String[] keysArrayToDelete = keysToDelete.toArray(size -> new String[size]);

        if(keysArrayToDelete.length > 0){
            DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(AWS_S3_BUCKET_NAME).withKeys(keysArrayToDelete);
            awsSqsClient.deleteObjects(deleteObjectsRequest);
        }

    }

    private S3Object getLastObjectOfS3Bucket() {
        String objectKey = awsSqsClient.listObjects(AWS_S3_BUCKET_NAME).getObjectSummaries().get(0).getKey();
        return awsSqsClient.getObject(AWS_S3_BUCKET_NAME, objectKey);
    }

    private void createBucket(){
        awsSqsClient.createBucket(AWS_S3_BUCKET_NAME);
    }

    private void deleteBucket(){
        awsSqsClient.deleteBucket(AWS_S3_BUCKET_NAME);
    }*/

}