package com.avioconsulting.log4j.sqs.processor;

import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.avioconsulting.log4j.sqs.wrapper.MessageRequestWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    private static final Logger logger = LogManager.getLogger(FifoMessageProcessor.class);

    @Override
    public MessageRequestWrapper process(ProcessorAttributes processorAttributes) {
        List<SendMessageRequest> sendMessageRequestList = new ArrayList<>();
        logger.debug("Splitting large message");
        UUID uuid = UUID.randomUUID();
        logger.debug("Large Message UUID: {}", uuid);
        String[] splitMessage = splitStringByByteLength(processorAttributes.getMessage(), "UTF-8", processorAttributes.getMaxMessageSize());
        MessageRequestWrapper messageRequestWrapper = new MessageRequestWrapper();

        for (int i = 0; i < splitMessage.length; i++) {
            logger.debug("Sending message {} of {}", i + 1, splitMessage.length);
            SendMessageRequest sendMessageRequest = new SendMessageRequest();
            sendMessageRequest.setMessageBody(String.format("currentPart=%d|totalParts=%d|uuid=%s|message=%s", i + 1, splitMessage.length, uuid, splitMessage[i]));
            sendMessageRequest.setMessageGroupId(uuid.toString());
            sendMessageRequestList.add(sendMessageRequest);
            messageRequestWrapper.setSendMessageRequest(sendMessageRequestList);
        }
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
    public static String[] splitStringByByteLength(final String src, final String encoding, final int maxsize) {
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
