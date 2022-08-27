package com.avioconsulting.log4j.sqs.processor;

import com.amazonaws.services.sqs.model.SendMessageRequest;

import java.util.List;

public interface LogEventProcessor {

	List<SendMessageRequest> process(ProcessorAttributes processorAttributes);

	String getClientName() ;
}
