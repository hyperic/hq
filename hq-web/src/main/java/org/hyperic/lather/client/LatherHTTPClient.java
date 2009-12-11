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

package org.hyperic.lather.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;
import org.hyperic.lather.xcode.LatherXCoder;

import org.hyperic.util.encoding.Base64;
import org.hyperic.util.security.UntrustedSSLProtocolSocketFactory;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;

/**
 * The LatherClient is the base object which is used to invoke
 * remote Lather methods.  
 */
public class LatherHTTPClient 
    implements LatherClient
{
    private static final int TIMEOUT_CONN = 10 * 1000;
    private static final int TIMEOUT_DATA = 10 * 1000;

    public static final String HDR_ERROR      = "X-error-response";
    public static final String HDR_VALUECLASS = "X-latherValue-class";

    private LatherXCoder xCoder;
    private String       baseURL;
    private int          timeoutConn, timeoutData;

    public LatherHTTPClient(String baseURL){
        this(baseURL, TIMEOUT_CONN, TIMEOUT_DATA);
    }

    public LatherHTTPClient(String baseURL, int timeoutConn, int timeoutData){
        Protocol mySSLProt;

        this.baseURL = baseURL;
        this.xCoder  = new LatherXCoder();

        mySSLProt = new Protocol("https",
                                 new UntrustedSSLProtocolSocketFactory(),
                                 443);
        Protocol.registerProtocol("https", mySSLProt);
        this.timeoutConn = timeoutConn;
        this.timeoutData = timeoutData;
    }

    public LatherValue invoke(String method, LatherValue args)
        throws IOException, LatherRemoteException
    {
        ByteArrayOutputStream bOs;
        DataOutputStream dOs;
        HttpClient client;
        PostMethod meth;
        String encodedArgs, responseBody;
        byte[] rawData;

        bOs    = new ByteArrayOutputStream();
        dOs    = new DataOutputStream(bOs);

        this.xCoder.encode(args, dOs);
        rawData     = bOs.toByteArray();
        encodedArgs = Base64.encode(rawData);

        client = new HttpClient();
        client.setConnectionTimeout(this.timeoutConn);
        client.setTimeout(this.timeoutData);
        client.setHttpConnectionFactoryTimeout(this.timeoutConn);
        configureProxy(client);

        meth   = new PostMethod(baseURL);

        meth.addParameter("method",    method);
        meth.addParameter("args",      encodedArgs);
        meth.addParameter("argsClass", args.getClass().getName());
        
        client.executeMethod(meth);

        responseBody = meth.getResponseBodyAsString();
        meth.releaseConnection();

        if(meth.getStatusCode() == HttpStatus.SC_OK){
            ByteArrayInputStream bIs;
            DataInputStream dIs;
            Header errHeader = meth.getResponseHeader(HDR_ERROR);
            Header clsHeader = meth.getResponseHeader(HDR_VALUECLASS);
            Class resClass;

            if(errHeader != null){
                throw new LatherRemoteException(responseBody);
            }
            
            if(clsHeader == null){
                throw new IOException("Server returned malformed result:  " +
                                      "did not contain a value class header");
            }

            try {
                resClass = Class.forName(clsHeader.getValue());
            } catch(ClassNotFoundException exc){
                throw new LatherRemoteException("Server returned a class '" +
                                                clsHeader.getValue() + 
                                                "' which the client did not " +
                                                "have access to");
            }

            bIs = new ByteArrayInputStream(Base64.decode(responseBody));
            dIs = new DataInputStream(bIs);

            return this.xCoder.decode(dIs, resClass);
        } else {
            throw new IOException("Connection failure: " + 
                                  meth.getStatusLine().toString());
        }
    }
    
    private void configureProxy(HttpClient client) {                
        String proxyHost = System.getProperty("lather.proxyHost");
        int proxyPort = Integer.getInteger("lather.proxyPort", new Integer(-1)).intValue();
        
        if (proxyHost != null & proxyPort != -1) {
            client.getHostConfiguration().setProxy(proxyHost, proxyPort);
            return;
        }
    }
}
