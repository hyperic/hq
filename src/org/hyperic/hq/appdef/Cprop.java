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

import java.io.Serializable;

/**
 *
 */
public class Cprop implements Serializable
{
    private Integer id;
    private long _version_;
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

    // Property accessors
    public Integer getId()
    {
        return this.id;
    }

    private void setId(Integer id)
    {
        this.id = id;
    }

    public long get_version_()
    {
        return this._version_;
    }

    private void set_version_(long _version_)
    {
        this._version_ = _version_;
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

    // TODO: fix equals and hashCode
    public boolean equals(Object other)
    {
        if ((this == other)) return true;
        if ((other == null)) return false;
        if (!(other instanceof Cprop)) return false;
        Cprop castOther = (Cprop) other;

        return ((this.getKey() == castOther.getKey()) || (this.getKey() != null && castOther.getKey() != null && this.getKey().equals(castOther.getKey())))
               && ((this.getAppdefId() == castOther.getAppdefId()) || (this.getAppdefId() != null && castOther.getAppdefId() != null && this.getAppdefId().equals(castOther.getAppdefId())))
               && ((this.getValueIdx() == castOther.getValueIdx()) || (this.getValueIdx() != null && castOther.getValueIdx() != null && this.getValueIdx().equals(castOther.getValueIdx())));
    }

    public int hashCode()
    {
        int result = 17;


        return result;
    }

}
