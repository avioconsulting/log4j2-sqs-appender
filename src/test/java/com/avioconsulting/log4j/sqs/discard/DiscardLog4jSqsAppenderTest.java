package com.avioconsulting.log4j.sqs.discard;

import com.avioconsulting.log4j.sqs.util.AppenderTestUtils;
import com.avioconsulting.log4j.sqs.util.PropertiesProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;

import static com.avioconsulting.log4j.sqs.util.PropertiesProvider.MAX_MESSAGE_BYTES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class DiscardLog4jSqsAppenderTest {

    private static final Logger logger = LogManager.getLogger(DiscardLog4jSqsAppenderTest.class.getName());
    private final static String MESSAGE_TO_LOG = "TRUNCATE21234567890123456789012345678901234567890123456789012345" +
            "67890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
            "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345" +
            "6789012345678901234567890123456789012345678901234567890END";
    private static SqsClient awsSqsClient;
    private static String DISCARDED_MESSAGES_QUEUE_URL;

    @BeforeClass
    public static void startUpClass() {
        PropertiesProvider.readProperties();
        awsSqsClient = AppenderTestUtils.getConnectionToSQSClient();
        DISCARDED_MESSAGES_QUEUE_URL = AppenderTestUtils.createQueue(awsSqsClient, "discard");
        logger.info(MESSAGE_TO_LOG); //Puts log message using appender into
    }

    @AfterClass
    public static void tearDownClass() {
        awsSqsClient.deleteQueue(r -> r.queueUrl(DISCARDED_MESSAGES_QUEUE_URL));
        awsSqsClient.close();
    }

    @Test
    public void test_discardedLogMessageContent_from_sqs() {
        List<Message> messages = AppenderTestUtils.receiveMessages(awsSqsClient, DISCARDED_MESSAGES_QUEUE_URL, 1, 160);
        assertFalse("No messages received!", messages.isEmpty());
        assertEquals("Can't display log message. Message max size exceeded. max size:" + MAX_MESSAGE_BYTES, messages.get(0).body());
    }

}
