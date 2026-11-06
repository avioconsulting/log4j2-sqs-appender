package com.avioconsulting.log4j.sqs.wrapper;

import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Collections;
import java.util.List;

public class MessageRequestWrapper {
	private List<SendMessageRequest> sendMessageRequest = Collections.emptyList();
	private List<S3ObjectRequest> putObjectRequest = Collections.emptyList();


	public List<SendMessageRequest> getSendMessageRequest() {
		return sendMessageRequest;
	}

	public void setSendMessageRequest(List<SendMessageRequest> sendMessageRequest) {
		this.sendMessageRequest = sendMessageRequest;
	}

	public List<S3ObjectRequest> getPutObjectRequest() {
		return putObjectRequest;
	}

	public void setPutObjectRequest(List<S3ObjectRequest> putObjectRequest) {
		this.putObjectRequest = putObjectRequest;
	}
}
