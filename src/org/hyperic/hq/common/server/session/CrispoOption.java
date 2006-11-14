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
