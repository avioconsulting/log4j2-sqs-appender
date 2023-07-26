package com.avioconsulting.log4j.sqs;

import org.junit.Test;

public class TestSqsManager {

    @Test
    public void truncateRegularMessageTest() {
        String testString = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
        //String truncatedString = SqsManager.truncateStringByByteLength(testString, "UTF-8", 24);
        //Assert.assertEquals("Lorem ipsum dolor sit am", truncatedString);
    }

    @Test
    public void truncate4byteMessageTest() {
        String testString = "\uD80C\uDCA9Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod ";
        //String truncatedString = SqsManager.truncateStringByByteLength(testString, "UTF-8", 24);
        //Assert.assertEquals("\uD80C\uDCA9Lorem ipsum dolor si", truncatedString);
    }

    @Test
    public void splitStringByByteLengthTest() {
        String testString = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
        //String[] splitStrings = SqsManager.splitStringByByteLength(testString, "UTF-8", 24);
        //Assert.assertEquals(splitStrings.length, 19);
    }

    @Test
    public void multibyteSplitStringByByteLengthTest() {
        String testString = "\uD80C\uDCA9\uD80C\uDCA9\uD80C\uDCA9\uD80C\uDCA9\uD80C\uDCA9\uD80C\uDCA9\uD80C\uDCA9\uD80C\uDCA9\uD80C\uDCA9\uD80C\uDCA9\uD80C\uDCA9\uD80C\uDCA9";
        // String[] splitStrings = SqsManager.splitStringByByteLength(testString, "UTF-8", 24);
        //Assert.assertEquals(splitStrings.length, 2);
    }

}