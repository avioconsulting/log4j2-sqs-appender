package com.avioconsulting.log4j.sqs.wrapper;

import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Collections;
import java.util.List;

/** SQS sends and S3 uploads produced by processing a single event. */
public class MessageRequestWrapper {
	private List<SendMessageRequest> sendMessageRequest = Collections.emptyList();
	private List<S3ObjectRequest> putObjectRequest = Collections.emptyList();


	/** @return SQS requests in dispatch order; empty when only S3 is used */
	public List<SendMessageRequest> getSendMessageRequest() {
		return sendMessageRequest;
	}

	/** @param sendMessageRequest SQS requests in dispatch order */
	public void setSendMessageRequest(List<SendMessageRequest> sendMessageRequest) {
		this.sendMessageRequest = sendMessageRequest;
	}

	/** @return uploads to perform after any SQS sends */
	public List<S3ObjectRequest> getPutObjectRequest() {
		return putObjectRequest;
	}

	/** @param putObjectRequest uploads to perform after any SQS sends */
	public void setPutObjectRequest(List<S3ObjectRequest> putObjectRequest) {
		this.putObjectRequest = putObjectRequest;
	}
}
