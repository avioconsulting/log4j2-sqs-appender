package com.avioconsulting.log4j.sqs.processor;

import com.avioconsulting.log4j.sqs.client.AWSClientType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ProcessorSupplier {

	private ProcessorSupplier() {
	}

	private static final Map<String, Supplier<LogEventProcessor>> processorsMap ;

	static {
		Map<String, Supplier<LogEventProcessor>> aMap = new HashMap<>();
		aMap.put(ProcessorType.TRUNCATE.name(), ()->  new TruncateMessageProcessor(AWSClientType.SQS.name()));
		aMap.put(ProcessorType.FIFO.name(), ()->  new FifoMessageProcessor(AWSClientType.SQS.name()));
		aMap.put(ProcessorType.DISCARD.name(), ()->  new DiscardMessageProcessor(AWSClientType.SQS.name()));
		aMap.put(ProcessorType.DEFAULT.name(), ()->  new DefaultMessageProcessor(AWSClientType.SQS.name()));
		aMap.put(ProcessorType.EXTENDED.name(), ()->  new ExtendedMessageProcessor(AWSClientType.JAVAEXTENDED.name()));
		processorsMap = Collections.unmodifiableMap(aMap);
	}

	public static LogEventProcessor selectProcessor(String type){
		if(!processorsMap.containsKey(type)){
			return processorsMap.get(ProcessorType.TRUNCATE.name()).get();
		}
		return processorsMap.get(type).get();
	}

}
