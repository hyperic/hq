/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.util.security;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.X509TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.ssl.AbstractVerifier;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.ExecuteWatchdog;
import org.hyperic.util.exec.PumpStreamHandler;
import org.hyperic.util.file.FileUtil;
import org.hyperic.util.timer.StopWatch;
import org.springframework.util.StringUtils;

public class KeystoreManager {
    private final Log log;
    private final AtomicBoolean isDB = new AtomicBoolean(false);
    private static KeystoreManager keystoreManager= new KeystoreManager();
    
    private KeystoreManager() {
        this.log = LogFactory.getLog(KeystoreManager.class);
    }
    
    public static KeystoreManager getKeystoreManager() {
        return keystoreManager;
    }
    
    private String getDName(KeystoreConfig keystoreConfig) {
        return "CN=" + keystoreConfig.getKeyCN()+
               " (HQ Self-Signed Cert), OU=HQ, O=hyperic.net, L=Unknown, ST=Unknown, C=US";
    }
    
    public KeyStore getKeyStore(KeystoreConfig keystoreConfig) throws KeyStoreException, IOException {
        FileInputStream keyStoreFileInputStream = null;
        
        String filePath = keystoreConfig.getFilePath();
        String filePassword = keystoreConfig.getFilePassword();
        
        //check if keystoreConfig valid (block if it's null or "")
        String errorMsg="";
        if(keystoreConfig.getAlias()==null){
            errorMsg+=" alias is null. ";
        }
        if(keystoreConfig.getFilePath()==null){
            errorMsg+=" filePath is null. ";
        }
        if(keystoreConfig.getFilePassword()==null){
            errorMsg+=" password is null. ";
        }
        if(!"".equals(errorMsg)){
            throw new KeyStoreException(errorMsg);
        }
        
	    try {
            KeyStore keystore = DbKeyStore.getInstance(KeyStore.getDefaultType(), isDB);
	        File file = new File(filePath);
	        char[] password = null;
	            
	        if (!file.exists()) {
	        	// ...if file doesn't exist, and path was user specified throw IOException...
	            if (StringUtils.hasText(filePath) && !keystoreConfig.isHqDefault()) {
	            	throw new IOException("User specified keystore [" + filePath + "] does not exist.");
	            }
	                
	            password = filePassword.toCharArray();
	            createInternalKeystore(keystoreConfig);	            
	            FileUtil.setReadWriteOnlyByOwner(file);          
	        }
	            
	        // ...keystore exist, so init the file input stream...
	        keyStoreFileInputStream = new FileInputStream(file);
	            
	        keystore.load(keyStoreFileInputStream, password);
	
	        return keystore;
	    } catch (NoSuchAlgorithmException e) {
	    	// can't check integrity of keystore, if this happens we're kind of screwed
	    	// is there anything we can do to self heal this problem?
	    	errorMsg = "The algorithm used to check the integrity of the keystore cannot be found.";
	    	throw new KeyStoreException(errorMsg,e);
	    } catch (CertificateException e) {
	    	// there are some corrupted certificates in the keystore, a bad thing
	    	// is there anything we can do to self heal this problem?
	    	errorMsg = "Keystore cannot be loaded. One possibility is that the password is incorrect.";
	    	throw new KeyStoreException(errorMsg, e);
	    } finally {
	    	if (keyStoreFileInputStream != null) {
	    		keyStoreFileInputStream.close();
	    		keyStoreFileInputStream = null;
	    	}
	    }        
    }


    private void createInternalKeystore(KeystoreConfig keystoreConfig) throws KeyStoreException{
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String javaHome = System.getProperty("java.home");
        String keytool = javaHome + File.separator + "bin" + File.separator + "keytool";
        String[] args = {
            keytool,
            "-genkey",
            "-dname",    getDName(keystoreConfig),
            "-alias",     keystoreConfig.getAlias(),
            "-keystore",  keystoreConfig.getFilePath(),
            "-storepass", keystoreConfig.getFilePassword(),
            "-keypass",   keystoreConfig.getFilePassword(),
            "-keyalg",    "RSA",
            "-validity", "3650"  //10 years
        };

        int timeout = 5 * 60 * 1000; //5min
        ExecuteWatchdog wdog = new ExecuteWatchdog(timeout);
        Execute exec = new Execute(new PumpStreamHandler(output), wdog);
        
        exec.setCommandline(args);
        
        //TODO shouldn't have password in log
        log.debug("Generating keystore: " +
        		keystoreConfig.getFilePath());

        int rc;
        
        try {
            rc = exec.execute();
        } catch (Exception e) {
            rc = -1;
            log.error(e);
        }
        
        if (rc != 0) {
            String msg = output.toString().trim();

            if (msg.length() == 0) {
                msg = "timeout after " + timeout + "ms";
            }

            // TODO This is super fugly but considering how we're creating the keystore file, there isn't a clean way of accomplishing this
            //      Basically, there is a small window of opportunity where two agent processes could discover no keystore file and try to
            //      generate one using the ExceuteWatchdog.  One will succeed, the other will fail, if that happens we shouldn't kill the process.  
            //      For any other exception throw it...
            if (!msg.toLowerCase().contains("key pair not generated, alias <" + keystoreConfig.getAlias().toLowerCase() + "> already exists")) {
            	//can't have password in log
            	throw new KeyStoreException("Failed to create keystore:"+keystoreConfig.getAlias()+", "+msg);
            }
        } 
    }

