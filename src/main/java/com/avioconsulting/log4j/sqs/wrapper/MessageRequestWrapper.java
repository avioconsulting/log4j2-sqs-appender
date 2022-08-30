package com.avioconsulting.log4j.sqs.wrapper;

import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import java.util.List;

public class MessageRequestWrapper {
	private List<SendMessageRequest> sendMessageRequest;
	private List<PutObjectRequest> putObjectRequest;


	public List<SendMessageRequest> getSendMessageRequest() {
		return sendMessageRequest;
	}

	public void setSendMessageRequest(List<SendMessageRequest> sendMessageRequest) {
		this.sendMessageRequest = sendMessageRequest;
	}

	public List<PutObjectRequest> getPutObjectRequest() {
		return putObjectRequest;
	}

	public void setPutObjectRequest(List<PutObjectRequest> putObjectRequest) {
		this.putObjectRequest = putObjectRequest;
	}
}
