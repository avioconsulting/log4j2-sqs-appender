# Changelog

User-visible changes for the SQS appender. Dates are the release commits in this repository.

## 1.2.0 - 2026-11-05

Existing `log4j2.xml` configurations work without changes.

* Replaced the AWS SDK for Java 1.x with the AWS SDK for Java 2.x (SDK 2.57.8). The application now includes the SDK 2.x libraries, including Netty and Apache HttpClient 5.
* Sends messages up to 1 MiB (1,048,576 bytes). In 1.1.0, a body larger than 256 KiB failed with `Failed to schedule request` because the SDK 1.x buffered client rejected it before the send.
* The default `maxMessageBytes` is 1048576. In 1.1.0 it was 250000.
* Messages of 256 KiB or smaller can share one `SendMessageBatch` call. A larger message, up to 1 MiB, is sent on its own with `SendMessage`. The 256 KiB value is a batching cutoff, not the SQS message limit.
* Added `largeMessageMode` with `TRUNCATE` (the default), `DISCARD`, `FIFO`, `EXTENDED`, and `S3`.
* `largeMessagesEnabled="true"` from 1.1.0 still selects `FIFO` when `largeMessageMode` is not set.
* Added `s3BucketName` for `EXTENDED` and `S3`. `EXTENDED` stores the body in S3 and sends an SQS reference. `S3` stores the body in S3 and does not send an SQS message.
* Added `endpointURL` for a private or local endpoint. When it is set, S3 uploads use path-style addressing.
* `maxInflightOutboundBatches` is still accepted so old configurations load. It has no effect on the SDK 2.x client.
* AWS clients are created when the first message is sent, not when Log4j loads the configuration. Creating them during configuration can re-enter Netty while it is still starting.
* Send failures go to the Log4j status logger, at most once every 10 seconds, and never include the message body. In 1.1.0 they used the regular logger, so they could be sent back to this appender.
* A message larger than `maxMessageBytes` writes a normal log line before it is handled. `TRUNCATE` and `DISCARD` are WARN. `FIFO`, `EXTENDED`, and `S3` are INFO. The line contains the size, the limit, and the mode, not the original event.
* Added `samples/mule-sqs-appender-test`, a Mule 4.9 application that logs a small, a medium, and an oversized message.
* Added LocalStack integration tests. `mvn clean verify` starts LocalStack 4.14.0 and needs Docker.

## 1.1.0 - 2022-03-22

* Messages larger than `maxMessageBytes` are truncated to that size before they are sent.
* Added `largeMessagesEnabled`. When it is `true`, a message larger than `maxMessageBytes` is split into ordered parts on an SQS FIFO queue instead of being truncated. Each part uses this form: `currentPart=1|totalParts=5|uuid=<uuid>|message=<part>`.
* Added `largeMessageQueueName`. When it is omitted, the appender uses `queueName` with `.fifo` appended.
* The default `maxMessageBytes` is 250000, under the 256 KiB SQS limit of that time.
* Uses the AWS SDK for Java 1.12.181.
