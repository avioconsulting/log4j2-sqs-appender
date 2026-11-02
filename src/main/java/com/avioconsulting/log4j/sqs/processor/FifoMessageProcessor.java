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
    private static final Logger logger = StatusLogger.getLogger();

    @Override
    public MessageRequestWrapper process(ProcessorAttributes processorAttributes) {
        List<SendMessageRequest> sendMessageRequestList = new ArrayList<>();
        UUID uuid = UUID.randomUUID();
        logger.debug("Splitting large message. Large message UUID: {}", uuid);
        int partSize = Math.min(processorAttributes.getMaxMessageSize(),
                SqsLimits.MAX_MESSAGE_BYTES - PART_HEADER_RESERVED_BYTES);
        String[] splitMessage = splitStringByByteLength(processorAttributes.getMessage(), "UTF-8", partSize);

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
