package org.hyperic.util.security;

import java.security.cert.Certificate;

public interface KeystoreEntry {
    
    public String getAlias();
    public void setAlias(String alias);
    public String getType();
    public void setType(String type);
    public Certificate getCertificate();
    public Certificate[] getCertificateChain();

}
