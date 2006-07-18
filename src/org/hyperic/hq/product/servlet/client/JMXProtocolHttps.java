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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import java.io.InputStream;

/**
 * Talk to HQ JMXServlet over https.
 */
public class JMXProtocolHttps
    extends JMXProtocolRequest
    implements HostnameVerifier {

    public void shutdown() {
    }

    public boolean verify(String hostname, SSLSession session) {
        return true;
    }
    
    public InputStream openStream(String host, int port,
                                  String user, String pass,
                                  String path, String query)
        throws Exception 
    {
        if (query != null) {
            path=path + "?" + query;
        }

        //XXX cache

        URL url = new URL("https", host, port, path);

        HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();

        // HQ certificate signed with hyperic.net.  Install 
        // a bogus HostnameVerifier to allow identification mismatch
        // as no customers will have that hostname.
        conn.setHostnameVerifier(this);

        if (user != null) {
            String auth = user + ":" + pass;
            
            // Easiest - but may not be very portable.
            auth = new sun.misc.BASE64Encoder().encode(auth.getBytes());

            conn.setRequestProperty("Authorization", "Basic " + auth);
        }
        
        return conn.getInputStream();
    }
}
