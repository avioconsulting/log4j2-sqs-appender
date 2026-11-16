package com.avioconsulting.log4j.sqs.fifo;

import com.avioconsulting.log4j.sqs.processor.FifoMessageProcessor;
import com.avioconsulting.log4j.sqs.util.AppenderTestUtils;
import com.avioconsulting.log4j.sqs.util.PropertiesProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.avioconsulting.log4j.sqs.util.PropertiesProvider.AWS_SQS_LARGE_QUEUE_NAME;
import static com.avioconsulting.log4j.sqs.util.PropertiesProvider.MAX_MESSAGE_BYTES;
import static org.junit.Assert.*;

public class FifoLog4jSqsAppenderTest {

    private static final Logger logger = LogManager.getLogger(FifoLog4jSqsAppenderTest.class.getName());
    public static String FIFO_QUEUE_URL;
    private final static String MESSAGE_TO_LOG = "FIFO21234567890123456789012345678901234567890123456789012345" +
            "67890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
            "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345" +
            "6789012345678901234567890123456789012345678901234567890ENDFIFO21234567890123456789012345678901234567890123456789012345" +
            "67890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
            "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345" +
            "6789012345678901234567890123456789012345678901234567890END3";

    public static SqsClient awsSqsClient;

    @BeforeClass
    public static void startUpClass() {
        PropertiesProvider.readProperties();
        awsSqsClient = AppenderTestUtils.getConnectionToSQSClient();
        createQueue();
        logger.info(MESSAGE_TO_LOG); //Puts log message using appender into
    }

    @AfterClass
    public static void tearDownClass() {
        awsSqsClient.deleteQueue(r -> r.queueUrl(FIFO_QUEUE_URL));
        awsSqsClient.close();
    }

    private Message waitForMessage() {
        List<Message> messages = AppenderTestUtils.receiveMessages(awsSqsClient, FIFO_QUEUE_URL, 1, 200);
        assertFalse("No messages received!", messages.isEmpty());
        return messages.get(0);
    }

    @Test
    public void test_fifoLogMessageContent_from_sqs() {

        String[] parts = new FifoMessageProcessor().splitStringByByteLength("[INFO] - " + MESSAGE_TO_LOG, StandardCharsets.UTF_8.name(),
                FifoMessageProcessor.partSize(MAX_MESSAGE_BYTES));
        final int partAmount = parts.length;
        List<Message> messagesList = new ArrayList<>();
        for (int i = 0; i < partAmount; i++) {
            messagesList.add(waitForMessage());
        }

        final String uuidToAssert = getUUIDFromMessage(messagesList.get(0).body());

        for (int i = 0; i < messagesList.size(); i++) {

            String messageBody = messagesList.get(i).body();
            String messageUUID = getUUIDFromMessage(messageBody);
            assertEquals(uuidToAssert, messageUUID);

            int index = messageBody.lastIndexOf("|");
            String messageContent = messageBody.substring(index).replace("|message=", "");
            assertEquals(messageContent.trim(), parts[i]);

            assertTrue(messageBody.contains("currentPart=" + Integer.valueOf(i + 1) + "|totalParts=" + partAmount));

        }
    }

    private String getUUIDFromMessage(String message) {
        int index = message.lastIndexOf("|");
        int uuidIndex = message.indexOf("|uuid=");
        return message.substring(uuidIndex, index).replace("|uuid=", "");
    }

    private static void createQueue() {
        Map<QueueAttributeName, String> queueAttributes = new HashMap<>();
        queueAttributes.put(QueueAttributeName.FIFO_QUEUE, "true");
        queueAttributes.put(QueueAttributeName.CONTENT_BASED_DEDUPLICATION, "true");
        queueAttributes.put(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS, "20");

        FIFO_QUEUE_URL = awsSqsClient.createQueue(r -> r.queueName(AWS_SQS_LARGE_QUEUE_NAME + ".fifo")
                .attributes(queueAttributes)).queueUrl();
    }

}
