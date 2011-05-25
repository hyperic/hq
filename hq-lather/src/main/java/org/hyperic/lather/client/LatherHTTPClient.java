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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;

import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;
import org.hyperic.lather.xcode.LatherXCoder;

import org.hyperic.util.encoding.Base64;
import org.hyperic.util.security.ConfigurableX509TrustManager;
import org.hyperic.util.security.UntrustedSSLCertificateException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;

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

    private ConfigurableX509TrustManager trustManager;
    private HttpClient client;
    private LatherXCoder xCoder;
    private String       baseURL;
    
    public LatherHTTPClient(String baseURL) throws Exception {
        this(baseURL, TIMEOUT_CONN, TIMEOUT_DATA);
    }

    public LatherHTTPClient(String baseURL, int timeoutConn, int timeoutData) throws Exception {
    	KeyStore trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());
        FileInputStream instream = new FileInputStream(new File("/Users/david/.keystore"));
         
        try {
            trustStore.load(instream, "hyperic!".toCharArray());
        } finally {
        	try { instream.close(); } catch (Exception ignore) {}
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        
        keyManagerFactory.init(trustStore, "hyperic!".toCharArray());
        trustManager = new ConfigurableX509TrustManager("NORMAL", trustStore);
        
        SSLContext sslContext = SSLContext.getInstance("TLS");
        
        sslContext.init(keyManagerFactory.getKeyManagers(), new TrustManager[] { trustManager }, new SecureRandom());

        // XXX Should we use ALLOW_ALL_HOSTNAME_VERIFIER (least restrictive) or 
        //     BROWSER_COMPATIBLE_HOSTNAME_VERIFIER (moderate restrictive) or
        //     STRICT_HOSTNAME_VERIFIER (most restrictive)???
        SSLSocketFactory socketFactory = new SSLSocketFactory(sslContext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        Scheme sslScheme = new Scheme("https", 443, socketFactory);
		
		this.client = new DefaultHttpClient();
		this.baseURL = baseURL;
        this.xCoder  = new LatherXCoder();
        
        client.getConnectionManager().getSchemeRegistry().register(sslScheme);
        client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, timeoutData);
        client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeoutConn);
        
        configureProxy(client);
    }

    public LatherValue invoke(String method, LatherValue args)
        throws IOException, LatherRemoteException
    {
        ByteArrayOutputStream bOs = new ByteArrayOutputStream();
        DataOutputStream dOs = new DataOutputStream(bOs);
        
        this.xCoder.encode(args, dOs);
        
        byte[] rawData = bOs.toByteArray();
        String encodedArgs = Base64.encode(rawData);
        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        
        postParams.add(new BasicNameValuePair("method", method));
        postParams.add(new BasicNameValuePair("args", encodedArgs));
        postParams.add(new BasicNameValuePair("argsClass", args.getClass().getName()));
        
        HttpPost post = new HttpPost(baseURL);
        
        post.setEntity(new UrlEncodedFormEntity(postParams, "UTF-8"));
        
        HttpResponse response = null;

        try {
        	response = client.execute(post);
        } catch(SSLException e) {
        	throw new UntrustedSSLCertificateException(e, trustManager.getChain());
        }
        
        if (response != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
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
	
            Class resClass;
	
            try {
            	resClass = Class.forName(clsHeader.getValue());
	        } catch(ClassNotFoundException exc){
	            throw new LatherRemoteException("Server returned a class '" + clsHeader.getValue() + 
	                                            "' which the client did not have access to");
	        }
	
	        bIs = new ByteArrayInputStream(Base64.decode(responseBody));
	        dIs = new DataInputStream(bIs);
	
	        return this.xCoder.decode(dIs, resClass);
	    } else {
	        throw new IOException("Connection failure: " + response.getStatusLine());
	    }
    }
    
    private void configureProxy(HttpClient client) {                
        String proxyHost = System.getProperty("lather.proxyHost");
        int proxyPort = Integer.getInteger("lather.proxyPort", new Integer(-1)).intValue();
        
        if (proxyHost != null & proxyPort != -1) {
        	HttpHost proxy = new HttpHost(proxyHost, proxyPort);

        	client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }
    }
}
