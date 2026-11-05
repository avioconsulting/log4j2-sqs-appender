package com.avioconsulting.log4j.sqs;

import com.avioconsulting.log4j.sqs.processor.DiscardMessageProcessor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SqsManagerRoutingTest {

    private static final String QUEUE = "mule-log-test";
    private static final String LARGE_QUEUE = "mule-log-test-large.fifo";
    private static final String BUCKET = "log-bucket";

    private RecordingConnectorClient client;
    private final Layout<?> layout = PatternLayout.newBuilder().withPattern("%m").build();

    @Before
    public void setUp() {
        client = new RecordingConnectorClient();
    }

    @Test
    public void messageOver256KiBIsSentWholeWhenModeIsNotSet() {
        String message = repeat('a', 300_000);
        manager(1_000_000, null).send(layout, event(message));

        assertEquals(1, client.sent.size());
        assertEquals(QUEUE, client.sent.get(0).queueName);
        assertEquals(message, client.sent.get(0).request.messageBody());
        assertFalse(client.sent.get(0).extended);
    }

    @Test
    public void messageOverMaxMessageBytesIsTruncatedWhenModeIsNotSet() {
        manager(1_000_000, null).send(layout, event(repeat('a', 1_200_000)));

        assertEquals(1, client.sent.size());
        assertEquals(QUEUE, client.sent.get(0).queueName);
        assertEquals(1_000_000, bytes(client.sent.get(0).request.messageBody()));
    }

    @Test
    public void oversizedMessageIsLoggedWithoutTheOriginalBody() {
        List<LogEvent> captured = new ArrayList<>();
        Appender capture = new AbstractAppender("oversized-capture", null, null, true, null) {
            @Override
            public void append(LogEvent event) {
                captured.add(event.toImmutable());
            }
        };
        capture.start();
        org.apache.logging.log4j.core.Logger logger =
                (org.apache.logging.log4j.core.Logger) LogManager.getLogger(SqsManager.class);
        logger.addAppender(capture);
        try {
            manager(1_000, null).send(layout, event(repeat('x', 2_000)));
            manager(1_000, "EXTENDED").send(layout, event(repeat('y', 2_000)));
        } finally {
            logger.removeAppender(capture);
            capture.stop();
        }

        assertEquals(2, captured.size());
        assertEquals(Level.WARN, captured.get(0).getLevel());
        String truncated = captured.get(0).getMessage().getFormattedMessage();
        assertTrue(truncated.contains("2000"));
        assertTrue(truncated.contains("largeMessageMode=TRUNCATE"));
        assertFalse(truncated.contains("xxx"));
        assertEquals(Level.INFO, captured.get(1).getLevel());
        assertTrue(captured.get(1).getMessage().getFormattedMessage().contains("largeMessageMode=EXTENDED"));
    }

    @Test
    public void maxMessageBytesIsCappedAtSqsLimit() {
        SqsManager manager = manager(5_000_000, null);
        assertEquals(SqsLimits.MAX_MESSAGE_BYTES, manager.getMaxMessageBytes());

        manager.send(layout, event(repeat('a', 2_000_000)));
        assertEquals(SqsLimits.MAX_MESSAGE_BYTES, bytes(client.sent.get(0).request.messageBody()));
    }

    @Test
    public void maxMessageBytesDefaultsToSqsLimit() {
        assertEquals(SqsLimits.MAX_MESSAGE_BYTES, manager(null, null).getMaxMessageBytes());
    }

    @Test
    public void extendedModeSendsSmallMessagesDirectly() {
        String message = repeat('a', 900_000);
        manager(null, "EXTENDED").send(layout, event(message));

        assertEquals(1, client.sent.size());
        assertFalse(client.sent.get(0).extended);
        assertEquals(QUEUE, client.sent.get(0).queueName);
        assertEquals(message, client.sent.get(0).request.messageBody());
    }

    @Test
    public void extendedModeSendsMessagesOver1MiBThroughS3() {
        String message = repeat('a', SqsLimits.MAX_MESSAGE_BYTES + 1);
        manager(null, "EXTENDED").send(layout, event(message));

        assertEquals(1, client.sent.size());
        assertTrue(client.sent.get(0).extended);
        assertEquals(QUEUE, client.sent.get(0).queueName);
        assertEquals(message, client.sent.get(0).request.messageBody());
    }

    @Test
    public void s3ModeUploadsLargeMessagesOnly() {
        SqsManager manager = manager(1_000, "S3");
        manager.send(layout, event("small"));
        String large = repeat('b', 2_000);
        manager.send(layout, event(large));

        assertEquals(1, client.sent.size());
        assertEquals("small", client.sent.get(0).request.messageBody());
        assertEquals(1, client.s3Objects.size());
        assertEquals(BUCKET, client.s3Objects.get(0).getPutObjectRequest().bucket());
        assertEquals(large, client.s3Objects.get(0).getContent());
    }

    @Test
    public void fifoModeSplitsLargeMessagesToLargeMessageQueue() {
        manager(1_000, "FIFO").send(layout, event(repeat('c', 2_500)));

        assertEquals(3, client.sent.size());
        String groupId = client.sent.get(0).request.messageGroupId();
        for (int i = 0; i < 3; i++) {
            RecordingConnectorClient.Sent sent = client.sent.get(i);
            assertEquals(LARGE_QUEUE, sent.queueName);
            assertEquals(groupId, sent.request.messageGroupId());
            assertTrue(sent.request.messageBody().startsWith("currentPart=" + (i + 1) + "|totalParts=3|"));
        }
    }

    @Test
    public void fifoPartsFitInSqsLimit() {
        manager(null, "FIFO").send(layout, event(repeat('c', 3_000_000)));

        for (RecordingConnectorClient.Sent sent : client.sent) {
            assertTrue(bytes(sent.request.messageBody()) <= SqsLimits.MAX_MESSAGE_BYTES);
        }
    }

    @Test
    public void discardModeReplacesLargeMessages() {
        manager(1_000, "DISCARD").send(layout, event(repeat('d', 2_000)));

        assertEquals(1, client.sent.size());
        assertEquals(QUEUE, client.sent.get(0).queueName);
        assertEquals(DiscardMessageProcessor.DISCARD_MESSAGE + 1_000, client.sent.get(0).request.messageBody());
    }

    @Test
    public void stoppingManagerClosesClient() {
        SqsManager manager = manager(null, null);
        manager.stop(0, java.util.concurrent.TimeUnit.MILLISECONDS);
        assertEquals(SqsManager.DEFAULT_SHUTDOWN_TIMEOUT_MS, client.closedWithTimeoutMillis);
    }

    private SqsManager manager(Integer maxMessageBytes, String mode) {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        return new SqsManager(new DefaultConfiguration(), context, "test-" + UUID.randomUUID(), QUEUE,
                LARGE_QUEUE, maxMessageBytes, mode, BUCKET, client);
    }

    private static LogEvent event(String message) {
        return Log4jLogEvent.newBuilder().setMessage(new SimpleMessage(message)).build();
    }

    private static int bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    private static String repeat(char c, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(c);
        }
        return builder.toString();
    }
}
