package com.avioconsulting.log4j.sqs.processor;

import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.avioconsulting.log4j.sqs.wrapper.MessageRequestWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

/**
 * Default implementation will create a wrapper message within an unique message element.
 * Message will be logged without modifications.
 *
 * @param
 * @return
 */
public class DefaultMessageProcessor implements LogEventProcessor {

    private static final Logger logger = LogManager.getLogger(DefaultMessageProcessor.class);

    @Override
    public MessageRequestWrapper process(ProcessorAttributes processorAttributes) {
        logger.debug("Sending DEFAULT message.");
        SendMessageRequest sendMessageRequest = new SendMessageRequest();
        sendMessageRequest.setMessageBody(processorAttributes.getMessage());
        MessageRequestWrapper messageRequestWrapper = new MessageRequestWrapper();
        messageRequestWrapper.setSendMessageRequest(Arrays.asList(sendMessageRequest));
        return messageRequestWrapper;
    }

}
