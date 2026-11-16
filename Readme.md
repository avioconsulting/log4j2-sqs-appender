What?
====================
This is a custom appender for log4j2. When used with Java/Mule apps, This appender pushes all the application logs to
specified Amazon SQS Queue.

The appender uses the AWS SDK for Java 2.x and supports SQS messages up to 1 MiB (1,048,576 bytes).
Version history is in [CHANGELOG.md](CHANGELOG.md).

Why?
====================

* When using MuleSoft CloudHub, you may want to store the logs somewhere other than in CloudHub.
* Feed this Queue to any Log analyzers like Splunk, ELK etc.
* To make a backup copy for application logs.

How?
==========================

* Build this application using the following command.

```mvn clean install```

Use this dependency in your Java/Mule Applications

```xml
<dependency>
    <groupId>com.avioconsulting</groupId>
    <artifactId>log4j2-sqs-appender</artifactId>
    <version>1.2.0</version>
</dependency>
```

* Modify Application's log4j2.xml to add the below appender custom appender config.

```xml
<Appenders>
    <SQS name="SQS" 
         awsAccessKey="<aws access key>"
         awsRegion="<aws region>"
         awsSecretKey="<aws secret key>"
         maxBatchOpenMs="10000"
         maxBatchSize="1"
         maxInflightOutboundBatches="1"
         queueName="<a normal length message queue name>"
         largeMessageMode="<possible values: TRUNCATE|DISCARD|FIFO|EXTENDED|S3>"
         largeMessageQueueName="<a large length message queue name>"
         s3BucketName="<a s3 bucket name>"
         maxMessageBytes="<numeric max bytes of the message>">
         <PatternLayout pattern="<a log4j pattern ie: %-5p %d [%t] %c: ##MESSAGE## %m%n>"/>
    </SQS>
</Appenders>
```

Add this java package in your top level log4j2 configuration element

```<Configuration packages="com.avioconsulting.log4j">```

Add this custom appender to your Root logger in log4j2.xml.

```xml
<Root level="INFO">
    <AppenderRef ref="SQS"/>
</Root>      
```

or

```xml
<AsyncRoot level="INFO">
    <AppenderRef ref="SQS" />
</AsyncRoot>
```

* That's it!

When you run the project with this appender added with your AWS credentials, you should the see your app logs flowing
into the specified SQS queue.

CloudHub
--------

On CloudHub 1.0, the platform replaces the application's `log4j2.xml` with its own logging configuration unless you
turn that off. In Runtime Manager, open the application, choose Settings, and select **Disable CloudHub logs**.
CloudHub then uses the `log4j2.xml` shipped with the application, and logs from this appender can reach SQS.
Application logs no longer show on the Runtime Manager Logs page. Only system logs remain there.

