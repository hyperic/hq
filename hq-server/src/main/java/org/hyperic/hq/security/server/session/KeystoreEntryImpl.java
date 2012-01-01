package org.hyperic.hq.security.server.session;

import java.security.cert.Certificate;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.util.security.KeystoreEntry;

@SuppressWarnings("serial")
public class KeystoreEntryImpl extends PersistedObject implements KeystoreEntry {
    
    private String alias;
    private String type;
    private String encodedCertificate;
    private Certificate certificate;
    private Certificate[] certificateChain;
    private String encodedCertificateChain;

    public String getAlias() {
        return alias;
    }
    public void setAlias(String alias) {
        this.alias = alias;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public Certificate[] getCertificateChain() {
        return certificateChain;
    }
    public Certificate getCertificate() {
        return certificate;
    }
    String getEncodedCertificate() {
        return encodedCertificate;
    }
    void setEncodedCertificate(String encodedCertificate) {
        this.encodedCertificate = encodedCertificate;
    }
    String getEncodedCertificateChain() {
        return encodedCertificateChain;
    }
    void setEncodedCertificateChain(String encodedCertificateChain) {
        this.encodedCertificateChain = encodedCertificateChain;
    }
    void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }
    void setCertificateChain(Certificate[] chain) {
        this.certificateChain = chain;
    }

}
