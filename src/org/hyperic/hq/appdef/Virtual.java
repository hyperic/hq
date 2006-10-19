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

/**
 *
 */
public class Virtual
{
    private Integer resourceId;
    private long _version_;
    private Integer processId;
    private Integer physicalId;

    /**
     * default constructor
     */
    public Virtual()
    {
        super();
    }

    // Property accessors
    public Integer getResourceId()
    {
        return this.resourceId;
    }

    public void setResourceId(Integer resourceId)
    {
        this.resourceId = resourceId;
    }

    public long get_version_()
    {
        return this._version_;
    }

    public void set_version_(long _version_)
    {
        this._version_ = _version_;
    }

    public Integer getProcessId()
    {
        return this.processId;
    }

    public void setProcessId(Integer processId)
    {
        this.processId = processId;
    }

    public Integer getPhysicalId()
    {
        return this.physicalId;
    }

    public void setPhysicalId(Integer physicalId)
    {
        this.physicalId = physicalId;
    }
}
