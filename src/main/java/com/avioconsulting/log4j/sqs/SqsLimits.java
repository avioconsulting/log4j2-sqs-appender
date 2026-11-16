package com.avioconsulting.log4j.sqs;

/**
 * Amazon SQS service limits used by the appender.
 */
public final class SqsLimits {

    /**
     * Largest message body SQS accepts (1 MiB). Queues created before August 2025 may still
     * have their MaximumMessageSize attribute set to 262144 and must be updated to use it.
     */
    public static final int MAX_MESSAGE_BYTES = 1_048_576;

    /**
     * Largest message the SDK v2 SqsAsyncBatchManager accepts without sending it on its own.
     * Bigger messages are sent with a direct SendMessage call.
     */
    public static final int MAX_BATCHED_MESSAGE_BYTES = 262_144;

    /**
     * Largest number of entries in one SendMessageBatch call.
     */
    public static final int MAX_BATCH_SIZE = 10;

    private SqsLimits() {
    }
}
