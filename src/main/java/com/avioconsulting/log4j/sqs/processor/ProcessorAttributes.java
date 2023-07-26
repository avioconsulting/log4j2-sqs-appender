package com.avioconsulting.log4j.sqs.processor;

public class ProcessorAttributes {

    private String message;
    private String queueUrl;
    private Integer maxMessageSize;
    private String bucketName;

    public ProcessorAttributes(String message, String queueUrl, Integer maxMessageSize, String bucketName) {
        this.message = message;
        this.queueUrl = queueUrl;
        this.maxMessageSize = maxMessageSize;
        this.bucketName = bucketName;
    }

    public String getMessage() {
        return message;
    }

    public String getQueueUrl() {
        return queueUrl;
    }

    public Integer getMaxMessageSize() {
        return maxMessageSize;
    }

    public String getBucketName() {
        return bucketName;
    }
}
