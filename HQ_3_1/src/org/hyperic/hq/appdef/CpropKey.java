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

package org.hyperic.hq.appdef;

import org.hyperic.hq.appdef.shared.CPropKeyValue;
import org.hyperic.hibernate.PersistedObject;

import java.util.Collection;

/**
 *
 */
public class CpropKey extends PersistedObject
{
    private Integer appdefType;
    private Integer appdefTypeId;
    private String key;
    private String description;

    public Collection getCprops()
    {
        return cprops;
    }

    public void setCprops(Collection cprops)
    {
        this.cprops = cprops;
    }

    private Collection cprops;

    /**
     * default constructor
     */
    public CpropKey()
    {
        super();
    }

    public Integer getAppdefType()
    {
        return this.appdefType;
    }

    public void setAppdefType(Integer appdefType)
    {
        this.appdefType = appdefType;
    }

    public Integer getAppdefTypeId()
    {
        return this.appdefTypeId;
    }

    public void setAppdefTypeId(Integer appdefTypeId)
    {
        this.appdefTypeId = appdefTypeId;
    }

    public String getKey()
    {
        return this.key;
    }

    public void setKey(String propKey)
    {
        this.key = propKey;
    }

    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    private CPropKeyValue _value = new CPropKeyValue();
    /**
     * for legacy EJB Entity Bean compatibility
     * @return
     */
    public CPropKeyValue getCPropKeyValue()
    {
        _value.setAppdefType(appdefType == null ? 0 : appdefType.intValue());
        _value.setAppdefTypeId(appdefTypeId == null ? 0 : appdefTypeId.intValue());
        _value.setDescription(description == null ? "" : description);
        _value.setKey(key == null ? "" : key);
        _value.setId(getId());
        return _value;
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof CpropKey) || !super.equals(obj)) {
            return false;
        }
        CpropKey o = (CpropKey)obj;
        return (key==o.getKey() || (key!=null && o.getKey()!=null &&
                                    key.equals(o.getKey())))
               &&
               (appdefType==o.getAppdefType() ||
                (appdefType!=null && o.getAppdefType()!=null &&
                 appdefType.equals(o.getAppdefType())))
               &&
               (appdefTypeId==o.getAppdefTypeId() ||
                (appdefTypeId!=null && o.getAppdefTypeId()!=null &&
                 appdefTypeId.equals(o.getAppdefTypeId())));
    }

    public int hashCode()
    {
        int result = 17;

        result = 37*result + (key!=null ? key.hashCode() : 0);
        result = 37*result + (appdefType!=null ? appdefType.hashCode() : 0);
        result = 37*result + (appdefTypeId!=null ? appdefTypeId.hashCode() : 0);

        return result;
    }

}
