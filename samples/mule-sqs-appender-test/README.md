# Mule SQS appender sample

Small Mule 4.9 application that sends three log events through `log4j2-sqs-appender` 1.2.0.

`GET /sqs-size-test` writes three INFO logs. Each message ends with `###SQS-APPENDER-TEST-END###`.

| Log | Message body | What the appender does |
| --- | --- | --- |
| small | 50,095 bytes | At or under 256 KiB, so it is sent with `SendMessageBatch`. |
| medium | 500,099 bytes | Over 256 KiB and under `maxMessageBytes` (1,000,000), so it is sent with one `SendMessage`. |
| large | 1,100,088 bytes | Over 1 MB. This sample sets `largeMessageMode="DISCARD"`, so the queued body is `Can't display log message. Message max size exceeded. max size:1000000`. The end marker is not in that body. |

The sizes above are the log message text. The SQS body is the compact JSON event, which is a little larger. The small and medium sizes still land on the correct side of 256 KiB and 1 MB after that wrapping.

When `TRUNCATE` or `DISCARD` handles an oversized event, the appender also writes a WARN log with the original size, the limit, and the mode. That WARN follows the application's normal loggers, including this SQS appender. It does not include the original event.

## Before you run

1. Install this repository's 1.2.0 jar into the local Maven repository (`mvn -DskipTests package`, then `mvn install:install-file` with the built jar), or use a published 1.2.0.
2. Create an SQS queue named `sqs-appender-sample` in `us-east-1`.
3. If the queue was created before August 2025, set its `MaximumMessageSize` to `1048576`. The same applies to its dead-letter queue.

## Run

Open this folder in Anypoint Studio (Mule runtime 4.9, Java 17) and add these VM arguments:

```text
-Damazon.sqs.access.key=YOUR_KEY -Damazon.sqs.secret.key=YOUR_SECRET
```

Then call:

```text
http://localhost:8081/sqs-size-test
```

The response JSON reports the three message sizes. Check the `sqs-appender-sample` queue for the messages.

To try another oversized-message path, change `largeMessageMode` in `src/main/resources/log4j2.xml`. `EXTENDED` and `S3` require `s3BucketName`. `FIFO` sends the parts to a queue whose name ends with `.fifo`.
