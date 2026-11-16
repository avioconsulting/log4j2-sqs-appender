package com.avioconsulting.log4j.sqs.extended;

import com.avioconsulting.log4j.sqs.util.AppenderTestUtils;
import com.avioconsulting.log4j.sqs.util.PropertiesProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.avioconsulting.log4j.sqs.util.PropertiesProvider.AWS_S3_BUCKET_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExtendedLog4jSqsAppenderTest {

    private static final Logger logger = LogManager.getLogger(ExtendedLog4jSqsAppenderTest.class.getName());
    private static String MESSAGE_TO_LOG;
    private static SqsClient awsSqsClient;
    public static S3Client awsS3Client;
    private static String EXTENDED_MESSAGES_QUEUE_URL;

    @BeforeClass
    public static void setUpClass() throws Exception {
        PropertiesProvider.readProperties();
        awsS3Client = AppenderTestUtils.getConnectionToAWSS3();
        awsSqsClient = AppenderTestUtils.getConnectionToSQSClient();
        EXTENDED_MESSAGES_QUEUE_URL = AppenderTestUtils.createQueue(awsSqsClient, "extended");
        awsS3Client.createBucket(r -> r.bucket(AWS_S3_BUCKET_NAME));
        MESSAGE_TO_LOG = AppenderTestUtils.readFileAsString();
        logger.info(MESSAGE_TO_LOG); //Puts log message using appender into
    }

    @AfterClass
    public static void tearDownClass() {
        awsSqsClient.deleteQueue(r -> r.queueUrl(EXTENDED_MESSAGES_QUEUE_URL));
        AppenderTestUtils.deleteObjectsFromS3Bucket(awsS3Client, AWS_S3_BUCKET_NAME);
        awsS3Client.deleteBucket(r -> r.bucket(AWS_S3_BUCKET_NAME));
        awsSqsClient.close();
        awsS3Client.close();
    }

    @Test
    public void test_extendedLogMessageContent_from_sqs() throws ParseException {
        List<Message> messages = AppenderTestUtils.receiveMessages(awsSqsClient, EXTENDED_MESSAGES_QUEUE_URL, 1, 160);
        assertFalse("No messages received!", messages.isEmpty());

        String messageKey = getMessageKey(messages.get(0).body());
        String textFromS3 = awsS3Client.getObjectAsBytes(r -> r.bucket(AWS_S3_BUCKET_NAME).key(messageKey))
                .asString(StandardCharsets.UTF_8);

        assertEquals("[INFO] - " + MESSAGE_TO_LOG.trim(), textFromS3.trim());
    }

    private static String getMessageKey(String messageBody) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONArray json = (JSONArray) parser.parse(messageBody);
        return ((JSONObject) json.get(1)).get("s3Key").toString();
    }

}
