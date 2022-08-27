package com.avioconsulting.log4j.sqs.client;

import com.amazonaws.services.sqs.model.SendMessageRequest;

import java.util.List;

public interface AvioAWSClient {

	String getQueueURL(String queueName);
	void sendMessages(List<SendMessageRequest> messageList);

}
