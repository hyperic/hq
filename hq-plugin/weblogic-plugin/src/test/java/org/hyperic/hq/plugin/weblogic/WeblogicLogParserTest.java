package org.hyperic.hq.plugin.weblogic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hyperic.hq.plugin.weblogic.WeblogicLogParser.Entry;
import org.junit.Test;

public class WeblogicLogParserTest {
    
    private static final List<String> MESSAGE_LIST = new ArrayList<String>();
    static {
        MESSAGE_LIST.add("1304459985815> <BEA-310002> <64% of the total memory in the server is free");
        MESSAGE_LIST.add("1304460105826> <BEA-310002> <85% of the total memory in the server is free");
        MESSAGE_LIST.add("1304463825888> <BEA-310002> <74% of the total memory in the server is free");
        MESSAGE_LIST.add("1304467605990> <BEA-310002> <85% of the total memory in the server is free");
        MESSAGE_LIST.add("1291875019751> <BEA-310003> <Free memory in the server is 3,704,096 bytes. There is danger of OutOfMemoryError");
        MESSAGE_LIST.add("1291875019751> <BEA-310003> <Free memory in the server is 3,704,096 bytes. There is danger of OutOfMemoryError");
        MESSAGE_LIST.add("1291875019751> <BEA-310003> <Free memory in the server is 3,704,096 bytes. There is danger of OutOfMemoryError");
        MESSAGE_LIST.add("1291875019751> <BEA-310003> <Free memory in the server is 3,704,096 bytes. There is danger of OutOfMemoryError");
        MESSAGE_LIST.add("1291875019751> <BEA-310003> <Free memory in the server is 3,704,096 bytes. There is danger of OutOfMemoryError");
        MESSAGE_LIST.add("1291875019751> <BEA-310003> <Free memory in the server is 3,704,096 bytes. There is danger of OutOfMemoryError");
        MESSAGE_LIST.add("1291875019751> <BEA-310003> <Free memory in the server is 3,704,096 bytes. There is danger of OutOfMemoryError");
        MESSAGE_LIST.add("1291875019751> <BEA-310003> <Free memory in the server is 3,704,096 bytes. There is danger of OutOfMemoryError");
    }

    private boolean isInAcceptableTimeRange(long eventTimeFromLog){
        long now = System.currentTimeMillis();
        // giving the event 10 sec buffer each side.
        if (eventTimeFromLog < now + 10000 && eventTimeFromLog > now - 10000){
            return true;
        }
        return false;
    }
    
    @Test
    public void testParseMessage() throws IOException {
        WeblogicLogParser parser = new WeblogicLogParser();
        InputStream fileStream = WeblogicLogParserTest.class.getResourceAsStream("/sample.log");
        InputStream inStream = new BufferedInputStream(fileStream);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(inStream));
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                try {
                    Entry entry = parser.parse(line);
                    if (entry == null) {
                        continue;
                    }
                    assertEquals("Messages should be equal", MESSAGE_LIST.get(count), entry.message);
                    assertTrue("Event date should match current time millis range: " + entry.time, isInAcceptableTimeRange(entry.time));
                    count++;
                } catch (Exception e) {
                    break;
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
    
    @Test
    public void testParseMessageWithDifferentDateFormat() throws IOException {
        WeblogicLogParser parser = new WeblogicLogParser();
        parser.setDateTimeFormat(new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a z", Locale.getDefault()));
        InputStream fileStream = WeblogicLogParserTest.class.getResourceAsStream("/sample.log");
        InputStream inStream = new BufferedInputStream(fileStream);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(inStream));
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                try {
                    Entry entry = parser.parse(line);
                    if (entry == null) {
                        continue;
                    }
                    assertEquals("Messages should be equal", MESSAGE_LIST.get(count),entry.message);
                    assertFalse("The event time should not be in the current time range: " + entry.time, isInAcceptableTimeRange(entry.time));
                    count++;
                } catch (Exception e) {
                    break;
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
}
