/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.PersistedObject;

public class CrispoOption 
    extends PersistedObject
{
    Log _log = LogFactory.getLog(CrispoOption.class.getName());
    
    private static final String[] ARRAY_DESCRIMINATORS = {".resources",
                                                          ".portlets.",
                                                          ".indicator.views",
                                                          ".charts"};
    private static final String ARRAY_DELIMITER = "|";
    
    private Crispo _crispo;
    private String _key;
    private String _val;
    
    private List _array = new ArrayList(0);
    
    protected CrispoOption() {}
    
    CrispoOption(Crispo crispo, String key, String val) {
        _crispo = crispo;
        _key    = key;
        setValue(val);
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
    
    protected String getOptionValue() {
        return _val == null ? "" : _val;
    }
    
    protected void setOptionValue(String val) {
        _val = val;
    }
    
    public String getValue() {
        if (_val != null && _val.trim().length() > 0) {
            return _val;
        } else if (_val == null && _array.size() == 0) {
            return "";
        } else {
            Iterator itr = _array.iterator();
            StringBuffer val = new StringBuffer();
            while (itr.hasNext()) {
                String item = (String) itr.next();
                if (item != null && item.length() > 0) {
                    val.append(ARRAY_DELIMITER).append(item);
                }
            }
            return val.toString();
        }
    }
    
    protected void setValue(String val) {
        for (int i = 0; i < ARRAY_DESCRIMINATORS.length; i++) {
            if (_key.indexOf(ARRAY_DESCRIMINATORS[i]) > -1) {
                if (val != null && val.trim().length() > 0) {
                    _array = new ArrayList();
                    String[] elem = val.split("\\" + ARRAY_DELIMITER);
                    for (int j = 0; j < elem.length; j++) {
                        if (elem[j] != null && elem[j].trim().length() > 0)
                            _array.add(elem[j]);
                        
                        if (_log.isDebugEnabled())
                            _log.debug("Adding: {"+elem[j]+"}");
                    }
                }
                _val = null;
                return;
            }
        }

        _val = val;
    }
    
    protected void setArray(List array) {
        _array = array;
    }
    
    protected List getArray() {
        return _array;
    }
    
    public int hashCode() {
        int result = 17;
        
        result = 37*result + _crispo.hashCode();
        result = 37*result + _key.hashCode();
        result = 37*result + _array.hashCode();
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null || obj instanceof CrispoOption == false)
            return false;
        
        CrispoOption opt = (CrispoOption)obj;
        return opt.getKey().equals(_key) && opt.getCrispo().equals(_crispo) 
               && opt.getArray().equals(_array);
    }
    
}
