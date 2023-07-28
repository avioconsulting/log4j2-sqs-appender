package com.avioconsulting.log4j.sqs.discard;

import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.avioconsulting.log4j.sqs.util.AppenderTestUtils;
import com.avioconsulting.log4j.sqs.util.PropertiesProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.avioconsulting.log4j.sqs.util.PropertiesProvider.AWS_SQS_LARGE_QUEUE_NAME;
import static com.avioconsulting.log4j.sqs.util.PropertiesProvider.MAX_MESSAGE_BYTES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DiscardLog4jSqsAppenderTest {

    private static final Logger logger = LogManager.getLogger(DiscardLog4jSqsAppenderTest.class.getName());
    private final static String MESSAGE_TO_LOG = "TRUNCATE21234567890123456789012345678901234567890123456789012345" +
            "67890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
            "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345" +
            "6789012345678901234567890123456789012345678901234567890END";
    private static AmazonSQSBufferedAsyncClient awsSQSAsyncClient;
    private static String DISCARDED_MESSAGES_QUEUE_URL;

    @BeforeClass
    public static void startUpClass() {
        PropertiesProvider.readProperties();
        awsSQSAsyncClient = AppenderTestUtils.getConnectionToSQSAsyncClient();
        DISCARDED_MESSAGES_QUEUE_URL = AppenderTestUtils.createQueue(awsSQSAsyncClient,"discard");
        logger.info(MESSAGE_TO_LOG); //Puts log message using appender into
    }

    @AfterClass
    public static void tearDownClass() {
        awsSQSAsyncClient.deleteQueue(DISCARDED_MESSAGES_QUEUE_URL);
        awsSQSAsyncClient.shutdown();
    }

    @Test
    public void test_discardedLogMessageContent_from_sqs () throws ExecutionException, InterruptedException {
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest()
                .withQueueUrl(DISCARDED_MESSAGES_QUEUE_URL)
                .withWaitTimeSeconds(160)
                .withMaxNumberOfMessages(1);

        Future<ReceiveMessageResult> messageResultFuture = awsSQSAsyncClient.receiveMessageAsync(receiveMessageRequest);
        boolean received = false;

        while (!received) {
            if (messageResultFuture.get().getMessages().size() > 0) {
                Message message = messageResultFuture.get().getMessages().get(0);
                assertEquals(message.getBody(), "Can't display log message. Message max size exceeded. max size:"+MAX_MESSAGE_BYTES);
                awsSQSAsyncClient.deleteMessage(DISCARDED_MESSAGES_QUEUE_URL, message.getReceiptHandle());
                received=true;
            }
        }

        if(!received){
            fail("No messages received!");
        }
    }

}
