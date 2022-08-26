package com.avioconsulting.log4j.sqs.processor;

import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 */
public class FifoMessageProcessor implements LogEventProcessor{

	private static final Logger logger = LogManager.getLogger(FifoMessageProcessor.class);
	@Override public List<SendMessageRequest> process(String message, Integer messageSize, String queueUrl) {
		List<SendMessageRequest> sendMessageRequestList = new ArrayList<>();
		//logger.debug("Fifo - Splitting large message");
		// Generate Message Hash to use as the group ID
		UUID uuid = UUID.randomUUID();
		//logger.debug("Large Message UUID: " + uuid);
		String[] splitMessage = splitStringByByteLength(message, "UTF-8", messageSize);

		for (int i = 0; i < splitMessage.length; i++) {
			//logger.debug(String.format("Sending message %d of %d", i + 1, splitMessage.length));
			SendMessageRequest sendMessageRequest = new SendMessageRequest();
			sendMessageRequest.setMessageBody(String.format("currentPart=%d|totalParts=%d|uuid=%s|message=%s", i + 1, splitMessage.length, uuid, splitMessage[i]));
			sendMessageRequest.setMessageGroupId(uuid.toString());
			sendMessageRequest.setQueueUrl(queueUrl);
			sendMessageRequestList.add(sendMessageRequest);
		}
		return sendMessageRequestList;
	}

	/**
	 * Split the input string into a list of strings, each with a maximum memory size of maxsize bytes.
	 *
	 * @param src the source string
	 * @param encoding the encoding to use for the output string
	 * @param maxsize the maximum size of the string parts in bytes
	 * @return an array of strings, each with a maximum length of maxsize bytes
	 */
	protected static String[] splitStringByByteLength(final String src, final String encoding, final int maxsize) {
		Charset cs = Charset.forName(encoding);
		CharsetEncoder coder = cs.newEncoder();
		ByteBuffer out = ByteBuffer.allocate(maxsize);  // output buffer of required size
		CharBuffer in = CharBuffer.wrap(src);
		List<String> ss = new ArrayList<>();            // a list to store the chunks
		int pos = 0;
		while (true) {
			CoderResult result = coder.encode(in, out, true); // try to encode as much as possible
			int newpos = src.length() - in.length();
			String s = src.substring(pos, newpos);
			ss.add(s);                                  // add what has been encoded to the list
			pos = newpos;                               // store new input position
			out.rewind();                               // and rewind output buffer
			if (!result.isOverflow()) {
				break;                                  // everything has been encoded
			}
		}
		return ss.toArray(new String[0]);
	}
}
