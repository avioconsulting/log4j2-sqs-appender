package com.avioconsulting.log4j.sqs.processor;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.avioconsulting.log4j.sqs.wrapper.MessageRequestWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

/**
 * This class creates a putObject request given message attributes. ConnectorClient knows which s3 bucket
 * use to send this message.
 */
public class S3MessageProcessor implements LogEventProcessor {

    public static final String APPLICATION_TXT = "application/txt";
    private static final Logger logger = LogManager.getLogger(S3MessageProcessor.class);


    @Override
    public MessageRequestWrapper process(ProcessorAttributes processorAttributes) {
        logger.debug("Sending S3 message.");
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(APPLICATION_TXT);
        objectMetadata.setContentLength(processorAttributes.getMessage().length());
        InputStream stream = new ByteArrayInputStream(processorAttributes.getMessage().getBytes(StandardCharsets.UTF_8));
        PutObjectRequest putObjectRequest = new PutObjectRequest(processorAttributes.getBucketName(), UUID.randomUUID().toString(), stream, objectMetadata);
        MessageRequestWrapper messageRequestWrapper = new MessageRequestWrapper();
        messageRequestWrapper.setPutObjectRequest(Collections.singletonList(putObjectRequest));
        return messageRequestWrapper;
    }

}
