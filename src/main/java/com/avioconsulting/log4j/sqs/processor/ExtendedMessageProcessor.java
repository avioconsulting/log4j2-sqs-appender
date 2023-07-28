package com.avioconsulting.log4j.sqs.processor;

import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.avioconsulting.log4j.sqs.wrapper.MessageRequestWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

/**
 * ExtendedMessageProcessor takes the input message adding queueUrl but no modifies message content
 * ConnectorClient will push this message to sqs queue and s3 when message excedes 256kb, otherwise just
 * publish in sqs queue
 */
public class ExtendedMessageProcessor implements LogEventProcessor {

    private static final Logger logger = LogManager.getLogger(ExtendedMessageProcessor.class);

    @Override
    public MessageRequestWrapper process(ProcessorAttributes processorAttributes) {
        logger.debug("Sending EXTENDED message.");
        SendMessageRequest sendMessageRequest = new SendMessageRequest();
        sendMessageRequest.setMessageBody(processorAttributes.getMessage());
        MessageRequestWrapper messageRequestWrapper = new MessageRequestWrapper();
        messageRequestWrapper.setSendMessageRequest(Arrays.asList(sendMessageRequest));
        return messageRequestWrapper;
    }

}
