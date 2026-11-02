package com.avioconsulting.log4j.sqs;

import com.avioconsulting.log4j.sqs.client.ConnectorClient;
import com.avioconsulting.log4j.sqs.client.ConnectorClientFactory;
import com.avioconsulting.log4j.sqs.processor.ProcessorType;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Plugin(name = "SQS", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
class SqsAppender extends AbstractAppender {

    private final SqsManager manager;

    public SqsAppender(final String name, final Layout<? extends Serializable> layout, final Filter filter,
                       final boolean ignoreExceptions, final SqsManager manager) {
        super(name, filter, layout, ignoreExceptions);
        this.manager = Objects.requireNonNull(manager, "manager");
    }

    /**
     * @return a builder for a HttpAppender.
     */
    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    public void append(LogEvent event) {
        try {
            manager.send(getLayout(), event);
        } catch (final Exception e) {
            error("Unable to sent SQS message in appender [" + getName() + "]", event, e);
        }
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        boolean stopped = super.stop(timeout, timeUnit, false);
        stopped &= manager.stop(timeout, timeUnit);
        setStopped();
        return stopped;
    }

    SqsManager getManager() {
        return manager;
    }

    @Override
    public String toString() {
        return "SqsAppender{" +
                "name=" + getName() +
                ", state=" + getState() +
                '}';
    }

    public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<SqsAppender> {

        private static final Logger logger = StatusLogger.getLogger();
        @PluginBuilderAttribute
        @Required(message = "No AWS Access Key provided for SQSAppender")
        private String awsAccessKey;
        @PluginBuilderAttribute
        @Required(message = "No AWS Secret Key provided for SQSAppender")
        private String awsSecretKey;
        @PluginBuilderAttribute
        @Required(message = "No AWS Region provided for SQSAppender")
        private String awsRegion;
        @PluginBuilderAttribute
        @Required(message = "No SQS Queue provided for SQSAppender")
        private String queueName;
        @PluginBuilderAttribute
        private String largeMessageQueueName;
        @PluginBuilderAttribute
        private Integer maxBatchOpenMs;
        @PluginBuilderAttribute
        private Integer maxBatchSize;
        @PluginBuilderAttribute
        private Integer maxInflightOutboundBatches;
        @PluginBuilderAttribute
        private Integer maxMessageBytes;
        @PluginBuilderAttribute
        private String largeMessageMode;
        @PluginBuilderAttribute
        private String s3BucketName;
        @PluginBuilderAttribute
        private String endpointURL;
        /**
         * Version 1.1.0 attribute. When true and largeMessageMode is not set, large messages use FIFO mode.
         */
        @PluginBuilderAttribute
        private Boolean largeMessagesEnabled;


        @Override
        public SqsAppender build() {
            logger.debug("Initializing SQS appender");

            if (isBlank(largeMessageMode) && Boolean.TRUE.equals(largeMessagesEnabled)) {
                largeMessageMode = ProcessorType.FIFO.name();
            }

            if(ProcessorType.S3.name().equals(largeMessageMode) && isBlank(s3BucketName)){
                throw new RuntimeException("No s3BucketName provided for S3 largeMessageMode");
            }

            if(ProcessorType.EXTENDED.name().equals(largeMessageMode) && isBlank(s3BucketName)){
                throw new RuntimeException("No s3BucketName provided for EXTENDED largeMessageMode");
            }

            if(ProcessorType.FIFO.name().equals(largeMessageMode) && !isBlank(largeMessageQueueName) && !largeMessageQueueName.endsWith(".fifo")){
                throw new RuntimeException("largeMessageQueueName must end with .fifo for FIFO largeMessageMode");
            }

            ConnectorClient connectorClient = ConnectorClientFactory.createConnectorClient(awsAccessKey,
                    awsSecretKey,
                    awsRegion,
                    maxBatchOpenMs,
                    maxBatchSize,
                    maxInflightOutboundBatches,
                    s3BucketName,
                    endpointURL,
                    largeMessageMode);
            final SqsManager manager = new SqsManager(getConfiguration(), getConfiguration().getLoggerContext(),
                    getName(), queueName, largeMessageQueueName, maxMessageBytes, largeMessageMode, s3BucketName,
                    connectorClient);
            return new SqsAppender(getName(), getLayout(), getFilter(), isIgnoreExceptions(), manager);
        }

        private static boolean isBlank(String value) {
            return value == null || value.trim().isEmpty();
        }

    }
}