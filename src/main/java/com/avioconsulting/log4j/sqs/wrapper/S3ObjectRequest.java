package com.avioconsulting.log4j.sqs.wrapper;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * An S3 PutObject request together with the content to upload.
 */
public class S3ObjectRequest {
    private final PutObjectRequest putObjectRequest;
    private final String content;

    public S3ObjectRequest(PutObjectRequest putObjectRequest, String content) {
        this.putObjectRequest = putObjectRequest;
        this.content = content;
    }

    public PutObjectRequest getPutObjectRequest() {
        return putObjectRequest;
    }

    public String getContent() {
        return content;
    }
}
