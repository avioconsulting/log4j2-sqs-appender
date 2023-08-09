package com.avioconsulting.log4j.sqs.processor;

public class ProcessorAttributes {

    private final String message;
    private final Integer maxMessageSize;
    private final String bucketName;

    public ProcessorAttributes(String message, Integer maxMessageSize, String bucketName) {
        this.message = message;
        this.maxMessageSize = maxMessageSize;
        this.bucketName = bucketName;
    }


    public Integer getMaxMessageSize() {
        return maxMessageSize;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getMessage() {
        return message;
    }
}
