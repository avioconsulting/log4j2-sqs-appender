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
        <PatternLayout pattern="%-5p %d [%t] %c: %m%n"/>
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