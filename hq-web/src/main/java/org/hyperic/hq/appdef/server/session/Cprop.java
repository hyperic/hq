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

package org.hyperic.hq.appdef.server.session;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.appdef.server.session.CpropKey;

public class Cprop extends PersistedObject
{
    private CpropKey _key;
    private Integer _appdefId;
    private Integer _valueIdx;
    private String _propValue;

    public Cprop() {
        super();
    }

    public CpropKey getKey() {
        return _key;
    }

    protected void setKey(CpropKey keyId) {
        _key = keyId;
    }

    public Integer getAppdefId() {
        return _appdefId;
    }

    protected void setAppdefId(Integer appdefId) {
        _appdefId = appdefId;
    }

    public Integer getValueIdx() {
        return _valueIdx;
    }

    protected void setValueIdx(Integer valueIdx) {
        _valueIdx = valueIdx;
    }

    public String getPropValue() {
        return _propValue;
    }

    protected void setPropValue(String propValue) {
        _propValue = propValue;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Cprop) || !super.equals(obj)) {
            return false;
        }
        Cprop o = (Cprop) obj;
        return (_key == o.getKey() || (_key != null && o.getKey() != null &&
            _key.equals(o.getKey())))
            &&
            (_appdefId == o.getAppdefId() ||
                (_appdefId != null && o.getAppdefId() != null &&
                    _appdefId.equals(o.getAppdefId())))
            &&
            (_valueIdx == o.getValueIdx() ||
                (_valueIdx != null && o.getValueIdx() != null &&
                    _valueIdx.equals(o.getValueIdx())));
    }

    public int hashCode() {
        int result = super.hashCode();

        result = 37 * result + (_key != null ? _key.hashCode() : 0);
        result = 37 * result + (_appdefId != null ? _appdefId.hashCode() : 0);
        result = 37 * result + (_valueIdx != null ? _valueIdx.hashCode() : 0);

        return result;
    }
}
