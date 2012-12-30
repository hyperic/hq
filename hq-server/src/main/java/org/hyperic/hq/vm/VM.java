package org.hyperic.hq.vm;

import java.io.Serializable;

public class VM implements Serializable {
    private static final long serialVersionUID = -833293198386426936L;
    private String mac;
    private String uuid;

    public VM(String mac, String uuid) {
        this.mac = mac;
        this.uuid = uuid;
    }
    public String getMac() {
        return mac;
    }
    public void setMac(String mac) {
        this.mac = mac;
    }
    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