This option is available on request. If it is not in the Settings page, request it from Salesforce Help.
See [Integrate with Your Logging System Using Log4j](https://docs.mulesoft.com/cloudhub/custom-log-appender).

CloudHub 2.0 does not use that checkbox. It merges the application's `log4j2.xml` with the platform appenders.

How messages are sent
=====================

1. The appender formats the log event with the configured layout.
2. If the message is `maxMessageBytes` or smaller, the appender sends it unchanged to `queueName`.
3. If the message is larger than `maxMessageBytes`, the appender handles it with `largeMessageMode`.
   When `largeMessageMode` is not set, the appender uses `TRUNCATE`.

The message limit is 1 MiB (`maxMessageBytes`, capped at 1,048,576 bytes). Messages of 256 KiB or smaller may share one
`SendMessageBatch` call (`maxBatchSize` and `maxBatchOpenMs`). A bigger message, up to 1 MiB, is sent on its own with
`SendMessage`, because the SDK batch manager still refuses a single batch entry over 256 KiB. That is a batching
cutoff, not the SQS message limit.

Send failures are reported to the Log4j status logger (the console), at most once every 10 seconds. The report never
contains the message body, and it is never sent back to the SQS appender.

If the root logger is `DEBUG`, AWS SDK and Netty debug lines also reach the SQS appender. Sending those lines creates
more debug lines. Keep them out of SQS with `additivity="false"`. This is optional. An `INFO` root logger does not
need it.

```xml
<Loggers>
    <Logger name="software.amazon.awssdk" level="INFO" additivity="false">
        <AppenderRef ref="Console"/>
    </Logger>
    <Logger name="io.netty" level="INFO" additivity="false">
        <AppenderRef ref="Console"/>
    </Logger>
</Loggers>
```

When a log event is larger than `maxMessageBytes`, the appender also writes a normal log line before handling it.
`TRUNCATE` and `DISCARD` are logged at WARN. `FIFO`, `EXTENDED`, and `S3` are logged at INFO. That line follows the
application's usual loggers, so it reaches the console, the log file, and this SQS appender. It contains the original
size, the limit, and the mode. It does not contain the original event. If that line is itself larger than
`maxMessageBytes`, it is handled by `largeMessageMode` like any other event, but it does not produce another line.

When the appender stops, it waits up to the Log4j shutdown timeout (5 seconds when none is set) for buffered and
in-flight sends. Sends still running are cancelled, and the AWS clients are then closed. Closing the clients can take
longer than the timeout. The status logger reports an incomplete stop, and messages still in flight may be lost.

Optional Configurations
=======================

* `maxMessageBytes` - this is the maximum size, in bytes, of a single message on the SQS queue. The default and the
  maximum value is 1048576 (1 MiB), which is the SQS limit. A larger value is reduced to 1048576.
* `largeMessageQueueName` - this is the name of an SQS FIFO queue to be used for large messages in `FIFO` mode.
  The name must end with `.fifo`. The default is `queueName` followed by `.fifo`.
* `queueName` - this is the name of an SQS normal queue to be used for messages which `length <= maxMessageBytes`.
  Discarded & truncated message will be included as well.
* `largeMessageMode` - select one of the provided options `(TRUNCATE|DISCARD|FIFO|EXTENDED|S3)` to process an event message larger than `maxMessageBytes`
  before sent it to target.
    * `TRUNCATE` - message will be truncated to `maxMessageBytes` and sent to `queueName`. This is the default.
    * `DISCARD` - replaces the original content with
      `Can't display log message. Message max size exceeded. max size:` followed immediately by the `maxMessageBytes` value.
    * `FIFO` - this option will split the messages into multiple SQS messages and placed on a FIFO queue with the same
      group ID value (the uuid) so they are guaranteed to be processed in order. Each part is sent only after SQS
      accepts the previous part, so the logging thread waits for one SQS round trip per part. Each part, including
      its header, fits in `maxMessageBytes`. `maxMessageBytes` must be at least 132 in this mode. The SQS messages will need to be
      accumulated so the log message can be reconstructed.
      The message format looks like
      this: `currentPart=1|totalParts=5|uuid=8744fe2c-cbea-4e49-8b81-0d7076899a48|message=[INFO] - Hello World!` 
    * `EXTENDED` - In this case the message will be queued into SQS `queueName` queue with a reference id to `s3BucketName`
      bucket file, which at the end, will contain the actual message. The appender uses the
      [Amazon SQS Extended Client Library](https://github.com/awslabs/amazon-sqs-java-extended-client-lib).
      With the default `maxMessageBytes`, only messages larger than 1 MiB are stored in S3. `s3BucketName` is required.
    * `S3` - Finally, if this is chosen, the message will be stored directly as a file into `s3BucketName`. No SQS
      message is send. `s3BucketName` is required.
* `s3BucketName` - the S3 bucket used by the `EXTENDED` and `S3` modes.
* `maxBatchSize` - the maximum number of messages in one SQS batch request. Valid values are 1 to 10. The default is 10.
  A value of 1 turns off batching.
* `maxBatchOpenMs` - how long, in milliseconds, the appender waits to fill a batch before it sends it. The default is
  200. A value of 0 turns off batching.
* `maxInflightOutboundBatches` - this attribute is accepted so existing configurations keep working, but it has no
  effect. The AWS SDK for Java 2.x does not have this setting. Outstanding sends are limited by the SDK HTTP client
  instead: 50 connections, and up to 10,000 requests waiting for a connection for at most 10 seconds. Requests over
  those limits fail and are reported to the Log4j status logger. A normal SQS send does not wait. `FIFO` parts,
  `EXTENDED` sends, and `S3` uploads do. Each waits until AWS accepts that one call.
* `endpointURL` - allows using VPC private network for sending messages. It's not required. When it is set, S3 uploads use path-style addressing so the same URL can serve SQS and S3.
* `largeMessagesEnabled` - version 1.1.0 attribute. If it is `true` and `largeMessageMode` is not set, the appender
  uses `FIFO` mode. New configurations should use `largeMessageMode`.

Example with all possible configurations:

```xml
<Appenders>
    <SQS name="SQS" 
         awsAccessKey="YOURACCESSKEY"
         awsRegion="YOURAWSREGION"
         awsSecretKey="YOURSECRETKEY"
         maxBatchOpenMs="10000"
         maxBatchSize="1"
         maxInflightOutboundBatches="1"
         largeMessageMode="EXTENDED"
         queueName="stg-normal-messages-queue"
         largeMessageQueueName="stg-large-messages-queue.fifo"
         s3BucketName="stg-sqs-messages-bucket"
         maxMessageBytes="1048576"
         endpointURL="https://sqs.us-east-2.amazonaws.com">
         <PatternLayout pattern="%-5p %d [%t] %c: ##MESSAGE## %m%n"/>
    </SQS>
</Appenders>
```

Queues created before August 2025
=================================

On August 4, 2025, AWS increased the SQS maximum message size from 256 KiB to 1 MiB. New queues use 1 MiB by default.
A queue created before that date can still have its `MaximumMessageSize` attribute set to 262144 (256 KiB). SQS
rejects larger messages sent to that queue.

1. Check the queue setting:

   ```
   aws sqs get-queue-attributes --queue-url <queue url> --attribute-names MaximumMessageSize
   ```

2. If the value is smaller than 1048576, update it:

   ```
   aws sqs set-queue-attributes --queue-url <queue url> --attributes MaximumMessageSize=1048576
   ```

3. Do the same for the dead-letter queue, if the queue has one.

If you cannot change the queue, set `maxMessageBytes` to the queue's `MaximumMessageSize` value.

Run Automated tests
==========================
* Run automated test of this component using the following command.
```
mvn clean verify
```
As it runs localstack using a docker image, you need to have docker installed in your local machine.

Mule sample
===========

`samples/mule-sqs-appender-test` is a small Mule 4.9 application that logs three messages through this appender:

* about 50 KB, under the 256 KiB batch limit
* about 500 KB, over 256 KiB and under 1 MiB, sent as one `SendMessage` call
* about 1.1 MB, over `maxMessageBytes`

Each message ends with `###SQS-APPENDER-TEST-END###`. If that marker is present in the stored event, the full message was sent. The sample sets `largeMessageMode="TRUNCATE"`, so the 1.1 MB event is cut to `maxMessageBytes` and the marker is not present. Change `largeMessageMode` in `samples/mule-sqs-appender-test/src/main/resources/log4j2.xml` to try `DISCARD`, `FIFO`, `EXTENDED`, or `S3`.

The sample is not part of this repository's Maven build. Open it in Anypoint Studio. See `samples/mule-sqs-appender-test/README.md`.

