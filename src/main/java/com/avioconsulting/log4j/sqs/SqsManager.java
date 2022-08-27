package com.avioconsulting.log4j.sqs;

import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.avioconsulting.log4j.sqs.client.AVIOAWSClientSupplier;
import com.avioconsulting.log4j.sqs.client.AvioAWSClient;
import com.avioconsulting.log4j.sqs.client.AwsSQSClient;
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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SqsManager extends AbstractManager {
    private final Configuration configuration;
    private String queueName;
    private String largeMessageQueueName;
    private Integer maxMessageBytes;

    private final String largeMessageMode;
    private AVIOAWSClientSupplier avioawsClientSupplier;

    private static final Logger classLogger = LogManager.getLogger(SqsManager.class);

    protected SqsManager(final Configuration configuration,
                         final LoggerContext loggerContext,
                         final String name,
                         final String queueName,
                         final String largeMessageQueueName,
                         final Integer maxMessageBytes,
                         final String largeMessageMode,
                         final AVIOAWSClientSupplier avioawsClientSupplier) {
        super(loggerContext, name);
        this.configuration = Objects.requireNonNull(configuration);
        this.queueName = queueName;
        this.largeMessageQueueName = largeMessageQueueName == null ? queueName.concat(".fifo") : largeMessageQueueName;
        this.maxMessageBytes = maxMessageBytes == null ? 250000 : maxMessageBytes;
        this.largeMessageMode = largeMessageMode;
        this.avioawsClientSupplier = avioawsClientSupplier;

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
        String queueName = this.queueName;
        if(ProcessorType.FIFO.name().equals(largeMessageMode)){
            queueName = this.largeMessageQueueName;
        }

        if(messageLength <= maxMessageBytes ){
            //DefaultMessageProcessor
            sendMessageList(ProcessorType.DEFAULT.name(),message,queueName,maxMessageBytes);
        }else {
            sendMessageList(largeMessageMode,message,queueName,maxMessageBytes);
        }

    }


    private void sendMessageList(String processorType, String message, String queueName, Integer maxMessageBytes) {

        LogEventProcessor processor = ProcessorSupplier.selectProcessor(processorType);
        AvioAWSClient awsClient = avioawsClientSupplier.selectClient(processor.getClientName());
        String queueUrl = awsClient.getQueueURL(queueName);
        ProcessorAttributes processorAttributes = new ProcessorAttributes(message,queueUrl,maxMessageBytes);
        List<SendMessageRequest> messagesToSend = processor.process(processorAttributes);
        awsClient.sendMessages(messagesToSend);
    }

    /*public String toString() {
        return "[ region=" + this.awsRegion + ", maxBatchOpenMs=" + this.maxBatchOpenMs + ", maxBatchSize=" + this.maxBatchSize + ", maxInflightOutboundBatches=" + this.maxInflightOutboundBatches + " ]";
    }*/

}