package org.hyperic.hq.plugin.weblogic;

import static org.junit.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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
                    assertEquals(MESSAGE_LIST.get(count),entry.message);
                    count++;
                } catch (Exception e) {
                    System.out.println("ERROR-->" + e.getMessage());
                    e.printStackTrace();
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
