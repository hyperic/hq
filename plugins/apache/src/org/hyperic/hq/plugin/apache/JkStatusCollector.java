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
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.plugin.netservices.HTTPCollector;
import org.hyperic.hq.product.PluginException;

public class JkStatusCollector extends HTTPCollector
{
    private static final Log log =
        LogFactory.getLog(JkStatusCollector.class.getName());
    private static final String MIME_FLAG = "?mime=prop",
                                ERRORS_KEY = "Errors",
                                READS_KEY = "Reads",
                                CLIENT_ERRORS_KEY = "ClientErrors",
                                RETRIES_KEY = "Retries",
                                RECOVER_TIME_KEY = "RecoverTime",
                                MEMBER_COUNT_KEY = "MemberCount",
                                MAPCOUNT_KEY = "Maps";

    protected void init() throws PluginException
    {
        super.init();
    
        setMethod(METHOD_GET);

        String url = getURL();
        if (!url.endsWith(MIME_FLAG))
            setURL(url + MIME_FLAG);
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

    private void parse(HttpMethod method) throws IOException
    {
        Map props = getPerfProps(method);
        setValue(MAPCOUNT_KEY, getCount(props, "map_count"));
        setValue(RETRIES_KEY, getCount(props, "retries"));
        setValue(ERRORS_KEY, getCount(props, "errors"));
        setValue(READS_KEY, getCount(props, "read"));
        setValue(RETRIES_KEY, getCount(props, "retries"));
        setValue(RECOVER_TIME_KEY, getCount(props, "recover_time"));
        setValue(MEMBER_COUNT_KEY, getCount(props, "member_count"));
        setValue(CLIENT_ERRORS_KEY, getCount(props, "client_errors"));
    }

    private long getCount(Map props, String key)
    {
        long rtn = 0;
        List list = (List)props.get(key);
        for (int i=0; i<list.size(); i++)
        {
            try {
            rtn += Integer.parseInt(list.get(i).toString());

            } catch (NumberFormatException e) {
            }
        }
        return rtn;
    }

    private Map getPerfProps(HttpMethod method) throws IOException
    {
        Map rtn = new HashMap();
        InputStream is = method.getResponseBodyAsStream();
        String line;
        BufferedReader bf = new BufferedReader(new InputStreamReader(is));
        while (null != (line = bf.readLine()))
        {
            List list;
            Object[] array = line.split("\\=");
            Object[] tmp = array[0].toString().split("\\.");
            Object key = tmp[tmp.length-1];
            Object val = (array.length > 1) ? array[1] : "";
            if (null == (list = (List)rtn.get(key)))
            {
                list = new ArrayList();
                list.add(val);
                rtn.put(key, list);
                continue;
            }
            list.add(val);
        }
        return rtn;
    }
}
