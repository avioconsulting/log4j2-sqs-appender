package com.avioconsulting.log4j.sqs;

import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.avioconsulting.log4j.sqs.client.AWSClient;
import com.avioconsulting.log4j.sqs.processor.LogEventProcessor;
import com.avioconsulting.log4j.sqs.processor.ProcessorsSupplier;
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SqsManager extends AbstractManager {
    private final Configuration configuration;
    private String queueName;
    private String largeMessageQueueName;
    private Integer maxMessageBytes;

    private final String largeMessageMode;
    private AWSClient awsClient;

    private static final Logger logger = LogManager.getLogger(SqsManager.class);

    protected SqsManager(final Configuration configuration,
                         final LoggerContext loggerContext,
                         final String name,
                         final String queueName,
                         final String largeMessageQueueName,
                         final Integer maxMessageBytes,
                         final String largeMessageMode,
                         final AWSClient awsClient) {
        super(loggerContext, name);
        this.configuration = Objects.requireNonNull(configuration);
        this.queueName = queueName;
        this.largeMessageQueueName = largeMessageQueueName == null ? queueName.concat(".fifo") : largeMessageQueueName;
        this.maxMessageBytes = maxMessageBytes == null ? 250000 : maxMessageBytes;
        this.largeMessageMode = largeMessageMode;
        this.awsClient = awsClient;

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
        logger.debug("Message length: {}", messageLength);
        String queueURL = this.awsClient.getQueueURL(queueName);
        List<SendMessageRequest> messagesToSend = Arrays.asList(new SendMessageRequest(queueURL,message));

        if (messageLength > this.maxMessageBytes) {
            LogEventProcessor processor = ProcessorsSupplier.selectProcessor(largeMessageMode);
            queueURL = this.awsClient.getQueueURL(largeMessageQueueName);
            messagesToSend = processor.process(message,maxMessageBytes,queueURL);

        }
        sendMessageList(messagesToSend);
    }

    private void sendMessageList(List<SendMessageRequest> messageList){
        logger.debug("Sending [{}] messages to queue [{}]" , messageList.size());
        this.awsClient.sendMessages(messageList);
    }
    /*public String toString() {
        return "[ region=" + this.awsRegion + ", maxBatchOpenMs=" + this.maxBatchOpenMs + ", maxBatchSize=" + this.maxBatchSize + ", maxInflightOutboundBatches=" + this.maxInflightOutboundBatches + " ]";
    }*/



    /**
     * Truncate a string to a specific number of bytes.
     *
     * @param src the string to be truncated
     * @param encoding the character encoding to use for the truncated string
     * @param maxsize the maximum size of the string in bytes
     * @return the truncated string
     */
    protected static String truncateStringByByteLength(final String src, final String encoding, final int maxsize) {
        Charset cs = Charset.forName(encoding);
        CharsetEncoder coder = cs.newEncoder();
        ByteBuffer out = ByteBuffer.allocate(maxsize);
        CharBuffer in = CharBuffer.wrap(src);
        coder.encode(in, out, true);
        int pos = src.length() - in.length();
        return src.substring(0, pos);
    }
}