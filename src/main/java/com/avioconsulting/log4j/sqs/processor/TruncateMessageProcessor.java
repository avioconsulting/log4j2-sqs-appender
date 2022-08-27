package com.avioconsulting.log4j.sqs.processor;

import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;
import java.util.List;

/**
 * Truncates the incoming message if its length is larger than 256kb
 * @author francisco.rodriguez
 */
public class TruncateMessageProcessor implements LogEventProcessor{

	private String clientName;

	public TruncateMessageProcessor(String client) {
		clientName = client;
	}

	private static final Logger logger = LogManager.getLogger(TruncateMessageProcessor.class);

	@Override public List<SendMessageRequest> process(ProcessorAttributes processorAttributes) {
		logger.debug("Sending truncated message");
		SendMessageRequest sendMessageRequest = new SendMessageRequest();
		sendMessageRequest.setMessageBody(truncateStringByByteLength(processorAttributes.getMessage(),
				"UTF-8", processorAttributes.getMaxMessageSize()));
		sendMessageRequest.setQueueUrl(processorAttributes.getQueueUrl());
		return Arrays.asList(sendMessageRequest);
	}

	@Override public String getClientName() {
		return this.clientName;
	}

	/**
	 * Truncate a string to a specific number of bytes.
	 *
	 * @param src the string to be truncated
	 * @param encoding the character encoding to use for the truncated string
	 * @param maxsize the maximum size of the string in bytes
	 * @return the truncated string
	 */
	private static String truncateStringByByteLength(final String src, final String encoding, final int maxsize) {
		Charset cs = Charset.forName(encoding);
		CharsetEncoder coder = cs.newEncoder();
		ByteBuffer out = ByteBuffer.allocate(maxsize);
		CharBuffer in = CharBuffer.wrap(src);
		coder.encode(in, out, true);
		int pos = src.length() - in.length();
		return src.substring(0, pos);
	}
}

