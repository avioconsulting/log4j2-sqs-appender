package com.avioconsulting.log4j.sqs;

import com.avioconsulting.log4j.sqs.client.ConnectorClient;
import com.avioconsulting.log4j.sqs.processor.LogEventProcessor;
import com.avioconsulting.log4j.sqs.processor.ProcessorAttributes;
import com.avioconsulting.log4j.sqs.processor.ProcessorType;
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
                         final ConnectorClient connectorClient) {
        super(loggerContext, name);
        this.configuration = Objects.requireNonNull(configuration);
        this.queueName = queueName;
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
        String message = new String(layout.toByteArray(event), StandardCharsets.UTF_8);
        int messageLength = message.getBytes().length;
        classLogger.debug("Message length: {}", messageLength);
        String queueUrl = connectorClient.getQueueURL(queueName);

        if(ProcessorType.FIFO.name().equals(largeMessageMode)){
            queueUrl = connectorClient.getLargeQueueUrl(largeMessageQueueName);
        }

        if(messageLength <= maxMessageBytes ){
            sendMessageList(ProcessorType.DEFAULT.name(),message,queueUrl,maxMessageBytes);
        }else {
            sendMessageList(largeMessageMode,message,queueUrl,maxMessageBytes);
        }

    }

    private void sendMessageList(String processorType, String message, String queueUrl, Integer maxMessageBytes) {
        LogEventProcessor processor = ProcessorSupplier.selectProcessor(processorType);
        ProcessorAttributes processorAttributes = new ProcessorAttributes(message,queueUrl,maxMessageBytes);
        this.connectorClient.sendMessages(processor.process(processorAttributes), processorType);
    }

}