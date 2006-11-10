package org.hyperic.hq.common.server.session;

import org.hyperic.hibernate.PersistedObject;

public class CrispoOption 
    extends PersistedObject
{
    private Crispo _crispo;
    private String _key;
    private String _val;
    
    protected CrispoOption() {}
    
    CrispoOption(Crispo crispo, String key, String val) {
        _crispo = crispo;
        _key    = key;
        _val    = val;
    }

    public Crispo getCrispo() {
        return _crispo;
    }
    
    protected void setCrispo(Crispo crispo) {
        _crispo = crispo;
    }
    
    public String getKey() {
        return _key == null ? "" : _key;
    }
    
    protected void setKey(String key) {
        _key = key;
    }
    
    public String getValue() {
        return _val == null ? "" : _val;
    }
    
    protected void setValue(String val) {
        _val = val;
    }
    
    public int hashCode() {
        int result = 17;
        
        result = 37*result + _crispo.hashCode();
        result = 37*result + _key.hashCode();
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null || obj instanceof CrispoOption == false)
            return false;
        
        CrispoOption opt = (CrispoOption)obj;
        return opt.getKey().equals(_key) && opt.getCrispo().equals(_crispo);
    }
}
