package com.avioconsulting.log4j.sqs.extended;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.model.*;
import com.avioconsulting.log4j.sqs.util.AppenderTestUtils;
import com.avioconsulting.log4j.sqs.util.PropertiesProvider;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.avioconsulting.log4j.sqs.util.PropertiesProvider.AWS_S3_BUCKET_NAME;
import static com.avioconsulting.log4j.sqs.util.PropertiesProvider.AWS_SQS_QUEUE_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExtendedLog4jSqsAppenderTest {

    private static final Logger logger = LogManager.getLogger(ExtendedLog4jSqsAppenderTest.class.getName());
    private static String MESSAGE_TO_LOG;
    private static AmazonSQSBufferedAsyncClient awsSQSAsyncClient;
    public static AmazonS3 awsS3Client;
    private static String EXTENDED_MESSAGES_QUEUE_URL;

    @BeforeClass
    public static void setUpClass() throws Exception {
        PropertiesProvider.readProperties();
        awsS3Client = AppenderTestUtils.getConnectionToAWSS3();
        awsSQSAsyncClient = AppenderTestUtils.getConnectionToSQSAsyncClient();
        EXTENDED_MESSAGES_QUEUE_URL = AppenderTestUtils.createQueue(awsSQSAsyncClient,"extended");
        awsS3Client.createBucket(AWS_S3_BUCKET_NAME);
        MESSAGE_TO_LOG = AppenderTestUtils.readFileAsString();
        logger.info(MESSAGE_TO_LOG); //Puts log message using appender into
    }

    @AfterClass
    public static void tearDownClass() {
        awsSQSAsyncClient.deleteQueue(EXTENDED_MESSAGES_QUEUE_URL);
        AppenderTestUtils.deleteObjectsFromS3Bucket(awsS3Client,AWS_S3_BUCKET_NAME);
        awsS3Client.deleteBucket(AWS_S3_BUCKET_NAME);
        awsSQSAsyncClient.shutdown();
        awsS3Client.shutdown();
    }

    @Test
    public void test_extendedLogMessageContent_from_sqs () throws ExecutionException, InterruptedException, ParseException, IOException {
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest()
                .withQueueUrl(EXTENDED_MESSAGES_QUEUE_URL)
                .withWaitTimeSeconds(160)
                .withMaxNumberOfMessages(2);

        Future<ReceiveMessageResult> messageResultFuture = awsSQSAsyncClient.receiveMessageAsync(receiveMessageRequest);
        boolean received = false;
        Message message = null;
        while (!received) {
            if (messageResultFuture.isDone()) {
                if(messageResultFuture.get().getMessages().size() > 0) {
                    message = messageResultFuture.get().getMessages().get(0);
                }
                received=true;
            }
        }

        if(!received) {
            fail("No messages received!");
        }

        String messageKey = getMessageKey(message.getBody());
        String textFromS3=null;
        S3Object bucketObject = awsS3Client.getObject(AWS_S3_BUCKET_NAME,messageKey);
        textFromS3 =IOUtils.toString(bucketObject.getObjectContent(), StandardCharsets.UTF_8);

        assertEquals("[INFO] - "+MESSAGE_TO_LOG.trim(),textFromS3.trim());

    }

    private static String getMessageKey(String messageBody) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONArray json = (JSONArray) parser.parse(messageBody);
        return ((JSONObject) json.get(1)).get("s3Key").toString();
    }

}
