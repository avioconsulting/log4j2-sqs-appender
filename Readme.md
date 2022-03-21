What?
====================
This is a custom appender for log4j2. When used with Java/Mule apps, This appender pushes all the application logs to specified Amazon SQS Queue.

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

```
<dependency>
    <groupId>com.avioconsulting</groupId>
    <artifactId>log4j2-sqs-appender</artifactId>
    <version>1.0.0</version>
</dependency>
```

* Modify Application's log4j2.xml to add the below appender custom appender config.

```
<Appenders>
    <SQS name="SQS" 
         awsAccessKey="<your AWS access key>" 
         awsRegion="<your AWS region>" 
         awsSecretKey="<your AWS secret key>" 
         maxBatchOpenMs="10000" 
         maxBatchSize="5" 
         maxInflightOutboundBatches="5" 
         queueName="<your AWS SQS queue name>">
        <PatternLayout pattern="%-5p %d [%t] %c: ##MESSAGE## %m%n"/>
    </SQS>
</Appenders>
```
Add this java package in your top level log4j2 configuration element

```<Configuration packages="com.avioconsulting.log4j">```

Add this custom appender to your Root logger in log4j2.xml.
```
<Root level="INFO">
    <AppenderRef ref="SQS"/>
</Root>        
        (or)
<AsyncRoot level="INFO">
    <AppenderRef ref="SQS" />
</AsyncRoot>
```

* That's it!

When you run the project with this appender added with your AWS credentials, you should the see your app logs flowing into the specified SQS queue.

Optional Configurations
=======================
* `largeMessagesEnabled` - set this to `true` to enable large message handling. Defaults to `false`
* `largeMessageQueueName` - this is the name of an SQS FIFO queue to be used for large messages. If the attribute is not provided `.fifo` will be appended to the `queueName` attribute
* `maxMessageBytes` - this is the maximum size to allow for a single message on the SQS queue. SQS has a maximum message size of 256 KB, so the default maximum size is 250 KB to allow for space in the message to provide info needed to rebuild the split message

Example with all possible configurations:
```
<Appenders>
    <SQS name="SQS" 
         awsAccessKey="<your AWS access key>" 
         awsRegion="<your AWS region>" 
         awsSecretKey="<your AWS secret key>" 
         maxBatchOpenMs="10000" 
         maxBatchSize="5" 
         maxInflightOutboundBatches="5" 
         queueName="<your AWS SQS queue name>"
         largeMessagesEnabled="true"
         largeMessageQueueName="<your AWS SQS FIFO queue name>"
         maxMessageBytes="250">
        <PatternLayout pattern="%-5p %d [%t] %c: ##MESSAGE## %m%n"/>
    </SQS>
</Appenders>
```

When `largeMessagesEnabled` is true, messages larger than `maxMessageBytes` will be split into multiple SQS messages and placed on a FIFO queue with the same group ID so they are guaranteed to be processed in order. The SQS messages will need to be accumulated so the log message can be reconstructed.
The message format looks like this: `currentPart=1|totalParts=5|hash=f11b7d310127a11680d0eb73d3d109adfa48608efa67f5fd69876e2f60b4ec22|message="{\"timeMillis\":1647825592778,\"thread\":\"[MuleRuntime].uber.09: [sqs-test].logFilesFlow.CPU_LITE @5d97440f\",\"level\":\"INFO\",\"loggerName\":\"com.avioconsulting.api\",\"message\":{\"timestamp\":\"2022-03-21T01:19:52.776Z\",\"appName\":\"sqs-test\",\"appVersion\":\"1.0.0\",\"correlationId\":\"0295a030-a8b5-11ec-95cb-f01898a624f3\",\"payload\":\"{\\n  \\\"filePath\\\": \\\"/docs/samplePayload.json\\\"\\n}\"},\"endOfBatch\":true,\"loggerFqcn\":\"org.apache.logging.log4j.spi.AbstractLogger\",\"contextMap\":{\"correlationId\":\"0295a030-a8b5-11ec-95cb-f01898a624f3\",\"processorPath\":\"logFilesFlow/processors/0/processors/0\"},\"threadId\":58,\"threadPriority\":5,\"timestamp\":\"2022-03-20T20:19:52.778-0500\",\"deployedAppName\":\"${sys:domain}\"}"`