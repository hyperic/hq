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
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.hq.common.SystemException;
import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;
import org.hyperic.lather.xcode.LatherXCoder;
import org.hyperic.util.encoding.Base64;
import org.hyperic.util.http.HQHttpClient;
import org.hyperic.util.http.HttpConfig;

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

    private HQHttpClient client;
    private LatherXCoder xCoder;
    private String       baseURL;
    
    public LatherHTTPClient(String baseURL) throws Exception {
        this(baseURL, TIMEOUT_CONN, TIMEOUT_DATA);
    }

    public LatherHTTPClient(String baseURL, int timeoutConn, int timeoutData) {
        this(baseURL, timeoutConn, timeoutData,new AgentKeystoreConfig().isAcceptUnverifiedCert());
    }
    
    public LatherHTTPClient(String baseURL, int timeoutConn, int timeoutData, final boolean acceptUnverifiedCertificates) {
    	try {
    		// get proxy info
    		String proxyHostname = System.getProperty("lather.proxyHost", null);
	        int proxyPort = Integer.getInteger("lather.proxyPort", new Integer(-1)).intValue();
	        
	        // setup http client config
    		HttpConfig config = new HttpConfig();
    		
    		config.setConnectionTimeout(timeoutConn);
    		config.setSocketTimeout(timeoutData);
    		config.setProxyHostname(proxyHostname);
    		config.setProxyPort(proxyPort);

	        this.client = new HQHttpClient(new AgentKeystoreConfig(), config, acceptUnverifiedCertificates);
			this.baseURL = baseURL;
	        this.xCoder  = new LatherXCoder();
    	} catch(Exception e) {
    		throw new IllegalStateException(e);
    	}
    }

    public LatherValue invoke(String method, LatherValue args) throws IOException, LatherRemoteException {
        ByteArrayOutputStream bOs = new ByteArrayOutputStream();
        DataOutputStream dOs = new DataOutputStream(bOs);
        
        this.xCoder.encode(args, dOs);
        
        byte[] rawData = bOs.toByteArray();
        String encodedArgs = Base64.encode(rawData);
        Map<String, String> postParams = new HashMap<String, String>();
        
        postParams.put("method", method);
        postParams.put("args", encodedArgs);
        postParams.put("argsClass", args.getClass().getName());
        
        HttpResponse response = client.post(baseURL, postParams);
        
        if ((response != null) && (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)) {
            ByteArrayInputStream bIs;
            DataInputStream dIs;
            Header errHeader = response.getFirstHeader(HDR_ERROR);
            Header clsHeader = response.getFirstHeader(HDR_VALUECLASS);
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);

            if (errHeader != null) {
                throw new LatherRemoteException(responseBody);
            }

            if (clsHeader == null) {
                throw new IOException("Server returned malformed result: did not contain a value class header");
            }
	
            Class<?> resClass;
	
            try {
            	resClass = Class.forName(clsHeader.getValue());
	        } catch(ClassNotFoundException exc){
	            throw new LatherRemoteException("Server returned a class '" + clsHeader.getValue() + 
	                                            "' which the client did not have access to");
	        }
	
            try {
                bIs = new ByteArrayInputStream(Base64.decode(responseBody));
            } catch (IllegalArgumentException e) {
                throw new SystemException("could not decode response from server body=" + responseBody, e);
            }
	        dIs = new DataInputStream(bIs);
	
	        return this.xCoder.decode(dIs, resClass);
	    } else {
	        throw new IOException("Connection failure: " + response.getStatusLine());
	    }
    }
}
