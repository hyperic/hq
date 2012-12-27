package org.hyperic.hq.vm;

public class VM {
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
