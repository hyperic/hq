package org.hyperic.hq.agent;

import static org.junit.Assert.assertEquals;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Test;


/**
 * Test the AgentRemoteValue class.
 * @author jasonkonicki
 *
 */
public class AgentRemoteValueTest {

    /**
     * Tests the chunking and unchunking of the large values in the AgentRemoteValue class.
     * @throws IOException
     */
    @Test
    public void testHandlingOfLargeValues() throws IOException {
        StringBuilder builder = new StringBuilder();
        for(int i=0; i<60000; i++){
            builder.append(i+",");
        }
        String[][] keyvals = new String[3][2];
        keyvals[0][0] = "KEY22";
        keyvals[0][1] = "VALUE22";
        keyvals[1][0] = "LongKey1";
        keyvals[1][1] = builder.toString();
        keyvals[2][0] = "LongKey2";
        builder = new StringBuilder();
        for(int i=0; i<20000; i++){
            builder.append(i+"-");
        }
        keyvals[2][1] = builder.toString();
        AgentRemoteValue remoteValue = new AgentRemoteValue(keyvals);
        FileOutputStream fos = new FileOutputStream(System.getProperty("java.io.tmpdir") + "/t.tmp");
        DataOutputStream os = new DataOutputStream(fos);
        remoteValue.toStream(os);
        os.close();
        fos.close();
        
        FileInputStream fis = new FileInputStream(System.getProperty("java.io.tmpdir") + "/t.tmp");
        DataInputStream is =new DataInputStream(fis);
        AgentRemoteValue afterRemoteValue = AgentRemoteValue.fromStream(is);
        fis.close();
        is.close();
        assertEquals(keyvals[0][1], afterRemoteValue.getValue(keyvals[0][0]));
        assertEquals(keyvals[1][1], afterRemoteValue.getValue(keyvals[1][0]));
        assertEquals(keyvals[2][1], afterRemoteValue.getValue(keyvals[2][0]));
        new File(System.getProperty("java.io.tmpdir") + "/t.tmp").delete();
    }
}
