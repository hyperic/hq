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

import org.hyperic.hibernate.PersistedObject;

/**
 *
 */
public class Cprop extends PersistedObject
{
    private Integer key;
    private Integer appdefId;
    private Integer valueIdx;
    private String propValue;

    // Constructors

    /**
     * default constructor
     */
    public Cprop()
    {
        super();
    }

    public Integer getKey()
    {
        return this.key;
    }

    public void setKey(Integer keyId)
    {
        this.key = keyId;
    }

    public Integer getAppdefId()
    {
        return this.appdefId;
    }

    public void setAppdefId(Integer appdefId)
    {
        this.appdefId = appdefId;
    }

    public Integer getValueIdx()
    {
        return this.valueIdx;
    }

    public void setValueIdx(Integer valueIdx)
    {
        this.valueIdx = valueIdx;
    }

    public String getPropValue()
    {
        return this.propValue;
    }

    public void setPropValue(String propValue)
    {
        this.propValue = propValue;
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof Cprop) || !super.equals(obj)) {
            return false;
        }
        Cprop o = (Cprop)obj;
        return (key==o.getKey() || (key!=null && o.getKey()!=null &&
                                    key.equals(o.getKey())))
               &&
               (appdefId==o.getAppdefId() ||
                (appdefId!=null && o.getAppdefId()!=null &&
                 appdefId.equals(o.getAppdefId())))
               &&
               (valueIdx==o.getValueIdx() ||
                (valueIdx!=null && o.getValueIdx()!=null &&
                 valueIdx.equals(o.getValueIdx())));
    }

    public int hashCode()
    {
        int result = super.hashCode();

        result = 37*result + (key!=null ? key.hashCode() : 0);
        result = 37*result + (appdefId!=null ? appdefId.hashCode() : 0);
        result = 37*result + (valueIdx!=null ? valueIdx.hashCode() : 0);

        return result;
    }

}
