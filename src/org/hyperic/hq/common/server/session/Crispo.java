package org.hyperic.hq.common.server.session;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.util.config.ConfigResponse;

/**
 * A Crispo is a collection of String Key/Value pairs.
 * 
 * The motivation behind its creation is to provide a real database storage
 * mechanism for {@link ConfigResponse} objects.
 */
public class Crispo
    extends PersistedObject
{
    private Collection _opts = new HashSet();
    
    protected Crispo() {}
    
    /**
     * Return a collection of {@link CrispoOption}s
     */
    public Collection getOptions() {
        return Collections.unmodifiableCollection(_opts);
    }
    
    protected Collection getOptsSet() {
        return _opts;
    }
    
    protected void setOptsSet(Collection opts) {
        _opts = opts;
    }
    
    void addOption(String key, String val) {
        getOptsSet().add(new CrispoOption(this, key, val));
    }

    public int hashCode() {
        return getId() == null ? 0 : getId().intValue();
    }
    
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null || obj instanceof Crispo == false)
            return false;

        Crispo o = (Crispo)obj;
        if (getId() == null || o.getId() == null)
            return false;

        return getId().equals(o.getId());
    }

    /**
     * Create a new {@link ConfigResponse} based on the key/values stored
     * within this object.
     */
    public ConfigResponse toResponse() {
        ConfigResponse res = new ConfigResponse();
        
        for (Iterator i=getOptions().iterator(); i.hasNext(); ) {
            CrispoOption opt = (CrispoOption)i.next();
            
            res.setValue(opt.getKey(), opt.getValue());
        }
        return res;
    }
    
    static Crispo create(Map keyVals) {
        Crispo res = new Crispo();
        
        for (Iterator i=keyVals.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry ent = (Map.Entry)i.next();
            
            res.addOption((String)ent.getKey(), (String)ent.getValue());
        }
        return res;
    }
    
    static Crispo create(ConfigResponse cfg) {
        Crispo res = new Crispo();
        
        for (Iterator i=cfg.getKeys().iterator(); i.hasNext(); ) {
            String key = (String)i.next();
            
            res.addOption(key, cfg.getValue(key));
        }
        return res;
    }
}
