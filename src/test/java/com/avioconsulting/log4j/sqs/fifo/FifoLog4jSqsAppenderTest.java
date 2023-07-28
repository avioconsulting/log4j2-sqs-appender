package com.avioconsulting.log4j.sqs.fifo;

import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.avioconsulting.log4j.sqs.processor.FifoMessageProcessor;
import com.avioconsulting.log4j.sqs.util.AppenderTestUtils;
import com.avioconsulting.log4j.sqs.util.PropertiesProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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

    public static AmazonSQSBufferedAsyncClient awsSqsClient;

    @BeforeClass
    public static void startUpClass() {
        PropertiesProvider.readProperties();
        awsSqsClient = AppenderTestUtils.getConnectionToSQSAsyncClient();
        createQueue();
        logger.info(MESSAGE_TO_LOG); //Puts log message using appender into
    }

    @AfterClass
    public static void tearDownClass() {
        awsSqsClient.deleteQueue(FIFO_QUEUE_URL);
        awsSqsClient.shutdown();
    }

    ReceiveMessageRequest createReceiveMessageRequest(String QUEUE_URL) {
        return new ReceiveMessageRequest().withQueueUrl(QUEUE_URL)
                .withWaitTimeSeconds(200)
                .withMaxNumberOfMessages(1);
    }

    private Message waitForMessages() throws ExecutionException, InterruptedException {
        ReceiveMessageRequest messagePart = createReceiveMessageRequest(FIFO_QUEUE_URL);
        Future<ReceiveMessageResult> messageResultFuture = awsSqsClient.receiveMessageAsync(messagePart);
        Message partMessage = null;
        boolean received = false;
        while (!received) {
            if (messageResultFuture.isDone()) {
                if (messageResultFuture.get().getMessages().size() > 0) {
                    Message message = messageResultFuture.get().getMessages().get(0);
                    awsSqsClient.deleteMessage(FIFO_QUEUE_URL, message.getReceiptHandle());
                    partMessage = message;
                    received = true;
                }
            }
        }

        if(!received) {
            fail("No messages received!");
        }

        return partMessage;
    }

    @Test
    public void test_fifoLogMessageContent_from_sqs() throws ExecutionException, InterruptedException {

        String[] parts = FifoMessageProcessor.splitStringByByteLength("[INFO] - " + MESSAGE_TO_LOG, StandardCharsets.UTF_8.name(), MAX_MESSAGE_BYTES);
        final int partAmount = parts.length;
        List<Message> messagesList = new ArrayList<>();
        for (int i = 0; i < partAmount; i++) {
            messagesList.add(waitForMessages());
        }

        final String uuidToAssert = getUUIDFromMessage(messagesList.get(0).getBody());

        for (int i = 0; i < messagesList.size(); i++) {

            String messageBody = messagesList.get(i).getBody();
            String messageUUID = getUUIDFromMessage(messageBody);
            assertEquals(uuidToAssert, messageUUID);

            int index = messageBody.lastIndexOf("|");
            String messageContent = messagesList.get(i).getBody().substring(index).replace("|message=", "");
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
        Map<String, String> queueAttributes = new HashMap<>();
        queueAttributes.put("FifoQueue", "true");
        queueAttributes.put("ContentBasedDeduplication", "true");
        queueAttributes.put("ReceiveMessageWaitTimeSeconds", "20");

        CreateQueueRequest createFifoQueueRequest = new CreateQueueRequest(
                AWS_SQS_LARGE_QUEUE_NAME+".fifo").withAttributes(queueAttributes);
        FIFO_QUEUE_URL = awsSqsClient.createQueue(createFifoQueueRequest)
                .getQueueUrl();
    }

}