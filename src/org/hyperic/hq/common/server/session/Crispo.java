/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

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

    void updateWith(ConfigResponse cfg) {
        // First, make any modifications to existing values, and add any
        // values not contained within the crispo
        for (Iterator i=cfg.getKeys().iterator(); i.hasNext(); ) {
            String key = (String)i.next();
            String val = cfg.getValue(key);
            
            boolean needToAdd = true;
            for (Iterator j=_opts.iterator(); j.hasNext(); ) {
                CrispoOption opt = (CrispoOption)j.next();
                
                if (opt.getKey().equals(key)) {
                    if (!opt.getValue().equals(val)) {
                        opt.setValue(val);
                    }
                    needToAdd = false;
                    break;
                }
            }
            
            if (needToAdd) {
                addOption(key, val);
            }
        }
        
        // Now remove any keys not contained within the cfg
        for (Iterator i = _opts.iterator(); i.hasNext(); ) {
            CrispoOption opt = (CrispoOption)i.next();
            
            if (cfg.getValue(opt.getKey()) == null || 
                opt.getValue() == null ||
                opt.getValue().length() == 0)
            {
                i.remove();
            }
        }
    }
    
    static Crispo create(Map keyVals) {
        Crispo res = new Crispo();
        
        for (Iterator i=keyVals.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry ent = (Map.Entry)i.next();
            String val = (String)ent.getValue();
            
            if (val == null || val.length() == 0)
                continue;
            
            res.addOption((String)ent.getKey(), val);
        }
        return res;
    }
    
    static Crispo create(ConfigResponse cfg) {
        Crispo res = new Crispo();
        
        for (Iterator i=cfg.getKeys().iterator(); i.hasNext(); ) {
            String key = (String)i.next();
            String val = cfg.getValue(key);
            
            if (val == null || val.length() == 0)
                continue;
            
            res.addOption(key, cfg.getValue(key));
        }
        return res;
    }
}
