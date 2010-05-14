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

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;

/** Pager Processor filter that filters object instances of
*   AppdefEntityID based on their membership to a particular
*   AppdefGroupValue.
*/
public class AppdefPagerFilterGroupMemExclude implements AppdefPagerFilter {

    private AppdefGroupValue   candidate; 
    private boolean            exclusive;
    private int                filterCount;

    public AppdefGroupValue  getCandidate ()   { return candidate; }
    public int               getFilterCount () { return filterCount; }
    public  boolean          isExclusive ()    { return exclusive; }

    public AppdefPagerFilterGroupMemExclude  ( AppdefGroupValue candidate ) {
        this.candidate = candidate;
        this.exclusive = true;
    }

    public AppdefPagerFilterGroupMemExclude  ( AppdefGroupValue candidate,
        boolean negate ) {
        this.candidate = candidate;
        this.exclusive = (! negate);
    }

   /** Evaluate an appdef entity against the filter.
    * @param object instance of AppdefEntityID
    * @return flag - true if caught (unless negated)  
    */
    public boolean isCaught ( Object o ) {

        if (!(o instanceof AppdefEntityID)) {
            throw new IllegalArgumentException("Invalid appdef entity id");
        }

        AppdefEntityID entity = (AppdefEntityID) o;

        boolean caught = isMemberOf(entity);
        if (exclusive == caught) {
            filterCount++;
        }
        return exclusive == caught;        
    }

    private boolean isMemberOf ( AppdefEntityID entity ) {
        if (candidate.existsAppdefEntity( entity ) ) {
            return true;
        }
        return false;
    }
}
