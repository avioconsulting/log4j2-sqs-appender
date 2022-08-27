package com.avioconsulting.log4j.sqs.processor;

import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;

import java.util.Arrays;
import java.util.List;

public class DiscardMessageProcessor implements LogEventProcessor{

	private String clientName;
	public DiscardMessageProcessor(String client) {
		this.clientName = client;
	}

	private void validateProcessorAttributes(ProcessorAttributes processorAttributes){
		if(processorAttributes.getMessage() == null ||
				processorAttributes.getQueueUrl() == null){
			throw new RuntimeException("queueUl or message not defined for: "+this.getClass().getName());
		}
	}

	@Override public List<SendMessageRequest> process(ProcessorAttributes processorAttributes) {
		validateProcessorAttributes(processorAttributes);
		MutableLogEvent mutableLogEvent = new MutableLogEvent();
		mutableLogEvent.setLevel(Level.WARN);
		mutableLogEvent.setMessage(new SimpleMessage("Can't display log message. Message max size exceeded. max size:"+processorAttributes.getMaxMessageSize()));
		SendMessageRequest sendMessageRequest = new SendMessageRequest();
		sendMessageRequest.setMessageBody(mutableLogEvent.getMessage().getFormattedMessage());
		sendMessageRequest.setQueueUrl(processorAttributes.getQueueUrl());
		return Arrays.asList(sendMessageRequest); // check this.
	}

	@Override public String getClientName() {
		return this.clientName;
	}
}
