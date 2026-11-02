package com.avioconsulting.log4j.sqs;

import com.avioconsulting.log4j.sqs.client.ConnectorClient;
import com.avioconsulting.log4j.sqs.wrapper.S3ObjectRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * ConnectorClient that records calls instead of sending them to AWS.
 */
class RecordingConnectorClient implements ConnectorClient {

    static class Sent {
        final String queueName;
        final SendMessageRequest request;
        final boolean extended;

        Sent(String queueName, SendMessageRequest request, boolean extended) {
            this.queueName = queueName;
            this.request = request;
            this.extended = extended;
        }
    }

    final List<Sent> sent = new ArrayList<>();
    final List<S3ObjectRequest> s3Objects = new ArrayList<>();
    long closedWithTimeoutMillis = -1;

    @Override
    public void sendMessage(String queueName, SendMessageRequest request) {
        sent.add(new Sent(queueName, request, false));
    }

    @Override
    public void sendExtendedMessage(String queueName, SendMessageRequest request) {
        sent.add(new Sent(queueName, request, true));
    }

    @Override
    public void putObject(S3ObjectRequest request) {
        s3Objects.add(request);
    }

    @Override
    public void close(long timeoutMillis) {
        closedWithTimeoutMillis = timeoutMillis;
    }
}
