package com.avioconsulting.log4j.sqs.truncate;

import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.avioconsulting.log4j.sqs.processor.TruncateMessageProcessor;
import com.avioconsulting.log4j.sqs.util.AppenderTestUtils;
import com.avioconsulting.log4j.sqs.util.PropertiesProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.avioconsulting.log4j.sqs.util.PropertiesProvider.MAX_MESSAGE_BYTES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TruncateLog4jSqsAppenderTest {

    private static final Logger logger = LogManager.getLogger(TruncateLog4jSqsAppenderTest.class.getName());
    private final static String MESSAGE_TO_LOG = "TRUNCATE21234567890123456789012345678901234567890123456789012345" +
            "67890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
            "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345" +
            "6789012345678901234567890123456789012345678901234567890END";
    private static AmazonSQSBufferedAsyncClient awsSQSAsyncClient;
    private static String TRUNCATED_MESSAGES_QUEUE_URL;

    @BeforeClass
    public static void startUpClass() {
        PropertiesProvider.readProperties();
        awsSQSAsyncClient = AppenderTestUtils.getConnectionToSQSAsyncClient();
        TRUNCATED_MESSAGES_QUEUE_URL = AppenderTestUtils.createQueue(awsSQSAsyncClient,"truncate");
        logger.info(MESSAGE_TO_LOG); //Puts log message using appender into
    }

    @AfterClass
    public static void tearDownClass() {
        awsSQSAsyncClient.deleteQueue(TRUNCATED_MESSAGES_QUEUE_URL);
        awsSQSAsyncClient.shutdown();
    }

    @Test
    public void test_truncatedLogMessageContent_from_sqs () throws ExecutionException, InterruptedException {

        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest()
                .withQueueUrl(TRUNCATED_MESSAGES_QUEUE_URL)
                .withWaitTimeSeconds(160)
                .withMaxNumberOfMessages(1);

        Future<ReceiveMessageResult> messageResultFuture = awsSQSAsyncClient.receiveMessageAsync(receiveMessageRequest);
        boolean received = false;
        while (!received) {
            if (messageResultFuture.get().getMessages().size() > 0) {
                Message message = messageResultFuture.get().getMessages().get(0);
                assertEquals(message.getBody(), truncateLoggedString());
                awsSQSAsyncClient.deleteMessage(TRUNCATED_MESSAGES_QUEUE_URL, message.getReceiptHandle());
                received = true;
            }
        }

        if(!received) {
            fail("No messages received!");
        }

        GetQueueAttributesResult attributes = awsSQSAsyncClient.getQueueAttributes(TRUNCATED_MESSAGES_QUEUE_URL, Collections.singletonList("ApproximateNumberOfMessages"));
        String sizeAsStr = attributes.getAttributes().get("ApproximateNumberOfMessages");
        assertEquals("0",sizeAsStr);
    }

    private String truncateLoggedString() {
        return new TruncateMessageProcessor().truncateStringByByteLength("[INFO] - " + MESSAGE_TO_LOG, StandardCharsets.UTF_8.name(), MAX_MESSAGE_BYTES);
    }


}
