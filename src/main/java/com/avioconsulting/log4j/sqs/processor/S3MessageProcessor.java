package com.avioconsulting.log4j.sqs.processor;

import com.avioconsulting.log4j.sqs.wrapper.MessageRequestWrapper;
import com.avioconsulting.log4j.sqs.wrapper.S3ObjectRequest;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Collections;
import java.util.UUID;

/**
 * This class creates a putObject request given message attributes. ConnectorClient knows which s3 bucket
 * use to send this message.
 */
public class S3MessageProcessor implements LogEventProcessor {

    public static final String APPLICATION_TXT = "application/txt";
    private static final Logger logger = StatusLogger.getLogger();


    @Override
    public MessageRequestWrapper process(ProcessorAttributes processorAttributes) {
        logger.debug("Sending S3 message.");
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(processorAttributes.getBucketName())
                .key(UUID.randomUUID().toString())
                .contentType(APPLICATION_TXT)
                .build();
        MessageRequestWrapper messageRequestWrapper = new MessageRequestWrapper();
        messageRequestWrapper.setPutObjectRequest(Collections.singletonList(
                new S3ObjectRequest(putObjectRequest, processorAttributes.getMessage())));
        return messageRequestWrapper;
    }

}
