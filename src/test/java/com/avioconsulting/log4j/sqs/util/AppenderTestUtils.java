package com.avioconsulting.log4j.sqs.util;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.buffered.QueueBufferConfig;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.avioconsulting.log4j.sqs.extended.ExtendedLog4jSqsAppenderTest;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static com.avioconsulting.log4j.sqs.util.PropertiesProvider.*;

public class AppenderTestUtils {

    public static AmazonSQSBufferedAsyncClient getConnectionToSQSAsyncClient() {
        AWSStaticCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY));
        AmazonSQSAsync asyncClient;
        AmazonSQSAsyncClientBuilder clientBuilder = AmazonSQSAsyncClientBuilder.standard();
        asyncClient = clientBuilder.withRegion(String.valueOf(AWS_REGION))
                .withCredentials(credentialsProvider)
                .build();
        QueueBufferConfig config = new QueueBufferConfig().withMaxBatchOpenMs(MAX_BATCH_OPEN_MS)
                .withMaxBatchSize(MAX_BATCH_SIZE)
                .withMaxInflightOutboundBatches(MAX_INFLIGHT_OUTBOUND_BATCHES);
        return new AmazonSQSBufferedAsyncClient(asyncClient, config);
    }

    public static AmazonS3 getConnectionToAWSS3() {
        AWSStaticCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY));
        return  AmazonS3ClientBuilder
                .standard()
                .withRegion(AWS_REGION)
                .withCredentials(credentialsProvider)
                .build();
    }

    public static void deleteObjectsFromS3Bucket(AmazonS3 awsS3Client, String bucketName) {
        Stream<String> keysToDelete = awsS3Client
                .listObjects(bucketName)
                .getObjectSummaries()
                .stream()
                .map(s3ObjectSummary -> s3ObjectSummary.getKey());
        String[] keysArrayToDelete = keysToDelete.toArray(size -> new String[size]);

        if(keysArrayToDelete.length > 0){
            DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName).withKeys(keysArrayToDelete);
            awsS3Client.deleteObjects(deleteObjectsRequest);
        }
    }

    public static String readFileAsString() throws Exception {
        InputStream inputStream = Class.forName(ExtendedLog4jSqsAppenderTest.class.getName()).getResourceAsStream("/extendedText_1mb.txt");
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
    }

    public static String createQueue(AmazonSQS sqsClient, String prefix) {
        CreateQueueResult result = sqsClient.createQueue(prefix+"-"+AWS_SQS_LARGE_QUEUE_NAME);
        return result.getQueueUrl();
    }

}
