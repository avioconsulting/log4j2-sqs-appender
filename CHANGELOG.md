# Changelog

User-visible changes for the SQS appender. Dates are the release commits in this repository.

## 1.2.0 - 2026-11-05

Existing `log4j2.xml` configurations work without changes.

* Replaced the AWS SDK for Java 1.x with the AWS SDK for Java 2.x (SDK 2.57.8). The application now includes the SDK 2.x libraries, including Netty and Apache HttpClient 5.
* Sends messages up to 1 MiB (1,048,576 bytes). In 1.1.0, a body larger than 256 KiB failed with `Failed to schedule request` because the SDK 1.x buffered client rejected it before the send.
* The default `maxMessageBytes` is 1048576. In 1.1.0 it was 250000.
* Messages of 256 KiB or smaller can share one `SendMessageBatch` call. A larger message, up to 1 MiB, is sent on its own with `SendMessage`. The 256 KiB value is a batching cutoff, not the SQS message limit.
* FIFO parts are sent one at a time. The next part is sent only after SQS accepts the previous part, so the queue keeps the part order. The logging thread waits for one SQS round trip per part. `EXTENDED` sends and `S3` uploads wait the same way. A normal SQS send does not.
* `maxMessageBytes` must be at least 132 in `FIFO` mode, so the part header and one character fit. A smaller limit is rejected instead of looping.
* Each FIFO part, including its header, fits in `maxMessageBytes`. A queue with `MaximumMessageSize` equal to `maxMessageBytes` accepts every part.
* `maxInflightOutboundBatches` is still accepted so old configurations load. It has no effect on the SDK 2.x client. Outstanding sends are limited by the SDK HTTP client (50 connections, 10,000 queued requests, 10-second acquire timeout).
* A message larger than `maxMessageBytes` writes a normal log line before it is handled. `TRUNCATE` and `DISCARD` are WARN. `FIFO`, `EXTENDED`, and `S3` are INFO. The line contains the size, the limit, and the mode, not the original event. That line never produces another one, even when it is itself over `maxMessageBytes`.
* Stopping waits up to the Log4j shutdown timeout for in-flight sends, then cancels anything still running and closes the AWS clients. Closing the clients can take longer than the timeout. An incomplete stop is reported to the Log4j status logger.
* Added `samples/mule-sqs-appender-test`, a Mule 4.9 application that logs a small, a medium, and an oversized message.
* Added LocalStack integration tests. `mvn clean verify` starts LocalStack 4.14.0 and needs Docker.

## 1.1.0 - 2022-03-22

* Messages larger than `maxMessageBytes` are truncated to that size before they are sent.
* Added `largeMessagesEnabled`. When it is `true`, a message larger than `maxMessageBytes` is split into ordered parts on an SQS FIFO queue instead of being truncated. Each part uses this form: `currentPart=1|totalParts=5|uuid=<uuid>|message=<part>`.
* Added `largeMessageQueueName`. When it is omitted, the appender uses `queueName` with `.fifo` appended.
* The default `maxMessageBytes` is 250000, under the 256 KiB SQS limit of that time.
* Uses the AWS SDK for Java 1.12.181.
