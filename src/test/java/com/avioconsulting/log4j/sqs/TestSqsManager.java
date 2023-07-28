package com.avioconsulting.log4j.sqs;

import com.avioconsulting.log4j.sqs.processor.FifoMessageProcessor;
import com.avioconsulting.log4j.sqs.processor.TruncateMessageProcessor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestSqsManager {

    @Test
    public void truncateRegularMessageTest() {
        String testString = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
        String truncatedString = TruncateMessageProcessor.truncateStringByByteLength(testString, "UTF-8", 24);
        assertEquals("Lorem ipsum dolor sit am", truncatedString);
    }

    @Test
    public void truncate4byteMessageTest() {
        String testString = "\uD80C\uDCA9Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod ";
        String truncatedString = TruncateMessageProcessor.truncateStringByByteLength(testString, "UTF-8", 24);
        assertEquals("\uD80C\uDCA9Lorem ipsum dolor si", truncatedString);
    }

    @Test
    public void splitStringByByteLengthTest() {
        String testString = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
        String[] splitStrings = FifoMessageProcessor.splitStringByByteLength(testString, "UTF-8", 24);
        assertEquals(19,splitStrings.length);
    }

    @Test
    public void multibyteSplitStringByByteLengthTest() {
        String testString = "\uD80C\uDCA9\uD80C\uDCA9\uD80C\uDCA9\uD80C\uDCA9\uD80C\uDCA9\uD80C\uDCA9\uD80C\uDCA9\uD80C\uDCA9\uD80C\uDCA9\uD80C\uDCA9\uD80C\uDCA9\uD80C\uDCA9";
        String[] splitStrings = FifoMessageProcessor.splitStringByByteLength(testString, "UTF-8", 24);
        assertEquals(2,splitStrings.length);
    }

}