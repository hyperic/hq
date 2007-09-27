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

package org.hyperic.hq.appdef.shared.pager;

import java.util.Iterator;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;

/** 
* A filter employed by the AppdefGroupPagerProcessor.  It operates during 
* the paging process and returns a flag based on whether or not an 
* AppdefGroupValue's type evaluates to the specified group type.
*
*/
public class AppdefGroupPagerFilterGrpTypeExclude implements AppdefPagerFilter {

    private int groupType;
    private boolean exclusive;
    private int filterCount;

    public int getFilterCount () { return filterCount; }
    public  boolean isExclusive () { return exclusive; }

    public AppdefGroupPagerFilterGrpTypeExclude  ( int groupType ) {
        this.groupType = groupType;
        this.exclusive = true;
    }

    public AppdefGroupPagerFilterGrpTypeExclude  ( int groupType, 
        boolean negate ) {
        this.groupType = groupType;
        this.exclusive = (! negate);
    }

   /** Evaluate an object against the filter.
    * @param object instance of AppdefGroupValue
    * @return flag - true if caught (unless negated)  */
    public boolean isCaught ( Object id ) {

        if (!(id instanceof AppdefGroupValue))
            throw new IllegalArgumentException("Invalid appdef group value");

        AppdefGroupValue gVo = (AppdefGroupValue) id;
        return exclusive == matchesGroup (gVo);

    }

    protected boolean matchesGroup ( AppdefGroupValue groupVo ) {
        return groupVo.getGroupType() == this.groupType;
    }
}

