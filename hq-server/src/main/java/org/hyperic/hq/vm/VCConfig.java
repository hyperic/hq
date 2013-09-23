package org.hyperic.hq.vm;

import java.io.Serializable;

import org.hyperic.hibernate.usertypes.EncryptedStringType.LazyDecryptableValue;

public class VCConfig implements Serializable{
   
    private static final long serialVersionUID = 1L;
   
    private int id;
    private String url;
    private String user;
    private LazyDecryptableValue formula;
    private boolean setByUI;
    private  String vcUuid;
    private transient boolean lastSyncSucceeded = false;
    
    public VCConfig(){
        
    }

    public VCConfig(String url, String user, String password) {
        this.url = url;
        this.user = user;
        setPassword(password);
    }
    
    public LazyDecryptableValue getFormula() { 
        return this.formula ; 
    }//EOM 
    
    public void setFormula(final LazyDecryptableValue formula) { 
        this.formula = formula ;  
    }//EOM
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getUser() {
        return user;
    }
    
    public void setUser(String user) {
        this.user = user;
    }
    public String getPassword() {
        return formula.get();
    }
    
    public void setPassword(String password) {
        this.formula = LazyDecryptableValue.set(this.formula, password);
    }

    public boolean lastSyncSucceeded() {
        return lastSyncSucceeded;
    }

    public void setLastSyncSucceeded(boolean hasConnectionProblem) {
        this.lastSyncSucceeded = hasConnectionProblem;
    }
    
   

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isSetByUI() {
        return setByUI;
    }

    public void setSetByUI(boolean setByUI) {
        this.setByUI = setByUI;
    }
    

    public String getVcUuid() {
        return vcUuid;
    }

    public void setVcUuid(String vcUuid) {
        this.vcUuid = vcUuid;
    }
  

    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }
    

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null) return false;
        if(getClass() != obj.getClass()) return false;
        VCConfig other = (VCConfig) obj;
        if(id != other.id) return false;
        return true;
    }

    @Override
    public String toString() {
        return "VCConfig [id=" + id + ", url=" + url + ", user=" + user + ", setByUI=" + setByUI + ", vcUuid=" + vcUuid
                + ", lastSyncSucceeded=" + lastSyncSucceeded + "]";
    }
   



}
