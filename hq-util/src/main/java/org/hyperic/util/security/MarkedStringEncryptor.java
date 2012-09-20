/**
    NOTE: This copyright does *not* cover user programs that use HQ
    program services by normal system calls through the application
    program interfaces provided as part of the Hyperic Plug-in Development
    Kit or the Hyperic Client Development Kit - this is merely considered
    normal use of the program, and does *not* fall under the heading of
     "derived work".

     Copyright (C) [2009-2012], VMware, Inc.
     This file is part of HQ.

     HQ is free software; you can redistribute it and/or modify
     it under the terms version 2 of the GNU General Public License as
     published by the Free Software Foundation. This program is distributed
     in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
     even the implied warranty of MERCHANTABILITY or FITNESS FOR A
     PARTICULAR PURPOSE. See the GNU General Public License for more
     details.

     You should have received a copy of the GNU General Public License
     along with this program; if not, write to the Free Software
     Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
     USA.
*/ 
package org.hyperic.util.security;

import java.security.Provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.PBEConfig;
import org.jasypt.salt.SaltGenerator;
import org.springframework.beans.factory.InitializingBean;

/**
 * a PBE String Encryptor, which marks the values it encrypts with a default prefix and postfix,
 * which enables us later on to know whether those values are encrypted or not.
 * 
 * @author yakarn
 *
 */
public class MarkedStringEncryptor implements PBEStringEncryptor, InitializingBean {
    protected PooledPBEStringEncryptor encryptor;
    protected final static String PREFIX = SecurityUtil.ENC_MARK_PREFIX;
    protected final static String POSTFIX = SecurityUtil.ENC_MARK_POSTFIX;
    protected final Log logger = LogFactory.getLog(this.getClass().getName());
    private static final int DEFAULT_POOL_SIZE = 1 ; 

    public MarkedStringEncryptor(int poolsize) {
        this.encryptor = new PooledPBEStringEncryptor();
        this.setPoolSize(poolsize) ; 
    }//EOM

    public MarkedStringEncryptor() {
        this(DEFAULT_POOL_SIZE) ;  
    }//EOM
    
    public MarkedStringEncryptor(String algorithm,String password, int poolsize) {
    	this(poolsize);
        this.setAlgorithm(algorithm);
        this.setPassword(password); 
    }//EOM
    
    public MarkedStringEncryptor(String algorithm,String password) {
    	this(algorithm, password, DEFAULT_POOL_SIZE) ; 
    }//EOM

    public String encrypt(String message) {
        logger.debug("encrypting: " + message);
        if (SecurityUtil.isMarkedEncrypted(message)) {
            logger.error("the following data is already encrypted: '" + message + "'");
            return message;
        }
        return SecurityUtil.mark(this.encryptor.encrypt(message));
    }

    public String decrypt(String encryptedMessage) {
        logger.debug("decrypting: " + encryptedMessage);
        if (!SecurityUtil.isMarkedEncrypted(encryptedMessage)) {
            logger.error("the following un-encrypted data exists: '" + encryptedMessage + "'");
            return encryptedMessage;
        }
        return this.encryptor.decrypt(SecurityUtil.unmark(encryptedMessage));
    }

    public void setConfig(PBEConfig config) {
        encryptor.setConfig(config);
    }

    public void setAlgorithm(String algorithm) {
        encryptor.setAlgorithm(algorithm);
    }

    public void setKeyObtentionIterations(int keyObtentionIterations) {
        encryptor.setKeyObtentionIterations(keyObtentionIterations);
    }

    public void setSaltGenerator(SaltGenerator saltGenerator) {
        encryptor.setSaltGenerator(saltGenerator);
    }

    public void setProviderName(String providerName) {
        encryptor.setProviderName(providerName);
    }

    public void setProvider(Provider provider) {
        encryptor.setProvider(provider);
    }
    
    public void setStringOutputType(String stringOutputType) {
        encryptor.setStringOutputType(stringOutputType);
    }

    public boolean isInitialized() {
        return encryptor.isInitialized();
    }

    public void initialize() {
        encryptor.initialize();
    }
    
    public final void setPoolSize(final int poolSize) { 
    	this.encryptor.setPoolSize(poolSize) ; 
    }//EOM 
    
    public void afterPropertiesSet() throws Exception {
    	this.initialize() ; 
    }//EOM 
    
    @Override
    public int hashCode() {
        return this.encryptor.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this.encryptor.equals(obj);
    }

    public void setPassword(String password) {
        this.encryptor.setPassword(password);
    }

    @Override
    public String toString() {
        return this.encryptor.toString();
    }
}
