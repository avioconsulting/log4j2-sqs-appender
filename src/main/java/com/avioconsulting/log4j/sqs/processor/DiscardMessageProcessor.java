package com.avioconsulting.log4j.sqs.processor;

import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;

import java.util.Arrays;
import java.util.List;

public class DiscardMessageProcessor implements LogEventProcessor{

	@Override public List<SendMessageRequest> process(String message, Integer messageSize, String queueUrl) {
		MutableLogEvent mutableLogEvent = new MutableLogEvent();
		mutableLogEvent.setLevel(Level.WARN);
		mutableLogEvent.setMessage(new SimpleMessage("Can't display log message. Message max size exceeded. max size:"+messageSize));
		SendMessageRequest sendMessageRequest = new SendMessageRequest();
		sendMessageRequest.setMessageBody(mutableLogEvent.getMessage().getFormattedMessage());
		sendMessageRequest.setQueueUrl(queueUrl);
		return Arrays.asList(sendMessageRequest); // check this.

	}
}
