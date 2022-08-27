package com.avioconsulting.log4j.sqs;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.avioconsulting.log4j.sqs.client.AVIOAWSClientSupplier;
import com.avioconsulting.log4j.sqs.client.AvioAWSClientAttributes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

@Plugin(name = "SQS", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
class SqsAppender extends AbstractAppender {

	public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B>
			implements org.apache.logging.log4j.core.util.Builder<SqsAppender> {

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

		private static final Logger logger = LogManager.getLogger();


		@Override
		public SqsAppender build() {
			logger.debug("Initializing SQS appender");
			AvioAWSClientAttributes attributes = new AvioAWSClientAttributes(awsRegion,awsAccessKey,awsSecretKey,
					maxBatchOpenMs,maxBatchSize,maxInflightOutboundBatches, s3BucketName);
			final AVIOAWSClientSupplier avioawsClientSupplier = new AVIOAWSClientSupplier(attributes);
			final SqsManager manager = new SqsManager(getConfiguration(), getConfiguration().getLoggerContext(),
					getName(), queueName, largeMessageQueueName, maxMessageBytes, largeMessageMode, avioawsClientSupplier);

			return new SqsAppender(getName(), getLayout(), getFilter(), isIgnoreExceptions(), manager);
		}

		public B setAwsAccessKey(final String awsAccessKey) {
			this.awsAccessKey = awsAccessKey;
			return asBuilder();
		}

		public B setAwsSecretKey(final String awsSecretKey) {
			this.awsSecretKey = awsSecretKey;
			return asBuilder();
		}

		public B setAwsRegion(final String awsRegion) {
			this.awsRegion = awsRegion;
			return asBuilder();
		}

		public B setQueueName(final String queueName) {
			this.queueName = queueName;
			return asBuilder();
		}

		public B setLargeMessageQueueName(final String largeMessageQueueName) {
			this.largeMessageQueueName = largeMessageQueueName;
			return asBuilder();
		}

		public B setMaxBatchOpenMs(final Integer maxBatchOpenMs) {
			this.maxBatchOpenMs = maxBatchOpenMs;
			return asBuilder();
		}

		public B setMaxBatchSize(final Integer maxBatchSize) {
			this.maxBatchSize = maxBatchSize;
			return asBuilder();
		}

		public B setMaxInflightOutboundBatches(final Integer maxInflightOutboundBatches) {
			this.maxInflightOutboundBatches = maxInflightOutboundBatches;
			return asBuilder();
		}

		public B setMaxMessageBytes(final Integer maxMessageBytes) {
			this.maxMessageBytes = maxMessageBytes;
			return asBuilder();
		}

		public B setLargeMessageMode(final String largeMessageMode) {
			this.largeMessageMode = largeMessageMode;
			return asBuilder();
		}

	}

	/**
	 * @return a builder for a HttpAppender.
	 */
	@PluginBuilderFactory
	public static <B extends Builder<B>> B newBuilder() {
		return new Builder<B>().asBuilder();
	}

	private final SqsManager manager;


	public SqsAppender(final String name, final Layout<? extends Serializable> layout, final Filter filter,
			final boolean ignoreExceptions, final SqsManager manager) {
		super(name, filter, layout, ignoreExceptions);
		this.manager = Objects.requireNonNull(manager, "manager");
	}

	@Override
	public void start() {
		super.start();
		manager.startup();
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

	@Override
	public String toString() {
		return "SqsAppender{" +
				"name=" + getName() +
				", state=" + getState() +
				'}';
	}
}