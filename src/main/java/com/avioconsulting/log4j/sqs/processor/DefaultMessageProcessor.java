package com.avioconsulting.log4j.sqs.processor;

import com.amazonaws.services.sqs.model.SendMessageRequest;

import java.util.Arrays;
import java.util.List;

public class DefaultMessageProcessor implements LogEventProcessor{

	private String clientName;
	public DefaultMessageProcessor(String awsClientName) {
		this.clientName = awsClientName;
	}

	/**
	 * Default implementation will return a sendMessage list with a unique message element
	 * without any modification
	 * @param processorAttributes
	 * @return
	 */
	@Override public List<SendMessageRequest> process(ProcessorAttributes processorAttributes) {
		SendMessageRequest sendMessageRequest = new SendMessageRequest();
		sendMessageRequest.setMessageBody(processorAttributes.getMessage());
		sendMessageRequest.setQueueUrl(processorAttributes.getQueueUrl());
		return Arrays.asList(sendMessageRequest);
	}

	@Override public String getClientName() {
		return this.clientName;
	}
}
