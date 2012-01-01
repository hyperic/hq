package org.hyperic.hq.security.server.session;

import org.hyperic.hibernate.PersistedObject;

@SuppressWarnings("serial")
public class HQCertificate extends PersistedObject {
    
    public String encoded;
    public String publicKey;
    public String type;
    private String publicKeyEncoded;
    private String publicKeyFormat;
    private String publicKeyAlgorithm;

    public String getEncoded() {
        return encoded;
    }
    public void setEncoded(String encoded) {
        this.encoded = encoded;
    }
    public String getPublicKey() {
        return publicKey;
    }
    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getPublicKeyEncoded() {
        return publicKeyEncoded;
    }
    public String getPublicKeyFormat() {
        return publicKeyFormat;
    }
    public String getPublicKeyAlgorithm() {
        return publicKeyAlgorithm;
    }
    public void setPublicKeyEncoded(String publicKeyEncoded) {
        this.publicKeyEncoded = publicKeyEncoded;
    }
    public void setPublicKeyFormat(String publicKeyFormat) {
        this.publicKeyFormat = publicKeyFormat;
    }
    public void setPublicKeyAlgorithm(String publicKeyAlgorithm) {
        this.publicKeyAlgorithm = publicKeyAlgorithm;
    }

}
