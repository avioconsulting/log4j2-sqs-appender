package com.avioconsulting.log4j.sqs.processor;

/** Strategies for preparing log events for delivery. */
public enum ProcessorType {
    /** Trim oversized text to the configured byte limit. */
    TRUNCATE,
    /** Send numbered parts to the FIFO queue in order. */
    FIFO,
    /** Write the event body to S3 instead of sending it to SQS. */
    S3,
    /** Send a size-limit notice instead of the oversized body. */
    DISCARD,
    /** Send the formatted event body unchanged. */
    DEFAULT,
    /** Use the SQS Extended Client to store the body in S3 and enqueue its reference. */
    EXTENDED
}
