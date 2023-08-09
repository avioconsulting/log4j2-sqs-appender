package com.avioconsulting.log4j.sqs.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.avioconsulting.log4j.sqs.util.AppenderTestUtils;
import com.avioconsulting.log4j.sqs.util.PropertiesProvider;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.avioconsulting.log4j.sqs.util.PropertiesProvider.AWS_S3_BUCKET_NAME;
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
        awsS3Client = AppenderTestUtils.getConnectionToAWSS3();
        awsS3Client.createBucket(AWS_S3_BUCKET_NAME);

    }
    @After
    public void tearDownClass() {
        AppenderTestUtils.deleteObjectsFromS3Bucket(awsS3Client,AWS_S3_BUCKET_NAME);
        awsS3Client.deleteBucket(AWS_S3_BUCKET_NAME);
        awsS3Client.shutdown();
    }

    @Test
    public void test_logMessagesQuantity_in_s3() {
        assertEquals(0, awsS3Client.listObjects(AWS_S3_BUCKET_NAME).getObjectSummaries().size());
        //Puts log messages using appender into
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

    private S3Object getLastObjectOfS3Bucket() {
        String objectKey = awsS3Client.listObjects(AWS_S3_BUCKET_NAME).getObjectSummaries().get(0).getKey();
        return awsS3Client.getObject(AWS_S3_BUCKET_NAME, objectKey);
    }

}