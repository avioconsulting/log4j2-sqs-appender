package com.avioconsulting.log4j.sqs.processor;

import com.avioconsulting.log4j.sqs.wrapper.MessageRequestWrapper;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Truncates the incoming message to maxMessageBytes.
 *
 * @author francisco.rodriguez
 */
public class TruncateMessageProcessor implements LogEventProcessor {

    private static final Logger logger = StatusLogger.getLogger();

    @Override
    public MessageRequestWrapper process(ProcessorAttributes processorAttributes) {
        logger.debug("Sending TRUNCATED message");
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .messageBody(truncateStringByByteLength(processorAttributes.getMessage(),
                        StandardCharsets.UTF_8.name(), processorAttributes.getMaxMessageSize()))
                .build();
        MessageRequestWrapper messageRequestWrapper = new MessageRequestWrapper();
        messageRequestWrapper.setSendMessageRequest(Collections.singletonList(sendMessageRequest));
        return messageRequestWrapper;
    }

    /**
     * Truncate a string to a specific number of bytes.
     *
     * @param src      the string to be truncated
     * @param encoding the character encoding to use for the truncated string
     * @param maxsize  the maximum size of the string in bytes
     * @return the truncated string
     */
    public String truncateStringByByteLength(final String src, final String encoding, final int maxsize) {
        Charset cs = Charset.forName(encoding);
        CharsetEncoder coder = cs.newEncoder();
        ByteBuffer out = ByteBuffer.allocate(maxsize);
        CharBuffer in = CharBuffer.wrap(src);
        coder.encode(in, out, true);
        int pos = src.length() - in.length();
        return src.substring(0, pos);
    }
}
