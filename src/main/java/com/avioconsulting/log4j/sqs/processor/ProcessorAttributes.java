package com.avioconsulting.log4j.sqs.processor;

/** Input for processing one formatted log event. */
public class ProcessorAttributes {

    private final String message;
    private final Integer maxMessageSize;
    private final String bucketName;

    /**
     * @param message log event text produced by the layout
     * @param maxMessageSize maximum SQS message body size in bytes
     * @param bucketName destination bucket for S3 uploads
     */
    public ProcessorAttributes(String message, Integer maxMessageSize, String bucketName) {
        this.message = message;
        this.maxMessageSize = maxMessageSize;
        this.bucketName = bucketName;
    }


    /** @return maximum SQS message body size in bytes */
    public Integer getMaxMessageSize() {
        return maxMessageSize;
    }

    /** @return destination bucket for S3 uploads */
    public String getBucketName() {
        return bucketName;
    }

    /** @return formatted log event text */
    public String getMessage() {
        return message;
    }
}
