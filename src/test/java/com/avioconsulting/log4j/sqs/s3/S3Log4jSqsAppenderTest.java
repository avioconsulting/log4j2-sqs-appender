package com.avioconsulting.log4j.sqs.s3;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.avioconsulting.log4j.sqs.client.ConnectorClientFactory;
import com.avioconsulting.log4j.sqs.util.PropertiesProvider;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static com.avioconsulting.log4j.sqs.util.PropertiesProvider.*;
import static com.avioconsulting.log4j.sqs.util.PropertiesProvider.AWS_REGION;
import static org.junit.Assert.assertEquals;

public class S3Log4jSqsAppenderTest {

    private static final Logger logger = LogManager.getLogger(S3Log4jSqsAppenderTest.class.getName());
    private final static String MESSAGE_TO_LOG = "s321234567890123456789012345678901234567890123456789012345" +
            "67890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
            "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345" +
            "6789012345678901234567890123456789012345678901234567890END";


    public static AmazonS3 awsS3Client;
    @Before
    public void setUpClass() {

            PropertiesProvider.readProperties();
            getConnectionToAWSS3();
         //   createBucket();
            deleteObjectsFromS3Bucket();

    }
    @After
    public void tearDownClass() {
        //deleteBucket();
        awsS3Client.shutdown();
    }

    @Test
    public void test_logMessagesQuantity_in_s3() {
        assertEquals(0, awsS3Client.listObjects(AWS_S3_BUCKET_NAME).getObjectSummaries().size());
        logger.info(MESSAGE_TO_LOG);
        logger.info(MESSAGE_TO_LOG);
        logger.info(MESSAGE_TO_LOG);
        assertEquals(3, awsS3Client.listObjects(AWS_S3_BUCKET_NAME).getObjectSummaries().size());
    }

    @Test
    public void test_logMessageContent_in_s3() throws IOException {
        assertEquals(0, awsS3Client.listObjects(AWS_S3_BUCKET_NAME).getObjectSummaries().size());
        logger.info(MESSAGE_TO_LOG);
        S3Object s3Object = getLastObjectOfS3Bucket();
        String logInS3Object = IOUtils.toString(s3Object.getObjectContent(), StandardCharsets.UTF_8);
        assertEquals(logInS3Object.trim(), "[INFO] - " + MESSAGE_TO_LOG);
    }


    private static void getConnectionToAWSS3() {
        AWSStaticCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY));
        awsS3Client =  AmazonS3ClientBuilder
                .standard()
                .withRegion(AWS_REGION)
                .withCredentials(credentialsProvider)
                .build();
    }

    private static void deleteObjectsFromS3Bucket() {
        Stream<String> keysToDelete = awsS3Client
                .listObjects(AWS_S3_BUCKET_NAME)
                .getObjectSummaries()
                .stream()
                .map(s3ObjectSummary -> s3ObjectSummary.getKey());
        String[] keysArrayToDelete = keysToDelete.toArray(size -> new String[size]);

        if(keysArrayToDelete.length > 0){
            DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(AWS_S3_BUCKET_NAME).withKeys(keysArrayToDelete);
            awsS3Client.deleteObjects(deleteObjectsRequest);
        }

    }

    private S3Object getLastObjectOfS3Bucket() {
        String objectKey = awsS3Client.listObjects(AWS_S3_BUCKET_NAME).getObjectSummaries().get(0).getKey();
        return awsS3Client.getObject(AWS_S3_BUCKET_NAME, objectKey);
    }

    private void createBucket(){
        awsS3Client.createBucket(AWS_S3_BUCKET_NAME);
    }

    private void deleteBucket(){
        awsS3Client.deleteBucket(AWS_S3_BUCKET_NAME);
    }

}