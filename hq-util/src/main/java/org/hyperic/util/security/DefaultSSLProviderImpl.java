package org.hyperic.util.security;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.ssl.AbstractVerifier;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.ExecuteWatchdog;
import org.hyperic.util.exec.PumpStreamHandler;
import org.springframework.util.StringUtils;

public class DefaultSSLProviderImpl implements SSLProvider {
	private SSLContext sslContext;
	private SSLSocketFactory sslSocketFactory;
	private final Log log= LogFactory.getLog(DefaultSSLProviderImpl.class);
	
    private String getFQDN() {
        String address;
        final String loopback = "127.0.0.1";

        try {
            address = InetAddress.getLocalHost().getCanonicalHostName();
            
            if (!loopback.equals(address)) {
                return address;
            }
        } catch(UnknownHostException e) {
            //hostname not in DNS or /etc/hosts
        }

        Sigar sigar = new Sigar();
        
        try {
            address = sigar.getFQDN();
        } catch (SigarException e) {
            address = loopback;
        } finally {
            sigar.close();
        }

        return address;
    }
    
    private KeyManagerFactory getKeyManagerFactory(final KeyStore keystore, final String password) throws KeyStoreException, IOException {
    	try {
    		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
    		
    		keyManagerFactory.init(keystore, password.toCharArray());
    		
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
            
    public DefaultSSLProviderImpl(final KeystoreConfig keystoreConfig, final boolean acceptUnverifiedCertificates ){
        log.info("acceptUnverifiedCertificates="+acceptUnverifiedCertificates);
        try{  
            KeystoreManager keystoreMgr = KeystoreManager.getKeystoreManager();
            final KeyStore trustStore = keystoreMgr.getKeyStore(keystoreConfig);
            KeyManagerFactory keyManagerFactory = getKeyManagerFactory(trustStore, keystoreConfig.getFilePassword());
            TrustManagerFactory trustManagerFactory = getTrustManagerFactory(trustStore);
       
	        final X509TrustManager defaultTrustManager = (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
	        
	        X509TrustManager customTrustManager = new X509TrustManager() {
	            private final Log log = LogFactory.getLog(X509TrustManager.class);
				public X509Certificate[] getAcceptedIssuers() {
					return defaultTrustManager.getAcceptedIssuers();
				}
				
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					try {
						defaultTrustManager.checkServerTrusted(chain, authType);
					} catch(Exception e) {
			        	log.info("cert is not trusted, acceptUnverifiedCertificates="+acceptUnverifiedCertificates);
						if (!acceptUnverifiedCertificates) {
							throw new CertificateException(e);
						} else {
						    log.info("import the cert");
							importCertificate(chain);
						}
					}
				}

				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					defaultTrustManager.checkClientTrusted(chain, authType);
				}
				
				private void importCertificate(X509Certificate[] chain) throws CertificateException {
					FileOutputStream keyStoreFileOutputStream = null;
			        int i=0;
			        
			        try {
			        	keyStoreFileOutputStream = new FileOutputStream(keystoreConfig.getFilePath());
			        	
				        for (X509Certificate cert : chain) {
				        	String[] cnValues = AbstractVerifier.getCNs(cert);
				        	String alias = cnValues[0];
				        	
				        	trustStore.setCertificateEntry(alias, cert);
				        }
				        
				        trustStore.store(keyStoreFileOutputStream, keystoreConfig.getFilePassword().toCharArray());
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
			};
	        
	        sslContext = SSLContext.getInstance("TLS");
	        
	        sslContext.init(keyManagerFactory.getKeyManagers(), new TrustManager[] { customTrustManager }, new SecureRandom());
	        
	        // XXX Should we use ALLOW_ALL_HOSTNAME_VERIFIER (least restrictive) or 
	        //     BROWSER_COMPATIBLE_HOSTNAME_VERIFIER (moderate restrictive) or
	        //     STRICT_HOSTNAME_VERIFIER (most restrictive)???
	        sslSocketFactory = new SSLSocketFactory(sslContext, new X509HostnameVerifier() {
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
    	} catch(Exception e) {
    		throw new IllegalStateException(e);
    	}
    }
	
	public SSLContext getSSLContext() {
		return sslContext;
	}

	public SSLSocketFactory getSSLSocketFactory() {
		return sslSocketFactory;
	}
}