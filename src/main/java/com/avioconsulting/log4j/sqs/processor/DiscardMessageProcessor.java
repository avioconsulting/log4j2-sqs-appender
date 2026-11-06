package com.avioconsulting.log4j.sqs.processor;

import com.avioconsulting.log4j.sqs.wrapper.MessageRequestWrapper;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Collections;

/**
 * DiscardMessage will modify the original message for a log trace indicating that message
 * exceeded maxBiteSize parameter.
 */
public class DiscardMessageProcessor implements LogEventProcessor {

    public static final String DISCARD_MESSAGE = "Can't display log message. Message max size exceeded. max size:";
    private static final Logger logger = StatusLogger.getLogger();

    @Override
    public MessageRequestWrapper process(ProcessorAttributes processorAttributes) {
        logger.debug("Sending DISCARD message.");
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .messageBody(DISCARD_MESSAGE + processorAttributes.getMaxMessageSize())
                .build();
        MessageRequestWrapper messageRequestWrapper = new MessageRequestWrapper();
        messageRequestWrapper.setSendMessageRequest(Collections.singletonList(sendMessageRequest));
        return messageRequestWrapper;
    }

}
