package com.avioconsulting.log4j.sqs;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.buffered.QueueBufferConfig;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.config.Configuration;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class SqsManager extends AbstractManager {
    private final Configuration configuration;
    private final String awsAccessKey;
    private final String awsSecretKey;
    private final String awsRegion;
    private final String queueName;
    private final String largeMessageQueueName;
    private final Integer maxBatchOpenMs;
    private final Integer maxBatchSize;
    private final Integer maxInflightOutboundBatches;
    private final Integer maxMessageBytes;
    private final Boolean largeMessagesEnabled;
    private String queueUrl;
    private String largeMessageQueueUrl;
    private AmazonSQSBufferedAsyncClient client;

    private static final Logger logger = LogManager.getLogger();

    protected SqsManager(final Configuration configuration,
                         final LoggerContext loggerContext,
                         final String name,
                         final String awsRegion,
                         final String awsAccessKey,
                         final String awsSecretKey,
                         final String queueName,
                         final String largeMessageQueueName,
                         final Integer maxBatchOpenMs,
                         final Integer maxBatchSize,
                         final Integer maxInflightOutboundBatches,
                         final Integer maxMessageBytes,
                         final Boolean largeMessagesEnabled) {
        super(loggerContext, name);
        this.configuration = Objects.requireNonNull(configuration);
        this.awsAccessKey = awsAccessKey;
        this.awsSecretKey = awsSecretKey;
        this.awsRegion = awsRegion;
        this.queueName = queueName;
        this.largeMessageQueueName = largeMessageQueueName == null ? queueName.concat(".fifo") : largeMessageQueueName;
        this.maxBatchOpenMs = maxBatchOpenMs == null ? 200 : maxBatchOpenMs;
        this.maxBatchSize = maxBatchSize == null ? 10 : maxBatchSize;
        this.maxInflightOutboundBatches = maxInflightOutboundBatches == null ? 5 : maxInflightOutboundBatches;
        this.maxMessageBytes = maxMessageBytes == null ? 250000 : maxMessageBytes;
        this.largeMessagesEnabled = largeMessagesEnabled != null && largeMessagesEnabled;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void startup() {
        // This default implementation does nothing
    }

    public void send(final Layout<?> layout, final LogEvent event) {
        try {
            String message = new String(layout.toByteArray(event), StandardCharsets.UTF_8);
            int messageLength = message.getBytes().length;
            logger.debug("Message length: " + messageLength);

            // We only need to split the message if it's larger than the maximum length
            if (messageLength > this.maxMessageBytes) {
                if (!this.largeMessagesEnabled) {
                    logger.debug("Sending truncated message");
                    SendMessageRequest request = new SendMessageRequest(this.queueUrl, truncateStringByByteLength(message, "UTF-8", this.maxMessageBytes));
                    this.client.sendMessageAsync(request);
                } else {
                    logger.debug("Splitting large message");
                    // Generate Message Hash to use as the group ID
                    UUID uuid = UUID.randomUUID();
                    logger.debug("Large Message UUID: " + uuid);
                    //String base64Message = Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
                    String[] splitMessage = splitStringByByteLength(message, "UTF-8", maxMessageBytes);
                    for (int i = 0; i < splitMessage.length; i++) {
                        logger.debug(String.format("Sending message %d of %d", i + 1, splitMessage.length));
                        SendMessageRequest request = new SendMessageRequest(this.largeMessageQueueUrl, String.format("currentPart=%d|totalParts=%d|uuid=%s|message=%s", i + 1, splitMessage.length, uuid, splitMessage[i]));
                        request.setMessageGroupId(uuid.toString());
                        this.getClient().sendMessageAsync(request);
                    }
                }
            } else {
                SendMessageRequest request = new SendMessageRequest(this.queueUrl, message);
                this.getClient().sendMessageAsync(request);
            }
        } catch (Exception e) {
            logger.error("Failed to send message to SQS", e);
        }
    }

    private AmazonSQSBufferedAsyncClient getClient() {
        if (this.client == null) {
            AmazonSQSAsync asyncClient;
            try {
                logger.debug("Initializing SQS Client: " + this);
                AWSStaticCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey));
                AmazonSQSAsyncClientBuilder clientBuilder = AmazonSQSAsyncClientBuilder.standard();
                asyncClient = clientBuilder.withRegion(awsRegion).withCredentials(credentialsProvider).build();
                QueueBufferConfig config = new QueueBufferConfig().withMaxBatchOpenMs(maxBatchOpenMs).withMaxBatchSize(maxBatchSize).withMaxInflightOutboundBatches(maxInflightOutboundBatches);
                this.client = new AmazonSQSBufferedAsyncClient(asyncClient, config);
                this.queueUrl = this.client.getQueueUrl(this.queueName).getQueueUrl();
                if (largeMessagesEnabled) {
                    logger.debug("Large Messages Enabled. Large Message Queue: " + this.largeMessageQueueName);
                    this.largeMessageQueueUrl = this.client.getQueueUrl(this.largeMessageQueueName).getQueueUrl();
                }
            } catch (Exception e) {
                logger.error("Failed to initialize SQS Client", e);
                throw e;
            }
        }

        return this.client;
    }

    public String toString() {
        return "[ region=" + this.awsRegion + ", maxBatchOpenMs=" + this.maxBatchOpenMs + ", maxBatchSize=" + this.maxBatchSize + ", maxInflightOutboundBatches=" + this.maxInflightOutboundBatches + " ]";
    }

    /**
     * Split the input string into a list of strings, each with a maximum memory size of maxsize bytes.
     *
     * @param src the source string
     * @param encoding the encoding to use for the output string
     * @param maxsize the maximum size of the string parts in bytes
     * @return an array of strings, each with a maximum length of maxsize bytes
     */
    protected static String[] splitStringByByteLength(final String src, final String encoding, final int maxsize) {
        Charset cs = Charset.forName(encoding);
        CharsetEncoder coder = cs.newEncoder();
        ByteBuffer out = ByteBuffer.allocate(maxsize);  // output buffer of required size
        CharBuffer in = CharBuffer.wrap(src);
        List<String> ss = new ArrayList<>();            // a list to store the chunks
        int pos = 0;
        while (true) {
            CoderResult result = coder.encode(in, out, true); // try to encode as much as possible
            int newpos = src.length() - in.length();
            String s = src.substring(pos, newpos);
            ss.add(s);                                  // add what has been encoded to the list
            pos = newpos;                               // store new input position
            out.rewind();                               // and rewind output buffer
            if (!result.isOverflow()) {
                break;                                  // everything has been encoded
            }
        }
        return ss.toArray(new String[0]);
    }

    /**
     * Truncate a string to a specific number of bytes.
     *
     * @param src the string to be truncated
     * @param encoding the character encoding to use for the truncated string
     * @param maxsize the maximum size of the string in bytes
     * @return the truncated string
     */
    protected static String truncateStringByByteLength(final String src, final String encoding, final int maxsize) {
        Charset cs = Charset.forName(encoding);
        CharsetEncoder coder = cs.newEncoder();
        ByteBuffer out = ByteBuffer.allocate(maxsize);
        CharBuffer in = CharBuffer.wrap(src);
        coder.encode(in, out, true);
        int pos = src.length() - in.length();
        return src.substring(0, pos);
    }
}