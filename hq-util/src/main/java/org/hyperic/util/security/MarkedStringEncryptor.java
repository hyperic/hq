package org.hyperic.util.security;

import java.security.Provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.PBEConfig;
import org.jasypt.salt.SaltGenerator;

/**
 * a PBE Strign Encryptor, which marks the values it encrypts with a default prefix and postfix,
 * which enables us later on to know whether those values are encrypted or not.
 * 
 * @author yakarn
 *
 */
public class MarkedStringEncryptor implements PBEStringEncryptor {
    protected StandardPBEStringEncryptor encryptor;
    protected final static String PREFIX = SecurityUtil.ENC_MARK_PREFIX;
    protected final static String POSTFIX = SecurityUtil.ENC_MARK_POSTFIX;
    protected final Log logger = LogFactory.getLog(this.getClass().getName());

    public MarkedStringEncryptor() {
        this.encryptor = new StandardPBEStringEncryptor();
    }

    public MarkedStringEncryptor(String algorithm,String password) {
        this();
        this.setAlgorithm(algorithm);
        this.setPassword(password);
    }

    public String encrypt(String message) {
        logger.debug("encrypting: " + message);
        if (SecurityUtil.isMarkedEncrypted(message)) {
            logger.error("the following dsn is already encrypted: '" + message + "'");
            return message;
        }
        return SecurityUtil.mark(this.encryptor.encrypt(message));
    }

    public String decrypt(String encryptedMessage) {
        logger.debug("decrypting: " + encryptedMessage);
        if (!SecurityUtil.isMarkedEncrypted(encryptedMessage)) {
            logger.error("the following un-encrypted dsn exists in the DB: '" + encryptedMessage + "'");
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
