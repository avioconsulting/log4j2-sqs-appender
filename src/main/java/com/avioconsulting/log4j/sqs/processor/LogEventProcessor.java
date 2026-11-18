package com.avioconsulting.log4j.sqs.processor;

import com.avioconsulting.log4j.sqs.wrapper.MessageRequestWrapper;


/** Converts a formatted event into requests for the connector to dispatch. */
public interface LogEventProcessor {
    /**
     * Prepares the requests for this delivery strategy.
     * @param processorAttributes formatted message, byte limit and optional bucket
     * @return SQS sends and S3 uploads to perform for this event
     */
    MessageRequestWrapper process(ProcessorAttributes processorAttributes);
}
