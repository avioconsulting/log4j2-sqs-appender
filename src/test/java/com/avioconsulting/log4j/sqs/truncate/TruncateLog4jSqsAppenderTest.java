package com.avioconsulting.log4j.sqs.truncate;

import com.avioconsulting.log4j.sqs.processor.TruncateMessageProcessor;
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
import java.util.List;

import static com.avioconsulting.log4j.sqs.util.PropertiesProvider.MAX_MESSAGE_BYTES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TruncateLog4jSqsAppenderTest {

    private static final Logger logger = LogManager.getLogger(TruncateLog4jSqsAppenderTest.class.getName());
    private final static String MESSAGE_TO_LOG = "TRUNCATE21234567890123456789012345678901234567890123456789012345" +
            "67890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890" +
            "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345" +
            "6789012345678901234567890123456789012345678901234567890END";
    private static SqsClient awsSqsClient;
    private static String TRUNCATED_MESSAGES_QUEUE_URL;

    @BeforeClass
    public static void startUpClass() {
        PropertiesProvider.readProperties();
        awsSqsClient = AppenderTestUtils.getConnectionToSQSClient();
        TRUNCATED_MESSAGES_QUEUE_URL = AppenderTestUtils.createQueue(awsSqsClient, "truncate");
        logger.info(MESSAGE_TO_LOG); //Puts log message using appender into
    }

    @AfterClass
    public static void tearDownClass() {
        awsSqsClient.deleteQueue(r -> r.queueUrl(TRUNCATED_MESSAGES_QUEUE_URL));
        awsSqsClient.close();
    }

    @Test
    public void test_truncatedLogMessageContent_from_sqs() {
        List<Message> messages = AppenderTestUtils.receiveMessages(awsSqsClient, TRUNCATED_MESSAGES_QUEUE_URL, 1, 160);
        assertFalse("No messages received!", messages.isEmpty());
        assertEquals(truncateLoggedString(), messages.get(0).body());

        String sizeAsStr = awsSqsClient.getQueueAttributes(r -> r.queueUrl(TRUNCATED_MESSAGES_QUEUE_URL)
                        .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES))
                .attributes().get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES);
        assertEquals("0", sizeAsStr);
    }

    private String truncateLoggedString() {
        return new TruncateMessageProcessor().truncateStringByByteLength("[INFO] - " + MESSAGE_TO_LOG, StandardCharsets.UTF_8.name(), MAX_MESSAGE_BYTES);
    }


}
