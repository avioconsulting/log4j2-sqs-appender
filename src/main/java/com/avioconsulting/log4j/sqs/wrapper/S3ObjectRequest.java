package com.avioconsulting.log4j.sqs.wrapper;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * An S3 PutObject request together with the content to upload.
 */
public class S3ObjectRequest {
    private final PutObjectRequest putObjectRequest;
    private final String content;

    /**
     * @param putObjectRequest S3 destination and object metadata
     * @param content text to encode as UTF-8 when uploaded
     */
    public S3ObjectRequest(PutObjectRequest putObjectRequest, String content) {
        this.putObjectRequest = putObjectRequest;
        this.content = content;
    }

    /** @return request identifying the bucket, key and object metadata */
    public PutObjectRequest getPutObjectRequest() {
        return putObjectRequest;
    }

    /** @return text to upload as the object body */
    public String getContent() {
        return content;
    }
}
