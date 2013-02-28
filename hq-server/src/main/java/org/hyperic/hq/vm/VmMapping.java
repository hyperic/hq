package org.hyperic.hq.vm;

import java.io.Serializable;

import com.vmware.vim25.GuestNicInfo;

public class VmMapping implements Serializable {
    

    private static final long serialVersionUID = -833293198386426936L;
    
    protected String macs;
    protected String moRef;
    protected String vcUUID;
    private String name;
    private transient GuestNicInfo[] guestNicInfo;
    private Long    _version_;
    
    public VmMapping() {}
    
    public VmMapping(String moRef, String vcUUID) {
        this.moRef = moRef;
        this.vcUUID = vcUUID;
    }
    
    public VmMapping(String moRef, String vcUUID, String macs) {
        this.moRef = moRef;
        this.vcUUID = vcUUID;
        this.macs = macs;
    }
    
    
    public void setMacs(String macs) {
        this.macs = macs;
    }

    public String getMacs() {
        return macs;
    }
    

    public String getMoref() {
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
    
    public long get_version_() {
        return _version_ != null ? _version_.longValue() : 0;
    }

    protected void set_version_(Long newVer) {
        _version_ = newVer;
    }
      
   
    public GuestNicInfo[] getGuestNicInfo() {
        return guestNicInfo;
    }

    public void setGuestNicInfo(GuestNicInfo[] guestNicInfo) {
        this.guestNicInfo = guestNicInfo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return "VmMapping [macs=" + macs + ", moRef=" + moRef + ", vcUUID=" + vcUUID + ", name=" + name
                + ", _version_=" + _version_ + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((moRef == null) ? 0 : moRef.hashCode());
        result = prime * result + ((vcUUID == null) ? 0 : vcUUID.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null) return false;
        if(getClass() != obj.getClass()) return false;
        VmMapping other = (VmMapping) obj;
        if(moRef == null) {
            if(other.moRef != null) return false;
        }else if(!moRef.equals(other.moRef)) return false;
        if(vcUUID == null) {
            if(other.vcUUID != null) return false;
        }else if(!vcUUID.equals(other.vcUUID)) return false;
        return true;
    }

}
