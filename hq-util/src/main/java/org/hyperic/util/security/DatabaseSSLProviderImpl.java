/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2011], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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

package org.hyperic.util.security;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
import org.hyperic.util.timer.StopWatch;

public class DatabaseSSLProviderImpl implements SSLProvider {

    private SSLContext sslContext;
    private SSLSocketFactory sslSocketFactory;
    private static final Log log = LogFactory.getLog(DefaultSSLProviderImpl.class);
    private static final ReadWriteLock KEYSTORE_LOCK = new ReentrantReadWriteLock();
    private static final Lock KEYSTORE_READER_LOCK = KEYSTORE_LOCK.readLock();
    private static final Lock KEYSTORE_WRITER_LOCK = KEYSTORE_LOCK.writeLock();

    private KeyManagerFactory getKeyManagerFactory(final KeyStore keystore, final String password) throws KeyStoreException {
        try {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, password.toCharArray());
            return keyManagerFactory;
        } catch (NoSuchAlgorithmException e) {
            // no support for algorithm, if this happens we're kind of screwed
            // we're using the default so it should never happen
            throw new KeyStoreException("The algorithm is not supported. Error message:"+e.getMessage());
        } catch (UnrecoverableKeyException e) {
            // invalid password, should never happen
            throw new KeyStoreException("Password for the keystore is invalid. Error message:"+e.getMessage());
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
            log.error("The algorithm is not supported. Error message:"+e.getMessage());
            throw new KeyStoreException(e);
        }
    }
            
    public DatabaseSSLProviderImpl(KeystoreConfig keystoreConfig, boolean acceptUnverifiedCertificates) {
        if (log.isDebugEnabled()) {
            log.debug("Keystore info: alias="+keystoreConfig.getAlias()+
                      ", path:"+keystoreConfig.getFilePath()+
                      ", acceptUnverifiedCertificates="+acceptUnverifiedCertificates);
        }
        boolean hasLock = false;
        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        try {
            KeystoreManager keystoreMgr = KeystoreManager.getKeystoreManager();
            KEYSTORE_READER_LOCK.lockInterruptibly();
            hasLock = true;
            KeyStore trustStore = keystoreMgr.getKeyStore(keystoreConfig);
            KeyManagerFactory keyManagerFactory = getKeyManagerFactory(trustStore, keystoreConfig.getFilePassword());
            TrustManagerFactory trustManagerFactory = getTrustManagerFactory(trustStore);
            X509TrustManager defaultTrustManager = (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
            X509TrustManager customTrustManager = getCustomTrustManager(defaultTrustManager,
                                                                        keystoreConfig,
                                                                        acceptUnverifiedCertificates,
                                                                        trustStore);
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(),
                            new TrustManager[] { customTrustManager },
                            new SecureRandom());
            // XXX Should we use ALLOW_ALL_HOSTNAME_VERIFIER (least restrictive) or 
            //     BROWSER_COMPATIBLE_HOSTNAME_VERIFIER (moderate restrictive) or
            //     STRICT_HOSTNAME_VERIFIER (most restrictive)???
            sslSocketFactory = new SSLSocketFactory(sslContext, getHostnameVerifier());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            if (hasLock) KEYSTORE_READER_LOCK.unlock();
            if (debug) log.debug("readCert: " + watch);
        }
    }
    
    private X509HostnameVerifier getHostnameVerifier() {
        return new X509HostnameVerifier() {
            private AllowAllHostnameVerifier internalVerifier = new AllowAllHostnameVerifier();
            public boolean verify(String host, SSLSession session) {
                return internalVerifier.verify(host, session);
            }
            public void verify(String host, String[] cns, String[] subjectAlts)
                throws SSLException {
                internalVerifier.verify(host, cns, subjectAlts);
            }
            public void verify(String host, X509Certificate cert)
                throws SSLException {
                internalVerifier.verify(host, cert);
            }
            public void verify(String host, SSLSocket ssl) throws IOException {
                try {
                    internalVerifier.verify(host, ssl);
                } catch (SSLPeerUnverifiedException e) {
                    throw new SSLPeerUnverifiedException("The authenticity of host '" + host +
                                                         "' can't be established.");
                }
            }
        };
    }

    private X509TrustManager getCustomTrustManager(final X509TrustManager defaultTrustManager,
                                                   final KeystoreConfig keystoreConfig,
                                                   final boolean acceptUnverifiedCertificates,
                                                   final KeyStore trustStore) {
        return new X509TrustManager() {
            private final Log log = LogFactory.getLog(X509TrustManager.class);

            public X509Certificate[] getAcceptedIssuers() {
                return defaultTrustManager.getAcceptedIssuers();
            }

            public void checkServerTrusted(X509Certificate[] chain,
                String authType) throws CertificateException {
                try {
                    defaultTrustManager.checkServerTrusted(chain, authType);
                } catch (CertificateException e){
                    CertificateExpiredException expiredCertException = getCertExpiredException(e);
                    if(expiredCertException!=null){
                        log.error("Fail the connection because received certificate is expired. " +
                                "Please update the certificate.",expiredCertException);
                        throw new CertificateException(e);
                    }
                    if (acceptUnverifiedCertificates) {
                        log.info("Import the certification. (Received certificate is not trusted by keystore)");
                        importCertificate(chain);
                    }else{
                        log.warn("Fail the connection because received certificate is not trusted by keystore: alias="
                            + keystoreConfig.getAlias()
                            + ", path=" + keystoreConfig.getFilePath());
                        log.debug("Fail the connection because received certificate is not trusted by keystore: alias="
                            + keystoreConfig.getAlias()
                            + ", path=" + keystoreConfig.getFilePath()
                            +", acceptUnverifiedCertificates="+acceptUnverifiedCertificates,e);
                        throw new CertificateException(e);
                    }
                }
            }
            
            private CertificateExpiredException getCertExpiredException(Exception e){  
                while(e !=null){
                    if(e instanceof CertificateExpiredException){
                        return (CertificateExpiredException)e;
                    }
                    e = (Exception) e.getCause();
                }
                return null;
            }

            public void checkClientTrusted(X509Certificate[] chain,
                String authType) throws CertificateException {
                defaultTrustManager.checkClientTrusted(chain, authType);
            }

            private void importCertificate(X509Certificate[] chain)
                throws CertificateException {
                FileOutputStream keyStoreFileOutputStream = null;
                boolean hasLock = false;
                final boolean debug = log.isDebugEnabled();
                final StopWatch watch = new StopWatch();
                try {
                    for (X509Certificate cert : chain) {
                        String[] cnValues = AbstractVerifier.getCNs(cert);
                        String alias;
                        
                        if (cnValues != null && cnValues.length > 0) {
                            alias = cnValues[0];
                        } else {
                            alias = "UnknownCN";
                        }
                        
                        alias += "-ts=" + System.currentTimeMillis();
                        
                        trustStore.setCertificateEntry(alias, cert);
                    }
                    KEYSTORE_WRITER_LOCK.lockInterruptibly();
                    hasLock = true;
                    keyStoreFileOutputStream = new FileOutputStream(keystoreConfig.getFilePath());
                    trustStore.store(keyStoreFileOutputStream, keystoreConfig
                        .getFilePassword().toCharArray());
                } catch (FileNotFoundException e) {
                    // Can't find the keystore in the path
                    log.error(
                        "Can't find the keystore in "
                            + keystoreConfig.getFilePath() + ". Error message:"
                            + e.getMessage(), e);
                } catch (NoSuchAlgorithmException e) {
                    log.error("The algorithm is not supported. Error message:"
                        + e.getMessage(), e);
                } catch (Exception e) {
                    // expect KeyStoreException, IOException
                    log.error("Exception when trying to import certificate: "
                        + e.getMessage(), e);
                } finally {
                    close(keyStoreFileOutputStream);
                    keyStoreFileOutputStream = null;
                    if (hasLock) {
                        KEYSTORE_WRITER_LOCK.unlock();
                    }
                    if (debug) log.debug("importCert: " + watch);
                }
            }

            private void close(FileOutputStream keyStoreFileOutputStream) {
                if (keyStoreFileOutputStream != null) {
                    try {
                        keyStoreFileOutputStream.close();
                    } catch (IOException e) {
                        log.error(e, e);
                    }
                }
            }
        };
    }

    public SSLContext getSSLContext() {
        return sslContext;
    }

    public SSLSocketFactory getSSLSocketFactory() {
        return sslSocketFactory;
    }
}
