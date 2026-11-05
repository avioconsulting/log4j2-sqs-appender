package com.avioconsulting.log4j.sqs;

import com.avioconsulting.log4j.sqs.client.ConnectorClient;
import com.avioconsulting.log4j.sqs.processor.DefaultMessageProcessor;
import com.avioconsulting.log4j.sqs.processor.LogEventProcessor;
import com.avioconsulting.log4j.sqs.processor.ProcessorAttributes;
import com.avioconsulting.log4j.sqs.processor.ProcessorSupplier;
import com.avioconsulting.log4j.sqs.processor.ProcessorType;
import com.avioconsulting.log4j.sqs.wrapper.MessageRequestWrapper;
import com.avioconsulting.log4j.sqs.wrapper.S3ObjectRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.status.StatusLogger;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Sends each log event to SQS.
 * <p>
 * Messages up to maxMessageBytes are sent unchanged to queueName. Bigger messages are handled by
 * largeMessageMode, which defaults to TRUNCATE. maxMessageBytes defaults to, and is capped at,
 * the SQS limit of 1 MiB.
 */
public class SqsManager extends AbstractManager {
    static final long DEFAULT_SHUTDOWN_TIMEOUT_MS = 5_000;

    private final Configuration configuration;
    private final String queueName;
    private final String bucketName;
    private final String largeMessageQueueName;
    private final int maxMessageBytes;
    private final String largeMessageMode;
    private final ConnectorClient connectorClient;

    private static final Logger classLogger = StatusLogger.getLogger();
    /**
     * Application logger, not the status logger, so truncation and the other oversized-message paths
     * show up on the same appenders as the rest of the application's logs. The text is short and does
     * not include the original event, so this log is sent normally and does not re-enter this branch.
     */
    private static final Logger oversizedLog = LogManager.getLogger(SqsManager.class);

    protected SqsManager(final Configuration configuration,
                         final LoggerContext loggerContext,
                         final String name,
                         final String queueName,
                         final String largeMessageQueueName,
                         final Integer maxMessageBytes,
                         final String largeMessageMode,
                         final String bucketName,
                         final ConnectorClient connectorClient) {
        super(loggerContext, name);
        this.configuration = Objects.requireNonNull(configuration);
        this.queueName = queueName;
        this.bucketName = bucketName;
        this.largeMessageQueueName = largeMessageQueueName == null ? queueName.concat(".fifo") : largeMessageQueueName;
        this.maxMessageBytes = effectiveMaxMessageBytes(maxMessageBytes);
        this.largeMessageMode = largeMessageMode;
        this.connectorClient = connectorClient;
    }

    public void send(final Layout<?> layout, final LogEvent event) {
        String message = new String(layout.toByteArray(event), StandardCharsets.UTF_8);
        int messageLength = message.getBytes(StandardCharsets.UTF_8).length;
        classLogger.debug("Message length: {}", messageLength);

        if (messageLength <= maxMessageBytes) {
            dispatch(new DefaultMessageProcessor().process(attributes(message)), queueName, false);
            return;
        }

        String mode = largeMessageMode == null || largeMessageMode.trim().isEmpty()
                ? ProcessorType.TRUNCATE.name()
                : largeMessageMode;
        logOversized(messageLength, mode);

        LogEventProcessor logEventProcessor = ProcessorSupplier.selectProcessor(largeMessageMode);
        MessageRequestWrapper wrapper = logEventProcessor.process(attributes(message));
        boolean fifo = ProcessorType.FIFO.name().equals(largeMessageMode);
        boolean extended = ProcessorType.EXTENDED.name().equals(largeMessageMode);
        dispatch(wrapper, fifo ? largeMessageQueueName : queueName, extended);
    }

    int getMaxMessageBytes() {
        return maxMessageBytes;
    }

    @Override
    protected boolean releaseSub(final long timeout, final TimeUnit timeUnit) {
        long timeoutMillis = timeout > 0 ? timeUnit.toMillis(timeout) : DEFAULT_SHUTDOWN_TIMEOUT_MS;
        connectorClient.close(timeoutMillis);
        return true;
    }

    private void dispatch(MessageRequestWrapper wrapper, String targetQueueName, boolean extended) {
        for (SendMessageRequest request : wrapper.getSendMessageRequest()) {
            if (extended) {
                connectorClient.sendExtendedMessage(targetQueueName, request);
            } else {
                connectorClient.sendMessage(targetQueueName, request);
            }
        }
        for (S3ObjectRequest request : wrapper.getPutObjectRequest()) {
            connectorClient.putObject(request);
        }
    }

    private void logOversized(int messageLength, String mode) {
        if (ProcessorType.TRUNCATE.name().equals(mode) || ProcessorType.DISCARD.name().equals(mode)) {
            oversizedLog.warn("Log message of {} bytes exceeds maxMessageBytes of {}. largeMessageMode={}. The original event was not sent whole.",
                    messageLength, maxMessageBytes, mode);
            return;
        }
        oversizedLog.info("Log message of {} bytes exceeds maxMessageBytes of {}. largeMessageMode={}.",
                messageLength, maxMessageBytes, mode);
    }

    private ProcessorAttributes attributes(String message) {
        return new ProcessorAttributes(message, maxMessageBytes, bucketName);
    }

    static int effectiveMaxMessageBytes(Integer configured) {
        if (configured == null || configured <= 0) {
            return SqsLimits.MAX_MESSAGE_BYTES;
        }
        return Math.min(configured, SqsLimits.MAX_MESSAGE_BYTES);
    }

}
