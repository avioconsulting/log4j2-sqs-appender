package com.avioconsulting.log4j.sqs.s3;

import com.avioconsulting.log4j.sqs.util.AppenderTestUtils;
import com.avioconsulting.log4j.sqs.util.PropertiesProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.avioconsulting.log4j.sqs.util.PropertiesProvider.AWS_S3_BUCKET_NAME;
import static org.junit.Assert.assertEquals;

public class S3Log4jSqsAppenderTest {

    private static final Logger logger = LogManager.getLogger(S3Log4jSqsAppenderTest.class.getName());
    private final static String MESSAGE_TO_LOG = "s321234567890123456789012345678901234567890123456789012345" +
            "67890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
            "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345" +
            "6789012345678901234567890123456789012345678901234567890END";

    public static S3Client awsS3Client;
    @Before
    public void setUpClass() {
        PropertiesProvider.readProperties();
        awsS3Client = AppenderTestUtils.getConnectionToAWSS3();
        awsS3Client.createBucket(r -> r.bucket(AWS_S3_BUCKET_NAME));

    }
    @After
    public void tearDownClass() {
        AppenderTestUtils.deleteObjectsFromS3Bucket(awsS3Client,AWS_S3_BUCKET_NAME);
        awsS3Client.deleteBucket(r -> r.bucket(AWS_S3_BUCKET_NAME));
        awsS3Client.close();
    }

    @Test
    public void test_logMessagesQuantity_in_s3() {
        assertEquals(0, listObjects().size());
        //Puts log messages using appender into
        logger.info(MESSAGE_TO_LOG);
        logger.info(MESSAGE_TO_LOG);
        logger.info(MESSAGE_TO_LOG);
        assertEquals(3, listObjects().size());
    }

    @Test
    public void test_logMessageContent_in_s3() {
        assertEquals(0, listObjects().size());
        logger.info(MESSAGE_TO_LOG);
        String objectKey = listObjects().get(0).key();
        String logInS3Object = awsS3Client.getObjectAsBytes(r -> r.bucket(AWS_S3_BUCKET_NAME).key(objectKey))
                .asString(StandardCharsets.UTF_8);
        assertEquals(logInS3Object.trim(), "[INFO] - " + MESSAGE_TO_LOG);
    }

    private List<S3Object> listObjects() {
        return awsS3Client.listObjects(r -> r.bucket(AWS_S3_BUCKET_NAME)).contents();
    }

}
