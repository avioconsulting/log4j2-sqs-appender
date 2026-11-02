package com.avioconsulting.log4j.sqs.util;

import com.avioconsulting.log4j.sqs.extended.ExtendedLog4jSqsAppenderTest;
import org.apache.commons.io.IOUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.avioconsulting.log4j.sqs.util.PropertiesProvider.*;

public class AppenderTestUtils {

    private static StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(AWS_ACCESS_KEY, AWS_SECRET_KEY));
    }

    public static SqsClient getConnectionToSQSClient() {
        SqsClientBuilder clientBuilder = SqsClient.builder()
                .region(Region.of(AWS_REGION))
                .credentialsProvider(credentialsProvider());

        if (ENDPOINT_URL != null && !ENDPOINT_URL.isEmpty()) {
            clientBuilder.endpointOverride(URI.create(ENDPOINT_URL));
        }

        return clientBuilder.build();
    }

    public static S3Client getConnectionToAWSS3() {
        S3ClientBuilder clientBuilder = S3Client.builder()
                .region(Region.of(AWS_REGION))
                .credentialsProvider(credentialsProvider());

        if (ENDPOINT_URL != null && !ENDPOINT_URL.isEmpty()) {
            clientBuilder.endpointOverride(URI.create(ENDPOINT_URL));
        }

        return clientBuilder.build();
    }

    public static void deleteObjectsFromS3Bucket(S3Client awsS3Client, String bucketName) {
        List<ObjectIdentifier> keysToDelete = awsS3Client
                .listObjects(r -> r.bucket(bucketName))
                .contents()
                .stream()
                .map(s3Object -> ObjectIdentifier.builder().key(s3Object.key()).build())
                .collect(Collectors.toList());

        if (!keysToDelete.isEmpty()) {
            awsS3Client.deleteObjects(r -> r.bucket(bucketName).delete(Delete.builder().objects(keysToDelete).build()));
        }
    }

    public static String readFileAsString() throws Exception {
        InputStream inputStream = Class.forName(ExtendedLog4jSqsAppenderTest.class.getName()).getResourceAsStream("/extendedText_1mb.txt");
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }

    /**
     * Creates the queue that the appender for the given prefix sends to, as configured in log4j2.xml.
     */
    public static String createQueue(SqsClient sqsClient, String prefix) {
        return sqsClient.createQueue(r -> r.queueName(prefix + "-" + AWS_SQS_QUEUE_NAME)).queueUrl();
    }

    /**
     * Waits up to waitTimeSeconds for messages and deletes the received ones from the queue.
     */
    public static List<Message> receiveMessages(SqsClient sqsClient, String queueUrl, int maxMessages, int waitTimeSeconds) {
        long deadline = System.currentTimeMillis() + waitTimeSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(maxMessages)
                    .waitTimeSeconds(Math.min(20, waitTimeSeconds))
                    .build()).messages();
            if (!messages.isEmpty()) {
                for (Message message : messages) {
                    sqsClient.deleteMessage(r -> r.queueUrl(queueUrl).receiptHandle(message.receiptHandle()));
                }
                return messages;
            }
        }
        return Collections.emptyList();
    }

}
