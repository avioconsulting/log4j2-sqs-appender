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
     * @param queueName destination queue name, not its URL
     * @param request message body and SQS delivery attributes
     */
    void sendMessage(String queueName, SendMessageRequest request);

    /**
     * Sends a message to the named SQS queue through the SQS Extended Client, which stores
     * the body in S3 and queues a reference to it.
     * @param queueName destination queue for the reference
     * @param request message to deliver through the extended client
     */
    void sendExtendedMessage(String queueName, SendMessageRequest request);

    /**
     * Uploads a message to S3.
     * @param request destination object and text to upload
     */
    void putObject(S3ObjectRequest request);

    /**
     * Waits up to timeoutMillis for in-flight sends, then releases resources. Sends still running are cancelled.
     * Resource release starts even when the wait runs out. Closing the AWS clients can take longer than timeoutMillis.
     *
     * @param timeoutMillis shutdown wait budget in milliseconds
     * @return true if the wait and the resource release both finished within timeoutMillis
     */
    boolean close(long timeoutMillis);
}
