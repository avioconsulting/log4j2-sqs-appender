package com.avioconsulting.log4j.sqs.client;

import com.avioconsulting.log4j.sqs.processor.LogEventProcessor;
import com.avioconsulting.log4j.sqs.processor.ProcessorType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class AVIOAWSClientSupplier {

	private final Map<String, AvioAWSClient> awsClientsMap = new HashMap<>() ;

	public AVIOAWSClientSupplier(AvioAWSClientAttributes attributes) {
		this.awsClientsMap.put(AWSClientType.SQS.name(), new AwsSQSClient(attributes) );
		this.awsClientsMap.put(AWSClientType.JAVAEXTENDED.name(), new AwsExtendendSQSClient(attributes));
	}

	public AvioAWSClient selectClient(String type){
		if(!awsClientsMap.containsKey(type)){
			return awsClientsMap.get(AWSClientType.SQS.name());
		}
		return awsClientsMap.get(type);
	}

}
