/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2011], VMWare, Inc.
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

package org.hyperic.util.security;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
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
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.hyperic.util.timer.StopWatch;

public class DefaultSSLProviderImpl implements SSLProvider {
    private SSLContext sslContext;
    private SSLSocketFactory sslSocketFactory;
    private static final Log log = LogFactory.getLog(DefaultSSLProviderImpl.class);

    private KeyManagerFactory getKeyManagerFactory(final KeyStore keystore, final String password)
    throws KeyStoreException {
        try {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, password.toCharArray());
            return keyManagerFactory;
        } catch (NoSuchAlgorithmException e) {
            // no support for algorithm, if this happens we're kind of screwed
            // we're using the default so it should never happen
            throw new KeyStoreException("The algorithm is not supported: "+e, e);
        } catch (UnrecoverableKeyException e) {
            // invalid password, should never happen
            throw new KeyStoreException("Password for the keystore is invalid: " + e,e);
        }
    }
    
    private TrustManagerFactory getTrustManagerFactory(final KeyStore keystore)
    throws KeyStoreException, IOException {
        try {
            TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keystore);
            return trustManagerFactory;
        } catch (NoSuchAlgorithmException e) {
            // no support for algorithm, if this happens we're kind of screwed
            // we're using the default so it should never happen
            log.error("The algorithm is not supported: "+e,e);
            throw new KeyStoreException(e);
        }
    }
            
    public DefaultSSLProviderImpl(KeystoreConfig keystoreConfig,
                                  boolean acceptUnverifiedCertificates ) {
        if (log.isDebugEnabled()) {
            log.debug("Keystore info: alias=" + keystoreConfig.getAlias() +
                      ", acceptUnverifiedCertificates=" + acceptUnverifiedCertificates);
        }
        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        try {
            KeystoreManager keystoreMgr = KeystoreManager.getKeystoreManager();
            KeyStore trustStore = keystoreMgr.getKeyStore(keystoreConfig);
            KeyManagerFactory keyManagerFactory = getKeyManagerFactory(trustStore, keystoreConfig.getFilePassword());
            TrustManagerFactory trustManagerFactory = getTrustManagerFactory(trustStore);
            X509TrustManager defaultTrustManager = (X509TrustManager) trustManagerFactory.getTrustManagers()[0];
            X509TrustManager customTrustManager =
                keystoreMgr.getCustomTrustManager(defaultTrustManager, keystoreConfig,
                                                  acceptUnverifiedCertificates, trustStore);
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
                    SSLPeerUnverifiedException sslPeerUnverifiedException =
                        new SSLPeerUnverifiedException("The authenticity of host '" + host +
                                                       "' can't be established: " + e);
                    sslPeerUnverifiedException.initCause(e);
                    throw sslPeerUnverifiedException;
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
