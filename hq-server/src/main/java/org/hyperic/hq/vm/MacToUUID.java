package org.hyperic.hq.vm;

import org.hyperic.hibernate.PersistedObject;

public class MacToUUID extends PersistedObject {
    private static final long serialVersionUID = -833293198386426936L;
    
    protected String mac;
    protected String moRef;
    protected String vcUUID;
    
    public MacToUUID() {}
    public MacToUUID(String mac, String moRef, String vcUUID) {
        this.mac = mac;
        this.moRef = moRef;
        this.vcUUID = vcUUID;
    }
    public String getMac() {
        return mac;
    }
    public void setMac(String mac) {
        this.mac = mac;
    }
    public String getMORef() {
        return moRef;
    }
    public void setMORef(String moRef) {
        this.moRef = moRef;
    }
    public String getVcUUID() {
        return vcUUID;
    }
    public void setVcUUID(String vcUUID) {
        this.vcUUID = vcUUID;
    }
}
