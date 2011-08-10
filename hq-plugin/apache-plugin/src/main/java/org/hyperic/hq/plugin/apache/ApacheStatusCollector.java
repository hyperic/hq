/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.plugin.apache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.hyperic.hq.plugin.netservices.HTTPCollector;
import org.hyperic.hq.product.MetricValue;
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

    public void collect() {
        super.collect();
        //ApacheServerDetector will auto-configure for localhost:80/server-status
        //if GET /server-status returns 404, assume httpd itself is available
        MetricValue value =
            getResult().getMetricValue(ATTR_RESPONSE_CODE);
        if ((value != null) &&
            (value.getValue() == HttpURLConnection.HTTP_NOT_FOUND))
        {
            setAvailability(true);
            String path = getProperties().getProperty(PROP_PATH);
            log.warn(METHOD_GET + " " + path + ": " + getMessage());
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

    private void parse(HttpResponse response) throws IOException {
        InputStream is = response.getEntity().getContent();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
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
            } else {
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

    protected void parseResults(HttpResponse response) {
        try {
            parse(response);
        } catch (IOException e) {
            log.error("Exception parsing: " + getURL(), e);
        }
    }

    protected void netstat() {
        //noop
    }
}
