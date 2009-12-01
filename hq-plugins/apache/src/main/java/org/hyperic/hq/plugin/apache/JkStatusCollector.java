/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.plugin.apache;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.plugin.netservices.HTTPCollector;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.PluginException;

public class JkStatusCollector extends HTTPCollector
{
    private static final Log log =
        LogFactory.getLog(JkStatusCollector.class.getName());
    private static final String MIME_FLAG = "?mime=prop";

    static final String JK_NAME = "mod_jk";
    static final String WORKER_NAME = JK_NAME + " Worker";
    static final String[] WORKER_PROPS = {
        //type=ajp13 (cprops)
        "host", "address",
    };

    private static final Map _filter = new HashMap();
    private static final Map _state = new HashMap();

    private Set _lbs = new HashSet();
    private Set _workers = new HashSet();

    private static void initConstants()
    {
        if (_filter.size() != 0) {
            return;
        }
        String[] keys = {
           //type=lb
           "member_count", "good", "degraded", "bad", "busy",
            //type=ajp13 (metrics)
            "state", "errors", "client_errors", "transferred", "read", "busy"
        };
        for (int i=0; i<keys.length; i++) {
            _filter.put(keys[i], Boolean.TRUE);
        }
        for (int i=0; i<WORKER_PROPS.length; i++) {
            _filter.put(WORKER_PROPS[i], Boolean.TRUE);
        }

        _state.put("OK", new Double(Metric.AVAIL_UP));
        _state.put("N/A", new Double(Metric.AVAIL_PAUSED));
        _state.put("REC", new Double(Metric.AVAIL_WARN));
        _state.put("ERR", new Double(Metric.AVAIL_DOWN));
    }

    protected void init() throws PluginException
    {
        super.init();
    
        setMethod(METHOD_GET);

        String url = getURL();
        if (!url.endsWith(MIME_FLAG))
            setURL(url + MIME_FLAG);

        initConstants();
    }

    protected void parseResults(HttpMethod method)
    {
        try {
            parse(method);
        }
        catch (IOException e) {
            log.error("Exception parsing: " + getURL(), e);
        }
    }

    Set getLoadBalancers() {
        return _lbs;
    }

    Set getWorkers() {
        return _workers;
    }

    private void parse(HttpMethod method) throws IOException
    {
        _lbs.clear();
        _workers.clear();
        InputStream is = method.getResponseBodyAsStream();
        String line;
        BufferedReader bf = new BufferedReader(new InputStreamReader(is));
        final String prefix = "worker.";
        String prevKey = null;

        while (null != (line = bf.readLine())) {
            int ix = line.indexOf('=');
            if (ix == -1) {
                continue;
            }
            String worker = line.substring(0, ix);
            String val = line.substring(ix+1);
            if (!worker.startsWith(prefix)) {
                continue;
            }
            worker = worker.substring(prefix.length());
            if ((ix = worker.indexOf('.')) == -1) {
                continue;
            }
            String name = worker.substring(0, ix);
            String key = worker.substring(ix+1);

            if (key.equals("type")) {
                if (val.equals("lb")) {
                    _lbs.add(name);
                }
                else if (val.startsWith("ajp")) {
                    if ("balance_workers".equals(prevKey)) {
                        _workers.add(name);
                    }
                }
                continue;
            }
            prevKey = key;
            if (_filter.get(key) != Boolean.TRUE) {
                continue;
            }

            if (key.equals("state")) {
                Double state = (Double)_state.get(val);
                double avail;
                if (state == null) {
                    avail = Metric.AVAIL_UP;
                }
                else {
                    avail = state.doubleValue();
                }
                setValue(worker, avail);
            }
            else {
                setValue(worker, val);
            }
        }
    }
}
