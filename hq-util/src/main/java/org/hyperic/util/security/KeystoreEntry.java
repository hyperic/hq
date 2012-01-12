package org.hyperic.util.security;

import java.io.IOException;
import java.security.cert.Certificate;

public interface KeystoreEntry {
    
    public String getAlias();
    public void setAlias(String alias);
    public String getType();
    public void setType(String type);
    
    Certificate getCertificate();
    void setCertificate(final Certificate cert) throws IOException ; 
    
    Certificate[] getCertificateChain();
    void setCertificateChain(final Certificate[] arrCertificateChain) throws IOException; 
    
    void setFile(byte[] arrFile) ; 
    byte[] getFile() ; 

}
