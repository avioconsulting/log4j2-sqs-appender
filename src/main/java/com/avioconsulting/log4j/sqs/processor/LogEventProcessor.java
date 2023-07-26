package com.avioconsulting.log4j.sqs.processor;

import com.avioconsulting.log4j.sqs.wrapper.MessageRequestWrapper;


public interface LogEventProcessor {
    MessageRequestWrapper process(ProcessorAttributes processorAttributes);
}
