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
public abstract class AppdefResourceType extends AppdefBean
{
    protected String name;
    protected String sortName;
    protected String description;

    /**
     * default constructor
     */
    public AppdefResourceType()
    {
        super();
    }

    public AppdefResourceType(Integer id)
    {
        super(id);
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
        setSortName(name);
    }

    public String getSortName()
    {
        return this.sortName;
    }

    public void setSortName(String sortName)
    {
        if (sortName != null) {
            this.sortName = sortName.toUpperCase();
        } else {
            this.sortName = null;
        }
    }

    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public boolean equals(Object o)
    {
        if (!super.equals(o) || !(o instanceof AppdefResourceType)) {
            return false;
        }
        AppdefResourceType a = (AppdefResourceType)o;
        return
            ((name==a.getName()) || (name!=null && a.getName()!=null &&
                                     name.equals(a.getName())));
    }

    public int hashCode()
    {
        int result = super.hashCode();

        result = 37*result + (name!=null ? name.hashCode() : 0);

        return result;
    }
}
