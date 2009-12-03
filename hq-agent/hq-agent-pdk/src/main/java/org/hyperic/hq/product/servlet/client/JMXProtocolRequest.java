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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.hyperic.hq.product.MetricInvalidException;

/**
 * Interface for use by TomcatMeasurementPlugin to abstract transport.
 */
public abstract class JMXProtocolRequest {

    public abstract InputStream openStream(String host, int port, String user,
                                              String pass,
                                              String path, String query) throws Exception;

    public abstract void shutdown();
    
    public Object parseResponse(InputStream is, String qry )
        throws Exception
    {
        BufferedReader in=new BufferedReader(new InputStreamReader(is));
        String value = null;
        try {
            //just like the BEEP protocol, but different.
            String line = in.readLine();

            if (line.startsWith("value=")) {
                line = line.substring(6);
                if (line.indexOf(';') < 0) {
                    throw new Exception("value type not specified");
                }

                value = line.substring(2);

                switch (line.charAt(0)) {
                  case 'L':
                    return Long.valueOf(value);
                  case 'I':
                    return Integer.valueOf(value);
                  case 'D':
                    return Double.valueOf(value);
                  case 'S':
                    return value;
                  case 'N':
                    return null;
                  default:
                    throw new Exception("invalid value type");
                }
            }
            else if (line.startsWith("exception=")) {
                MetricInvalidException ex=new MetricInvalidException("Exception result on DSN=" + qry +
                        " Exception=" + line.substring(10));
                ex.setRemoteMessage(line.substring(10));
                throw ex;
            }
            else {
                throw new Exception("Unknown response " + line  +
                        " DSN=" + qry);
            }
        } finally {
            try {
                in.close();
            } catch (Exception ioe) {
                // ignore problems on closing the stream.
            }
        }
    }
    
}
