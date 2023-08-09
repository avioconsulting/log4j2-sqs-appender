What?
====================
This is a custom appender for log4j2. When used with Java/Mule apps, This appender pushes all the application logs to
specified Amazon SQS Queue.

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
    <version>1.1.2</version>
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

Optional Configurations
=======================

* `maxMessageBytes` - this is the maximum size to allow for a single message on the SQS queue. SQS has a maximum message
  size of 256 KB, so the default maximum size is 250 KB to allow for space in the message to provide info needed to
  rebuild the split message
* `largeMessageQueueName` - this is the name of an SQS FIFO queue to be used for large messages.
* `queueName` - this is the name of an SQS normal queue to be used for messages which `length < maxMessageBytes`.
  Discarded & truncated message will be included as well.
* `largeMessageMode` - select one of the provided options `(TRUNCATE|DISCARD|FIFO|EXTENDED|S3)` to process an event message larger than `maxMessageBytes`
  before sent it to target.
    * `TRUNCATE` - message will be truncated to `maxMessageBytes`.
    * `DISCARD` - It will modify message by changing original content
      to `Can't display log message. Message max size exceeded. max size: maxMessageBytes`.
    * `FIFO` - this option will split the messages into multiple SQS messages and placed on a FIFO queue with the same
      group ID value (the uuid) so they are guaranteed to be processed in order. The SQS messages will need to be
      accumulated so the log message can be reconstructed.
      The message format looks like
      this: `currentPart=1|totalParts=5|uuid=8744fe2c-cbea-4e49-8b81-0d7076899a48|message="[INFO] - Hello World! "` 
    * `EXTENDED` - In this case the message will be queued into SQS `queueName` queue with a reference id to `s3BucketName`
      bucket file, which at the end, will contain the actual message.
    * `S3` - Finally, if this is chosen, the message will be stored directly as a file into `s3BucketName`. No SQS
      message is send.
    * `endpointURL` - allows using VPC private network for sending messages. It's not required.

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
         largeMessageQueueName="stg-large-messages-queue"
         s3BucketName="stg-sqs-messages-bucket"
         maxMessageBytes="256"
         endpointURL="https://sqs.us-east-2.amazonaws.com">
         <PatternLayout pattern="%-5p %d [%t] %c: ##MESSAGE## %m%n"/>
    </SQS>
</Appenders>
```
Run Automated tests
==========================
* Run automated test of this component using the following command.
```
mvn clean verify -Plocalstack-test
```
As it runs localstack using a docker image, you need to have docker installed in your local machine.


