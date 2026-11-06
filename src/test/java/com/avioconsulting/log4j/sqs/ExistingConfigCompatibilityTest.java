package com.avioconsulting.log4j.sqs;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Checks that SQS appender configurations written for version 1.x still load without changes.
 */
public class ExistingConfigCompatibilityTest {

    private static final String MULE_APP_CONFIG = "<Configuration packages=\"com.avioconsulting.log4j.sqs\">" +
            "<Appenders>" +
            "<SQS name=\"SQS\" queueName=\"mule-log-${sys:mule.env}\" awsRegion=\"us-east-1\"" +
            " awsAccessKey=\"${sys:amazon.sqs.access.key}\" awsSecretKey=\"${sys:amazon.sqs.secret.key}\"" +
            " maxMessageBytes=\"1000000\" maxBatchSize=\"10\" maxBatchOpenMs=\"500\" maxInflightOutboundBatches=\"5\">" +
            "<JSONLayout compact=\"true\" eventEol=\"true\" properties=\"true\" propertiesAsList=\"true\"" +
            " stacktraceAsString=\"true\" includeTimeMillis=\"true\"/>" +
            "</SQS>" +
            "</Appenders>" +
            "<Loggers><Root level=\"INFO\"><AppenderRef ref=\"SQS\"/></Root></Loggers>" +
            "</Configuration>";

    private static final String V110_LARGE_MESSAGES_CONFIG = "<Configuration packages=\"com.avioconsulting.log4j.sqs\">" +
            "<Appenders>" +
            "<SQS name=\"SQS\" queueName=\"mule-log\" awsRegion=\"us-east-1\" awsAccessKey=\"key\" awsSecretKey=\"secret\"" +
            " largeMessagesEnabled=\"true\" maxMessageBytes=\"1000\">" +
            "<PatternLayout pattern=\"%m\"/>" +
            "</SQS>" +
            "</Appenders>" +
            "<Loggers><Root level=\"INFO\"><AppenderRef ref=\"SQS\"/></Root></Loggers>" +
            "</Configuration>";

    private XmlConfiguration configuration;

    @Before
    public void setUp() {
        System.setProperty("mule.env", "test");
        System.setProperty("amazon.sqs.access.key", "test-access-key");
        System.setProperty("amazon.sqs.secret.key", "test-secret-key");
    }

    @After
    public void tearDown() {
        if (configuration != null) {
            configuration.stop();
        }
        System.clearProperty("mule.env");
        System.clearProperty("amazon.sqs.access.key");
        System.clearProperty("amazon.sqs.secret.key");
    }

    @Test
    public void muleAppConfigLoadsUnchanged() throws IOException {
        SqsAppender appender = load(MULE_APP_CONFIG);

        assertTrue(appender.isStarted());
        assertEquals(1_000_000, appender.getManager().getMaxMessageBytes());
    }

    @Test
    public void version110LargeMessagesEnabledStillLoads() throws IOException {
        SqsAppender appender = load(V110_LARGE_MESSAGES_CONFIG);

        assertTrue(appender.isStarted());
    }

    private SqsAppender load(String xml) throws IOException {
        ConfigurationSource source = new ConfigurationSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        configuration = new XmlConfiguration(new LoggerContext("compatibility-test"), source);
        configuration.initialize();
        configuration.start();
        Appender appender = configuration.getAppender("SQS");
        assertNotNull("SQS appender was not created", appender);
        return (SqsAppender) appender;
    }
}
