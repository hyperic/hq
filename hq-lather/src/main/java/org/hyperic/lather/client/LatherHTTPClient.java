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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

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
import org.apache.http.conn.ssl.AbstractVerifier;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
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

    private HttpClient client;
    private LatherXCoder xCoder;
    private String       baseURL;
    
    public LatherHTTPClient(String baseURL) throws Exception {
        this(baseURL, TIMEOUT_CONN, TIMEOUT_DATA);
    }

    private final static String KEYSTORE_PASSWORD = "cl1ent!";
    private final static String KEYSTORE_PATH = "httpclient.keystore";

    private KeyStore getKeyStore() throws KeyStoreException, IOException {
    	FileInputStream keyStoreFileInputStream = null;

        try {
        	KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            File file = new File(KEYSTORE_PATH);
            char[] password = null;
            
            if (file.exists()) {
            	// ...keystore exist, so init the file input stream...
            	keyStoreFileInputStream = new FileInputStream(file);
            	password = KEYSTORE_PASSWORD.toCharArray();
            }

            keystore.load(keyStoreFileInputStream, password);

            return keystore;
        } catch (NoSuchAlgorithmException e) {
        	// can't check integrity of keystore, if this happens we're kind of screwed
        	// is there anything we can do to self heal this problem?
			e.printStackTrace();

			throw new IOException(e);
		} catch (CertificateException e) {
			// there are some corrupted certificates in the keystore, a bad thing
			// is there anything we can do to self heal this problem?
			e.printStackTrace();

			throw new IOException(e);
		} finally {
            if (keyStoreFileInputStream != null) {
            	keyStoreFileInputStream.close();
            	keyStoreFileInputStream = null;
            }
        }
    }

    private KeyManagerFactory getKeyManagerFactory(final KeyStore keystore) throws KeyStoreException, IOException {
    	try {
    		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
    		
    		keyManagerFactory.init(keystore, KEYSTORE_PASSWORD.toCharArray());
    		
    		return keyManagerFactory;
		} catch (NoSuchAlgorithmException e) {
			// no support for algorithm, if this happens we're kind of screwed
        	// we're using the default so it should never happen
			e.printStackTrace();

			throw new IOException(e);
		} catch (UnrecoverableKeyException e) {
			// invalid password, should never happen
			e.printStackTrace();
			
			throw new IOException(e);
		}
    }
    
    private TrustManagerFactory getTrustManagerFactory(final KeyStore keystore) throws KeyStoreException, IOException {
    	try {
	    	TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
	    	
	    	trustManagerFactory.init(keystore);
	    	
	    	return trustManagerFactory;
    	} catch (NoSuchAlgorithmException e) {
    		// no support for algorithm, if this happens we're kind of screwed
        	// we're using the default so it should never happen
			e.printStackTrace();

			throw new IOException(e);
		}
    }

    public LatherHTTPClient(String baseURL, int timeoutConn, int timeoutData) {
    	this(baseURL, timeoutConn, timeoutData, false);
    }
    
    public LatherHTTPClient(String baseURL, int timeoutConn, int timeoutData, final boolean acceptUnverifiedCertificates) {
    	try {
	        final KeyStore trustStore = getKeyStore();
	        KeyManagerFactory keyManagerFactory = getKeyManagerFactory(trustStore);
	        TrustManagerFactory trustManagerFactory = getTrustManagerFactory(trustStore);
	        
	        final X509TrustManager defaultTrustManager = (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
	        
	        X509TrustManager customTrustManager = new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					return defaultTrustManager.getAcceptedIssuers();
				}
				
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					try {
						defaultTrustManager.checkServerTrusted(chain, authType);
					} catch(Exception e) {
						if (!acceptUnverifiedCertificates) {
							throw new CertificateException(e);
						} else {
							FileOutputStream keyStoreFileOutputStream = null;
					        int i=0;
					        
					        try {
					        	keyStoreFileOutputStream = new FileOutputStream(KEYSTORE_PATH);
					        	
						        for (X509Certificate cert : chain) {
						        	String[] cnValues = AbstractVerifier.getCNs(cert);
						        	String alias = cnValues[0];
						        	
						        	trustStore.setCertificateEntry(alias, cert);
						        }
						        
						        trustStore.store(keyStoreFileOutputStream, KEYSTORE_PASSWORD.toCharArray());
					        } catch (FileNotFoundException fnfe) {
								// Bad news here
								fnfe.printStackTrace();
							} catch (KeyStoreException ke) {
								// TODO Auto-generated catch block
								ke.printStackTrace();
							} catch (NoSuchAlgorithmException nsae) {
								// TODO Auto-generated catch block
								nsae.printStackTrace();
							} catch (IOException ioe) {
								// TODO Auto-generated catch block
								ioe.printStackTrace();
							} finally {
					        	if (keyStoreFileOutputStream != null) {
					        		try { 
					        			keyStoreFileOutputStream.close();
					        		} catch(IOException ioe) {}
					        		
					        		keyStoreFileOutputStream = null;
					        	}
					        }
						}
					}
				}
				
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					defaultTrustManager.checkClientTrusted(chain, authType);
				}
			};
	        
	        SSLContext sslContext = SSLContext.getInstance("TLS");
	        
	        sslContext.init(keyManagerFactory.getKeyManagers(), new TrustManager[] { customTrustManager }, new SecureRandom());
	        
	        // XXX Should we use ALLOW_ALL_HOSTNAME_VERIFIER (least restrictive) or 
	        //     BROWSER_COMPATIBLE_HOSTNAME_VERIFIER (moderate restrictive) or
	        //     STRICT_HOSTNAME_VERIFIER (most restrictive)???
	        SSLSocketFactory socketFactory = new SSLSocketFactory(sslContext, new X509HostnameVerifier() {
				private AllowAllHostnameVerifier internalVerifier = new AllowAllHostnameVerifier();
				
				public boolean verify(String host, SSLSession session) {
					return internalVerifier.verify(host, session);
				}
				
				public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
					internalVerifier.verify(host, cns, subjectAlts);
				}
				
				public void verify(String host, X509Certificate cert) throws SSLException {
					internalVerifier.verify(host, cert);
				}
				
				public void verify(String host, SSLSocket ssl) throws IOException {
					try {
						internalVerifier.verify(host, ssl);
					} catch(SSLPeerUnverifiedException e) {
						throw new SSLPeerUnverifiedException("The authenticity of host '" + host + "' can't be established.");
					}
				}
			});
	        Scheme sslScheme = new Scheme("https", 443, socketFactory);
			
			this.client = new DefaultHttpClient();
			this.baseURL = baseURL;
	        this.xCoder  = new LatherXCoder();
	        
	        client.getConnectionManager().getSchemeRegistry().register(sslScheme);
	        client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, timeoutData);
	        client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeoutConn);
	        
	        // configure proxy, if appropriate
	        String proxyHost = System.getProperty("lather.proxyHost");
	        int proxyPort = Integer.getInteger("lather.proxyPort", new Integer(-1)).intValue();
	        
	        if (proxyHost != null & proxyPort != -1) {
	        	HttpHost proxy = new HttpHost(proxyHost, proxyPort);

	        	client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
	        }
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
        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        
        postParams.add(new BasicNameValuePair("method", method));
        postParams.add(new BasicNameValuePair("args", encodedArgs));
        postParams.add(new BasicNameValuePair("argsClass", args.getClass().getName()));
        
        HttpPost post = new HttpPost(baseURL);
        
        post.setEntity(new UrlEncodedFormEntity(postParams, "UTF-8"));
        
        HttpResponse response = null;
        
        response = client.execute(post);
	    
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
}
