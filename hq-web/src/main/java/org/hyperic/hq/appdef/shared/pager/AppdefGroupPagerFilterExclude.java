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

import java.util.Set;
import java.util.Iterator;

import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;

/** Pager Processor filter that filters objects of both AppdefGroupValue
*   and AlldefEntityID based on their existance in an exclusionary set.
 */
public class AppdefGroupPagerFilterExclude implements AppdefPagerFilter {

    private Set     exclusionarySet;
    private boolean exclusive;
    private int filterCount;

    public Set getExclusionarySet () { return exclusionarySet; }
    public int getFilterCount ()     { return filterCount; }
    public  boolean isExclusive ()     { return exclusive; }

    public AppdefGroupPagerFilterExclude  ( Set excludes ) {
      this.exclusionarySet  = excludes;
      this.exclusive        = true;
      this.filterCount       = 0;
    }
    public AppdefGroupPagerFilterExclude  ( Set excludes, boolean negate ) {
      this.exclusionarySet = excludes;
      this.exclusive = (! negate);
      this.filterCount       = 0;
    }

   /** Evaluate an object against the filter.
    * @param object instance of either AppdefGroupValue or AppdefEntityID
    * @return flag - true if caught (unless negated)  */
    public boolean isCaught ( Object id ) {

        if (id instanceof AppdefGroupValue)  {
            id = ((AppdefGroupValue)id).getEntityId();
        }
     
        if (!(id instanceof AppdefEntityID)) {
            throw new IllegalArgumentException("Argument must be a group value or entity id.");
        }

        AppdefEntityID entityId = (AppdefEntityID) id;

        boolean caught = isExclude (entityId);

        if (exclusive == caught) {
            filterCount++;
        }
        return (exclusive == caught);
    }

    protected boolean isExclude ( AppdefEntityID id ) {

        Iterator i = exclusionarySet.iterator();
        while ( i.hasNext() ) {
            AppdefEntityID inSet = (AppdefEntityID) i.next();
            if ( inSet.getID()   == id.getID()   && 
                 inSet.getType() == id.getType()  )
                return true;
        }
        return false;
    }
   
}
