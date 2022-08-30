package com.avioconsulting.log4j.sqs.processor;

import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.avioconsulting.log4j.sqs.wrapper.MessageRequestWrapper;

import java.util.Arrays;

/**
 * Default implementation will create a wrapper message within an unique message element.
 * Message will be logged without modifications.
 * @param
 * @return
 */
public class DefaultMessageProcessor implements LogEventProcessor{

	@Override public MessageRequestWrapper process(ProcessorAttributes processorAttributes) {
		SendMessageRequest sendMessageRequest = new SendMessageRequest();
		sendMessageRequest.setMessageBody(processorAttributes.getMessage());
		sendMessageRequest.setQueueUrl(processorAttributes.getQueueUrl());
		MessageRequestWrapper messageRequestWrapper = new MessageRequestWrapper();
		messageRequestWrapper.setSendMessageRequest(Arrays.asList(sendMessageRequest));
		return messageRequestWrapper;
	}

}
