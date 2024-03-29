package com.avioconsulting.log4j.sqs.processor;

import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.avioconsulting.log4j.sqs.wrapper.MessageRequestWrapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;

import java.util.Collections;

/**
 * DiscardMessage will modify the original message for a log trace indicating that message
 * exceeded maxBiteSize parameter.
 */
public class DiscardMessageProcessor implements LogEventProcessor {

    private static final Logger logger = LogManager.getLogger(DiscardMessageProcessor.class);

    @Override
    public MessageRequestWrapper process(ProcessorAttributes processorAttributes) {
        logger.debug("Sending DISCARD message.");
        MutableLogEvent mutableLogEvent = new MutableLogEvent();
        mutableLogEvent.setLevel(Level.WARN);
        mutableLogEvent.setMessage(new SimpleMessage("Can't display log message. Message max size exceeded. max size:" + processorAttributes.getMaxMessageSize()));
        SendMessageRequest sendMessageRequest = new SendMessageRequest();
        sendMessageRequest.setMessageBody(mutableLogEvent.getMessage().getFormattedMessage());
        MessageRequestWrapper messageRequestWrapper = new MessageRequestWrapper();
        messageRequestWrapper.setSendMessageRequest(Collections.singletonList(sendMessageRequest));
        return messageRequestWrapper;
    }

}
