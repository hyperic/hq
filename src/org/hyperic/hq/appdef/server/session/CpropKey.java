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

import java.util.Collection;

import org.hyperic.hibernate.PersistedObject;

public class CpropKey extends PersistedObject
{
    private int _appdefType;
    private int _appdefTypeId;
    private String _key;
    private String _description;
    private Collection _cprops;

    public Collection getCprops() {
        return _cprops;
    }

    protected void setCprops(Collection cprops) {
        _cprops = cprops;
    }

    public CpropKey() {
        super();
    }

    public int getAppdefType() {
        return _appdefType;
    }

    protected void setAppdefType(int appdefType) {
        _appdefType = appdefType;
    }

    public int getAppdefTypeId() {
        return _appdefTypeId;
    }

    protected void setAppdefTypeId(int appdefTypeId) {
        _appdefTypeId = appdefTypeId;
    }

    public String getKey() {
        return _key;
    }

    protected void setKey(String propKey) {
        _key = propKey;
    }

    public String getDescription() {
        return _description;
    }

    protected void setDescription(String description) {
        _description = description;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof CpropKey) || !super.equals(obj)) {
            return false;
        }
        CpropKey o = (CpropKey) obj;
        return (_key == o.getKey() || (_key != null && o.getKey() != null &&
            _key.equals(o.getKey())))
            &&
            _appdefType == o.getAppdefType()
            &&
            _appdefTypeId == o.getAppdefTypeId();
    }

    public int hashCode() {
        int result = 17;

        result = 37*result + (_key!=null ? _key.hashCode() : 0);
        result = 37*result + _appdefType;
        result = 37*result + _appdefTypeId;

        return result;
    }
}
