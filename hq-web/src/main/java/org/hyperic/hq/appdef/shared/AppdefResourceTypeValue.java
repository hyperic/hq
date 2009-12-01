/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.appdef.shared;

import java.text.DateFormat;

/**
 * Abstract base class for firt class appdef resource types.  This was 
 * carbon copied from Soltero's AppdefResourceValue equivalent because
 * 75% of it is the same.
 *
 * The accessors provided in this class represent what the UI model labels
 * "General Properties". Any other attribute is assumed to be specific
 * to the resource type.
 *
 *
 */
public abstract class AppdefResourceTypeValue {
    final DateFormat dateFmt = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                                                              DateFormat.MEDIUM);
    // they all have id's
    public abstract Integer getId();
    public abstract void setId(Integer id);

    // they all have names
    public abstract String getName();
    public abstract void setName(String name);

    // they all have descriptions
    public abstract String getDescription();
    public abstract void setDescription(String desc);

    // they all have ctime 
    public abstract Long getCTime();

    // they all have mtime
    public abstract Long getMTime();

    /** used by the UI
     * 
     * @return formatted create time
     */
    public String getCreateTime()
    {
        Long ctime = getCTime();
        if (ctime == null)
            ctime = new Long(System.currentTimeMillis());
        return dateFmt.format(ctime);
    }
    
    /** used by the UI
     * 
     * @return formatted modified time
     */
    public String getModifiedTime()
    {
        Long mtime = getMTime();
        if (mtime == null)
            mtime = new Long(System.currentTimeMillis());
        return dateFmt.format(mtime);
    }
    
    /**
     * returns a stringified id in the form
     *  
     * [appdefType id]:[id]
     * 
     * @return a string based id
     */
    public String getAppdefTypeKey(){
        return getAppdefType() + ":" + getId();
    }

    /** 
     * @return appdef int value designator of entity type.
     */
    public abstract int getAppdefType();
}
