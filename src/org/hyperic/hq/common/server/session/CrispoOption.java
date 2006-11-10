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
}
