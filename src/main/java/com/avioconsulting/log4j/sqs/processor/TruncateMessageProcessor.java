package com.avioconsulting.log4j.sqs.processor;

import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.avioconsulting.log4j.sqs.wrapper.MessageRequestWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Truncates the incoming message if its length is larger than 256kb
 *
 * @author francisco.rodriguez
 */
public class TruncateMessageProcessor implements LogEventProcessor {

    private static final Logger logger = LogManager.getLogger(TruncateMessageProcessor.class);

    @Override
    public MessageRequestWrapper process(ProcessorAttributes processorAttributes) {
        logger.debug("Sending TRUNCATED message");
        SendMessageRequest sendMessageRequest = new SendMessageRequest();
        sendMessageRequest.setMessageBody(truncateStringByByteLength(processorAttributes.getMessage(),
                StandardCharsets.UTF_8.name(), processorAttributes.getMaxMessageSize()));
        MessageRequestWrapper messageRequestWrapper = new MessageRequestWrapper();
        messageRequestWrapper.setSendMessageRequest(Arrays.asList(sendMessageRequest));
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

