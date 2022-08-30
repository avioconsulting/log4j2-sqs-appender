package com.avioconsulting.log4j.sqs.processor;

import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.avioconsulting.log4j.sqs.wrapper.MessageRequestWrapper;

import java.util.Arrays;

/**
 * ExtendedMessageProcessor takes the input message adding queueUrl but no modifies message content
 * ConnectorClient will push this message to sqs queue and s3 when message excedes 256kb, otherwise just
 * publish in sqs queue
 */
public class ExtendedMessageProcessor implements LogEventProcessor {

	@Override public MessageRequestWrapper process(ProcessorAttributes processorAttributes) {
		SendMessageRequest sendMessageRequest = new SendMessageRequest();
		sendMessageRequest.setMessageBody(processorAttributes.getMessage());
		sendMessageRequest.setQueueUrl(processorAttributes.getQueueUrl());
		MessageRequestWrapper messageRequestWrapper = new MessageRequestWrapper();
		messageRequestWrapper.setSendMessageRequest(Arrays.asList(sendMessageRequest));
		return messageRequestWrapper;
	}

}
