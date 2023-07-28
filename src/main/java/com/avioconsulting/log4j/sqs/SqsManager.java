package com.avioconsulting.log4j.sqs;

import com.avioconsulting.log4j.sqs.client.ConnectorClient;
import com.avioconsulting.log4j.sqs.processor.LogEventProcessor;
import com.avioconsulting.log4j.sqs.processor.ProcessorAttributes;
import com.avioconsulting.log4j.sqs.processor.ProcessorSupplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.config.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class SqsManager extends AbstractManager {
    private final Configuration configuration;
    private String queueName;
    private String bucketName;
    private String largeMessageQueueName;
    private Integer maxMessageBytes;
    private final String largeMessageMode;
    private ConnectorClient connectorClient;

    private static final Logger classLogger = LogManager.getLogger(SqsManager.class);

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
        this.maxMessageBytes = maxMessageBytes == null ? 250000 : maxMessageBytes;
        this.largeMessageMode = largeMessageMode;
        this.connectorClient = connectorClient;

    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void startup() {
        // This default implementation does nothing
    }

    public void send(final Layout<?> layout, final LogEvent event) {
        LogEventProcessor logEventProcessor = ProcessorSupplier.selectProcessor(largeMessageMode);
        String message = new String(layout.toByteArray(event), StandardCharsets.UTF_8);
        String messageMode = largeMessageMode;
        int messageLength = message.getBytes().length;
        classLogger.debug("Message length: {}", messageLength);
        ProcessorAttributes processorAttributes = new ProcessorAttributes(message, maxMessageBytes, bucketName);
        this.connectorClient.sendMessages(logEventProcessor.process(processorAttributes), messageMode, queueName, largeMessageQueueName);

    }

}