package com.avioconsulting.log4j.sqs.processor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * This supplier knows how to create Processor given a ProcessorType
 */
public class ProcessorSupplier {

	private ProcessorSupplier() {
	}

	private static final Map<String, Supplier<LogEventProcessor>> processorsMap ;

	static {
		Map<String, Supplier<LogEventProcessor>> aMap = new HashMap<>();
		aMap.put(ProcessorType.TRUNCATE.name(), TruncateMessageProcessor::new);
		aMap.put(ProcessorType.FIFO.name(), FifoMessageProcessor::new);
		aMap.put(ProcessorType.DISCARD.name(), DiscardMessageProcessor::new);
		aMap.put(ProcessorType.DEFAULT.name(), DefaultMessageProcessor::new);
		aMap.put(ProcessorType.EXTENDED.name(), ExtendedMessageProcessor::new);
		aMap.put(ProcessorType.S3.name(), S3MessageProcessor::new);
		processorsMap = Collections.unmodifiableMap(aMap);
	}

	public static LogEventProcessor selectProcessor(String type){
		if(!processorsMap.containsKey(type)){
			return processorsMap.get(ProcessorType.TRUNCATE.name()).get();
		}
		return processorsMap.get(type).get();
	}

}
