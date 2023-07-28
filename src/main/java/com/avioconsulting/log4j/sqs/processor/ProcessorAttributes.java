package com.avioconsulting.log4j.sqs.processor;

public class ProcessorAttributes {

    private String message;
    private Integer maxMessageSize;
    private String bucketName;

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
