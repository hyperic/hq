package org.hyperic.hq.security.server.session;

import java.io.IOException;
import java.security.cert.Certificate;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.util.security.KeystoreEntry;

@SuppressWarnings("serial")
public class KeystoreEntryImpl extends PersistedObject implements KeystoreEntry {
    
    private String alias;
    private String type;
    private Certificate certificate;
    private Certificate[] certificateChain;
    private byte[] file ; 

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
    	return this.certificateChain ; 
    }//EOM 
    
    public Certificate getCertificate() {
    	return this.certificate ; 
    }//EOM 
    
    public void setCertificate(Certificate certificate) throws IOException{
        this.certificate = certificate;
    }
    
    public void setCertificateChain(Certificate[] chain) throws IOException{
        this.certificateChain = chain;
    }
    
    
    public void setFile(final byte[] fileContent) { 
        this.file = fileContent ; 
    }//EOM 
    
    public byte[] getFile() { 
        return this.file ; 
    }//EOM 


}
