package org.hyperic.hq.plugin.apache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.netservices.HTTPCollector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.StringUtil;

public class ApacheStatusCollector extends HTTPCollector {

    private static final Log log =
        LogFactory.getLog(ApacheStatusCollector.class.getName());

    private static final String AUTO_FLAG = "?auto";

    private static final String[][] STATES = {
        { "_", "Waiting" },
        { "S", "Starting" },
        { "R", "Reading" },
        { "W", "Sending" },
        { "K", "Keepalive" },
        { "D", "DNS" },
        { "C", "Closing" },
        { "L", "Logging" },
        { "G", "Finishing" },
        { ".", "Free" },
    };

    private static final Map STATE_MAP =
        new HashMap(STATES.length);

    static {
        for (int i=0; i<STATES.length; i++) {
            STATE_MAP.put(STATES[i][0], new Integer(i));
        }
    }

    protected void init() throws PluginException {
        super.init();
    
        setMethod(METHOD_GET);
    
        String url = getURL();
        if (!url.endsWith(AUTO_FLAG)) {
            setURL(url + AUTO_FLAG);
        }
    }

    private void parseScoreboard(String val) {
        int len = val.length();
        int[] states = new int[STATES.length];
        for (int i=0; i<states.length; i++) {
            states[i] = 0;
        }
        
        for (int i=0; i<len; i++) {
            String key = val.substring(i, i+1);
            Integer ix = (Integer)STATE_MAP.get(key);
            if (ix == null) {
                continue;
            }
            states[ix.intValue()]++;
        }

        for (int i=0; i<states.length; i++) {
            String name = STATES[i][1];
            setValue(name, states[i]);
        }
    }

    private void parse(HttpMethod method) throws IOException {
        InputStream is =
            method.getResponseBodyAsStream();
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(is));
        String line;

        while ((line = reader.readLine()) != null) {
            int ix = line.indexOf(": ");
            if (ix == -1) {
                continue;
            }
            String key = line.substring(0, ix).trim();
            String val = line.substring(ix+2).trim();
            if (key.equals("Scoreboard")) {
                parseScoreboard(val);
            }
            else {
                key = StringUtil.replace(key, " ", "");
                if ((ix = val.indexOf("e-")) != -1) {
                    val = val.substring(0, ix);
                }
                setValue(key, val);
                /* {Busy,Idle}Servers in 1.3, {Busy,Idle}Workers in 2.0 */
                if (key.endsWith("Workers")) {
                    key = StringUtil.replace(key, "Workers", "Servers");
                }
                setValue(key, val);
            }
        }        
    }

    protected void parseResults(HttpMethod method) {
        try {
            parse(method);
        } catch (IOException e) {
            log.error("Exception parsing: " + getURL(), e);
        }
    }

    protected void netstat() {
        //noop
    }
}
