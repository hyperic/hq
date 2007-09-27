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

package org.hyperic.hq.product.servlet.client;

import java.net.URL;

import java.io.InputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * Talk to jmx servlet over http.
 */
public class JMXProtocolHttp extends JMXProtocolRequest {

    public void shutdown() {
    }

    protected boolean isSSL() {
        return false;
    }

    public InputStream openStream(String host, int port,
                                  String user, String pass,
                                  String path, String query)
        throws Exception 
    {
        if (query != null) {
            path=path + "?" + query;
        }

        String scheme = isSSL() ? "https" : "http";
        URL url = new URL(scheme, host, port, path);

        HttpClient client = new HttpClient();
        final int timeout = 1 * 1000; //1min
        client.setTimeout(timeout);
        client.setConnectionTimeout(timeout);

        GetMethod get = new GetMethod(url.toString());

        if (user != null) {
            UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials(user, pass);

            client.getState().setCredentials(host,
                                             host,
                                             credentials);
                    
            get.setDoAuthentication(true);
        }

        client.executeMethod(get);

        return get.getResponseBodyAsStream();
    }
}
