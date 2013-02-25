package org.hyperic.hq.vm;

public class VMID {
    protected String moref;
    protected String vcUUID;

    public VMID(String moref, String vcUUID) {
        this.moref = moref;
        this.vcUUID = vcUUID;
    }

    public String getMoref() {
        return moref;
    }
    
    public void setMoref(String moref) {
        this.moref = moref;
    }
    
    public String getVcUUID() {
        return vcUUID;
    }
    
    public void setVcUUID(String vcUUID) {
        this.vcUUID = vcUUID;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((moref == null) ? 0 : moref.hashCode());
        result = prime * result + ((vcUUID == null) ? 0 : vcUUID.hashCode());
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null) return false;
        if(getClass() != obj.getClass()) return false;
        VMID other = (VMID) obj;
        if(moref == null) {
            if(other.moref != null) return false;
        }else if(!moref.equals(other.moref)) return false;
        if(vcUUID == null) {
            if(other.vcUUID != null) return false;
        }else if(!vcUUID.equals(other.vcUUID)) return false;
        return true;
    }
    
    
    @Override
    public String toString() {
        return "VMID [moref=" + moref + ", vcUUID=" + vcUUID + "]";
    }
}
