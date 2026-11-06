package com.avioconsulting.log4j.sqs.processor;

import com.avioconsulting.log4j.sqs.SqsLimits;
import com.avioconsulting.log4j.sqs.wrapper.MessageRequestWrapper;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This class creates a message object to be send using ConnectorClient. This processor
 * breaks down a large message into small messages of same size. ConnectorClient sends each message piece
 * to the configure .fifo queue
 */
public class FifoMessageProcessor implements LogEventProcessor {

    /**
     * Bytes kept free in each part for the "currentPart=..|totalParts=..|uuid=..|message=" header.
     */
    static final int PART_HEADER_RESERVED_BYTES = 128;
    /**
     * A UTF-8 character can take this many bytes. A smaller payload cannot make progress and the split loops.
     */
    static final int MAX_UTF8_BYTES_PER_CHAR = 4;
    static final int MIN_FIFO_MESSAGE_BYTES = PART_HEADER_RESERVED_BYTES + MAX_UTF8_BYTES_PER_CHAR;
    private static final Logger logger = StatusLogger.getLogger();

    @Override
    public MessageRequestWrapper process(ProcessorAttributes processorAttributes) {
        List<SendMessageRequest> sendMessageRequestList = new ArrayList<>();
        UUID uuid = UUID.randomUUID();
        logger.debug("Splitting large message. Large message UUID: {}", uuid);
        String[] splitMessage = splitStringByByteLength(processorAttributes.getMessage(), "UTF-8",
                partSize(processorAttributes.getMaxMessageSize()));

        for (int i = 0; i < splitMessage.length; i++) {
            logger.debug("Sending message {} of {}", i + 1, splitMessage.length);
            sendMessageRequestList.add(SendMessageRequest.builder()
                    .messageBody(String.format("currentPart=%d|totalParts=%d|uuid=%s|message=%s", i + 1, splitMessage.length, uuid, splitMessage[i]))
                    .messageGroupId(uuid.toString())
                    .build());
        }
        MessageRequestWrapper messageRequestWrapper = new MessageRequestWrapper();
        messageRequestWrapper.setSendMessageRequest(sendMessageRequestList);
        return messageRequestWrapper;
    }

    /**
     * Bytes of message text in each part, so the part plus its header fits in maxMessageBytes
     * (and in the SQS limit). A queue whose MaximumMessageSize equals maxMessageBytes accepts every part.
     */
    public static int partSize(Integer maxMessageBytes) {
        int limit = maxMessageBytes == null || maxMessageBytes <= 0
                ? SqsLimits.MAX_MESSAGE_BYTES
                : Math.min(maxMessageBytes, SqsLimits.MAX_MESSAGE_BYTES);
        int payload = limit - PART_HEADER_RESERVED_BYTES;
        if (payload < MAX_UTF8_BYTES_PER_CHAR) {
            throw new IllegalArgumentException("maxMessageBytes of " + limit
                    + " is too small for a FIFO part. It must be at least " + MIN_FIFO_MESSAGE_BYTES
                    + " so the header and one character fit.");
        }
        return payload;
    }

    /**
     * Split the input string into a list of strings, each with a maximum memory size of maxsize bytes.
     *
     * @param src      the source string
     * @param encoding the encoding to use for the output string
     * @param maxsize  the maximum size of the string parts in bytes
     * @return an array of strings, each with a maximum length of maxsize bytes
     */
    public String[] splitStringByByteLength(final String src, final String encoding, final int maxsize) {
        Charset cs = Charset.forName(encoding);
        CharsetEncoder coder = cs.newEncoder();
        ByteBuffer out = ByteBuffer.allocate(maxsize);
        CharBuffer in = CharBuffer.wrap(src);
        List<String> ss = new ArrayList<>();
        int pos = 0;
        while (true) {
            CoderResult result = coder.encode(in, out, true);
            int newpos = src.length() - in.length();
            if (result.isOverflow() && newpos == pos) {
                throw new IllegalArgumentException("Cannot split a FIFO part into " + maxsize
                        + " bytes. The next character needs more room than that.");
            }
            String s = src.substring(pos, newpos);
            ss.add(s);
            pos = newpos;
            out.rewind();
            if (!result.isOverflow()) {
                break;
            }
        }
        return ss.toArray(new String[0]);
    }
}
