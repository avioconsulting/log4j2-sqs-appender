package com.avioconsulting.log4j.sqs.client;

import com.avioconsulting.log4j.sqs.SqsLimits;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Runs the real SDK v2 client against a local fake of the SQS JSON API.
 */
public class AwsConnectorClientTest {

    private static final String QUEUE = "mule-log-test";

    private HttpServer server;
    private ExecutorService serverExecutor;
    private final List<String> operations = new CopyOnWriteArrayList<>();
    private final List<Integer> sentBodyBytes = new CopyOnWriteArrayList<>();
    private final List<Integer> batchSizes = new CopyOnWriteArrayList<>();
    private final List<String> fifoBodies = new CopyOnWriteArrayList<>();
    private final CountDownLatch firstFifoRequest = new CountDownLatch(1);
    private final CountDownLatch releaseFirstFifo = new CountDownLatch(1);
    private volatile boolean failSends;
    private volatile long batchResponseDelayMs;
    private volatile boolean holdFirstFifoSend;

    @Before
    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        serverExecutor = Executors.newCachedThreadPool();
        server.setExecutor(serverExecutor);
        server.createContext("/", this::handle);
        server.start();
    }

    @After
    public void stopServer() {
        server.stop(0);
        serverExecutor.shutdownNow();
    }

    @Test
    public void sendsMessageOver256KiBWithDirectSendMessage() {
        AwsConnectorClient client = client(10, 100);
        client.sendMessage(QUEUE, request(repeat('a', 300_000)));
        client.close(5_000);

        assertTrue(operations.contains("SendMessage"));
        assertEquals(Integer.valueOf(300_000), sentBodyBytes.get(0));
    }

    @Test
    public void sendsMessageOf1MiBWithDirectSendMessage() {
        AwsConnectorClient client = client(10, 100);
        client.sendMessage(QUEUE, request(repeat('a', SqsLimits.MAX_MESSAGE_BYTES)));
        client.close(5_000);

        assertEquals(Integer.valueOf(SqsLimits.MAX_MESSAGE_BYTES), sentBodyBytes.get(0));
    }

    @Test
    public void batchesSmallMessages() {
        AwsConnectorClient client = client(10, 200);
        for (int i = 0; i < 5; i++) {
            client.sendMessage(QUEUE, request("message " + i));
        }
        client.close(5_000);

        assertEquals(5, sentBodyBytes.size());
        assertTrue(operations.contains("SendMessageBatch"));
        assertTrue(batchSizes.stream().anyMatch(size -> size > 1));
    }

    @Test
    public void resolvesQueueUrlOnce() {
        AwsConnectorClient client = client(1, 0);
        client.sendMessage(QUEUE, request("one"));
        client.sendMessage(QUEUE, request("two"));
        client.close(5_000);

        assertEquals(1, operations.stream().filter("GetQueueUrl"::equals).count());
        assertEquals(2, sentBodyBytes.size());
    }

    @Test
    public void fifoPartIsAcceptedBeforeTheNextPartIsSent() throws Exception {
        holdFirstFifoSend = true;
        AwsConnectorClient client = client(10, 50);
        Thread sender = new Thread(() -> {
            client.sendMessage(QUEUE, fifoRequest("part-1"));
            client.sendMessage(QUEUE, fifoRequest("part-2"));
        });
        sender.start();

        assertTrue(firstFifoRequest.await(5, TimeUnit.SECONDS));
        Thread.sleep(300);
        assertEquals(Arrays.asList("part-1"), fifoBodies);

        releaseFirstFifo.countDown();
        sender.join(10_000);
        assertEquals(Arrays.asList("part-1", "part-2"), fifoBodies);
        client.close(5_000);
    }

    @Test
    public void closeFinishesWithinTimeout() {
        AwsConnectorClient client = client(1, 0);
        client.sendMessage(QUEUE, request("one"));

        assertTrue(client.close(5_000));
    }

    @Test
    public void closeFlushesBufferedMessagesBeforeBatchIntervalExpires() {
        // The batch timer cannot fire within the shutdown budget.
        AwsConnectorClient client = client(10, 30_000);
        client.sendMessage(QUEUE, request("buffered message"));

        boolean finished = client.close(5_000);

        assertTrue("Shutdown should finish against a healthy endpoint", finished);
        assertTrue(operations.contains("SendMessageBatch"));
        assertEquals(1, sentBodyBytes.size());
    }

    @Test
    public void closeTimeoutBoundsShutdownWhenSqsIsSlow() throws InterruptedException {
        batchResponseDelayMs = 800;
        AwsConnectorClient client = client(10, 50);
        client.sendMessage(QUEUE, request("slow"));

        long start = System.nanoTime();
        boolean finished = client.close(100);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertFalse(finished);
        assertTrue("close took " + elapsedMs + " ms", elapsedMs < 600);
        Thread.sleep(400);
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if ("log4j2-sqs-appender-shutdown".equals(thread.getName())) {
                fail("shutdown thread was still running after the timeout");
            }
        }
    }

    @Test
    public void sendFailureDoesNotThrowToCaller() {
        failSends = true;
        AwsConnectorClient client = client(1, 0);
        client.sendMessage(QUEUE, request("will fail"));
        client.close(5_000);

        assertTrue(operations.contains("SendMessage"));
    }

    @Test
    public void batchSizeIsClampedToSqsLimits() {
        assertEquals(10, AwsConnectorClient.batchSize(null));
        assertEquals(10, AwsConnectorClient.batchSize(50));
        assertEquals(1, AwsConnectorClient.batchSize(0));
        assertEquals(5, AwsConnectorClient.batchSize(5));
    }

    @Test
    public void describeUnwrapsAndShortensErrors() {
        String text = AwsConnectorClient.describe(new CompletionException(new IllegalStateException(repeat('x', 2_000))));
        assertTrue(text.startsWith("java.lang.IllegalStateException: xxx"));
        assertTrue(text.length() < 600);
    }

    private AwsConnectorClient client(int maxBatchSize, int maxBatchOpenMs) {
        ConnectorClientAttributes attributes = new ConnectorClientAttributes(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")),
                Region.US_EAST_1, maxBatchOpenMs, maxBatchSize, 5, null,
                "http://localhost:" + server.getAddress().getPort());
        return new AwsConnectorClient(attributes, null);
    }

    private static SendMessageRequest request(String body) {
        return SendMessageRequest.builder().messageBody(body).build();
    }

    private static SendMessageRequest fifoRequest(String body) {
        return SendMessageRequest.builder().messageBody(body).messageGroupId("group-1").build();
    }

    @SuppressWarnings("unchecked")
    private void handle(HttpExchange exchange) throws IOException {
        String target = exchange.getRequestHeaders().getFirst("X-Amz-Target");
        String operation = target == null ? "" : target.substring(target.indexOf('.') + 1);
        operations.add(operation);
        JSONObject response = new JSONObject();
        int status = 200;
        try {
            JSONObject request = (JSONObject) new JSONParser().parse(
                    IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8));
            if ("GetQueueUrl".equals(operation)) {
                response.put("QueueUrl", "http://localhost/000000000000/" + request.get("QueueName"));
            } else if (failSends) {
                status = 400;
                response.put("__type", "com.amazonaws.sqs#InvalidParameterValue");
                response.put("message", "Simulated failure");
            } else if ("SendMessage".equals(operation)) {
                String body = (String) request.get("MessageBody");
                sentBodyBytes.add(body.getBytes(StandardCharsets.UTF_8).length);
                if (request.get("MessageGroupId") != null) {
                    fifoBodies.add(body);
                    if (holdFirstFifoSend && fifoBodies.size() == 1) {
                        firstFifoRequest.countDown();
                        releaseFirstFifo.await(5, TimeUnit.SECONDS);
                    }
                }
                response.put("MessageId", UUID.randomUUID().toString());
                response.put("MD5OfMessageBody", md5(body));
            } else if ("SendMessageBatch".equals(operation)) {
                if (batchResponseDelayMs > 0) {
                    Thread.sleep(batchResponseDelayMs);
                }
                JSONArray entries = (JSONArray) request.get("Entries");
                batchSizes.add(entries.size());
                JSONArray successful = new JSONArray();
                for (Object entryObject : entries) {
                    JSONObject entry = (JSONObject) entryObject;
                    String body = (String) entry.get("MessageBody");
                    sentBodyBytes.add(body.getBytes(StandardCharsets.UTF_8).length);
                    JSONObject result = new JSONObject();
                    result.put("Id", entry.get("Id"));
                    result.put("MessageId", UUID.randomUUID().toString());
                    result.put("MD5OfMessageBody", md5(body));
                    successful.add(result);
                }
                response.put("Successful", successful);
                response.put("Failed", new JSONArray());
            }
        } catch (Exception e) {
            status = 500;
            response.put("message", e.toString());
        }
        byte[] bytes = response.toJSONString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/x-amz-json-1.0");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String md5(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.UTF_8));
        return String.format("%032x", new BigInteger(1, digest));
    }

    private static String repeat(char c, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(c);
        }
        return builder.toString();
    }
}
