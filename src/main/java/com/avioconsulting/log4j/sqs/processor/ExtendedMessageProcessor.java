package com.avioconsulting.log4j.sqs.processor;

import com.avioconsulting.log4j.sqs.wrapper.MessageRequestWrapper;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Collections;

/**
 * ExtendedMessageProcessor keeps the message content unchanged. ConnectorClient sends it with the
 * SQS Extended Client, which stores the body in S3 and queues a reference to it.
 */
public class ExtendedMessageProcessor implements LogEventProcessor {

    private static final Logger logger = StatusLogger.getLogger();

    @Override
    public MessageRequestWrapper process(ProcessorAttributes processorAttributes) {
        logger.debug("Sending EXTENDED message.");
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .messageBody(processorAttributes.getMessage())
                .build();
        MessageRequestWrapper messageRequestWrapper = new MessageRequestWrapper();
        messageRequestWrapper.setSendMessageRequest(Collections.singletonList(sendMessageRequest));
        return messageRequestWrapper;
    }

}
