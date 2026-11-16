package com.avioconsulting.log4j.sqs.processor;

import com.avioconsulting.log4j.sqs.wrapper.MessageRequestWrapper;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Collections;

/**
 * Default implementation will create a wrapper message within an unique message element.
 * Message will be logged without modifications.
 */
public class DefaultMessageProcessor implements LogEventProcessor {

    private static final Logger logger = StatusLogger.getLogger();

    @Override
    public MessageRequestWrapper process(ProcessorAttributes processorAttributes) {
        logger.debug("Sending DEFAULT message.");
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .messageBody(processorAttributes.getMessage())
                .build();
        MessageRequestWrapper messageRequestWrapper = new MessageRequestWrapper();
        messageRequestWrapper.setSendMessageRequest(Collections.singletonList(sendMessageRequest));
        return messageRequestWrapper;
    }

}
