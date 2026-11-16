package com.avioconsulting.log4j.sqs.client;

import com.avioconsulting.log4j.sqs.wrapper.S3ObjectRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * Sends log messages to AWS. Implementations must never log through Log4j appenders,
 * because the caller is itself an appender.
 */
public interface ConnectorClient {

    /**
     * Sends a message to the named SQS queue. The request queueUrl is filled in by the client.
     */
    void sendMessage(String queueName, SendMessageRequest request);

    /**
     * Sends a message to the named SQS queue through the SQS Extended Client, which stores
     * the body in S3 and queues a reference to it.
     */
    void sendExtendedMessage(String queueName, SendMessageRequest request);

    /**
     * Uploads a message to S3.
     */
    void putObject(S3ObjectRequest request);

    /**
     * Waits up to timeoutMillis for in-flight sends, then releases resources. Sends still running are cancelled.
     * Resource release starts even when the wait runs out. Closing the AWS clients can take longer than timeoutMillis.
     *
     * @return true if the wait and the resource release both finished within timeoutMillis
     */
    boolean close(long timeoutMillis);
}