    public X509TrustManager getCustomTrustManager(X509TrustManager defaultTrustManager,
                                                  KeystoreConfig keystoreConfig,
										          boolean acceptUnverifiedCertificates,
										          KeyStore trustStore) {
        return new CustomTrustManager(defaultTrustManager, keystoreConfig,
                                      acceptUnverifiedCertificates, trustStore, isDB.get());
    }
    
    private class CustomTrustManager implements X509TrustManager {
        private final Log log = LogFactory.getLog(X509TrustManager.class);
        private final X509TrustManager defaultTrustManager;
        private final KeystoreConfig keystoreConfig;
        private final boolean acceptUnverifiedCertificates;
        private final KeyStore trustStore;
        private final boolean isDB;
        private CustomTrustManager(X509TrustManager defaultTrustManager,
                                   KeystoreConfig keystoreConfig,
                                   boolean acceptUnverifiedCertificates,
                                   KeyStore trustStore, boolean isDB) {
            this.defaultTrustManager = defaultTrustManager;
            this.keystoreConfig = keystoreConfig;
            this.acceptUnverifiedCertificates = acceptUnverifiedCertificates;
            this.trustStore = trustStore;
            this.isDB = isDB;
        }
        public X509Certificate[] getAcceptedIssuers() {
            return defaultTrustManager.getAcceptedIssuers();
        }
        public void checkServerTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {
            try {
                defaultTrustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException e){
                CertificateExpiredException expiredCertException = getCertExpiredException(e);
                if (expiredCertException!=null){
                    log.error("Fail the connection because received certificate is expired. " +
                            "Please update the certificate.",expiredCertException);
                    throw new CertificateException(e);
                }
                if (acceptUnverifiedCertificates) {
                    log.info("Import the certification. (Received certificate is not trusted by keystore)");
                    importCertificate(chain);
                } else {
                    log.warn("Fail the connection because received certificate is not trusted by " +
                             "keystore: alias=" + keystoreConfig.getAlias());
                    log.debug("Fail the connection because received certificate is not trusted by " +
                              "keystore: alias=" + keystoreConfig.getAlias() +
                              ", acceptUnverifiedCertificates="+acceptUnverifiedCertificates,e);
                    throw new CertificateException(e);
                }
            }
        }
        private CertificateExpiredException getCertExpiredException(Exception e){  
            while (e !=null){
                if (e instanceof CertificateExpiredException){
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
            FileOutputStream ksFileOutputStream = null;
            final boolean debug = log.isDebugEnabled();
            final StopWatch watch = new StopWatch();
            try {
                for (X509Certificate cert : chain) {
                    String[] cnValues = AbstractVerifier.getCNs(cert);
                    String alias = (cnValues != null && cnValues.length > 0) ? cnValues[0] : "UnknownCN";
                    alias += "-ts=" + System.currentTimeMillis();
                    trustStore.setCertificateEntry(alias, cert);
                }
                if (!isDB) {
                    ksFileOutputStream = new FileOutputStream(keystoreConfig.getFilePath());
                    trustStore.store(ksFileOutputStream, keystoreConfig.getFilePassword().toCharArray());
                }
            } catch (FileNotFoundException e) {
                // Can't find the keystore in the path
                log.error("Can't find the keystore in " + keystoreConfig.getFilePath() +
                          ". Error message: " + e, e);
            } catch (NoSuchAlgorithmException e) {
                log.error("The algorithm is not supported. Error message: " + e, e);
            } catch (Exception e) {
                // expect KeyStoreException, IOException
                log.error("Exception when trying to import certificate: " + e, e);
            } finally {
                close(ksFileOutputStream);
                ksFileOutputStream = null;
                if (debug) log.debug("importCert: " + watch);
            }
        }
        private void close(FileOutputStream fos) {
            if (fos == null) {
                return;
            }
            try {
                fos.close();
            } catch (IOException e) {}
        }
    }
}
