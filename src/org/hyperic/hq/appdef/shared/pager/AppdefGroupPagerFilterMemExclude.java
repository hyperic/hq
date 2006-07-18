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
*   AppdefGroupValue based on the membership or non-membership
*   of a particular AppdefEntity.
*/
public class AppdefGroupPagerFilterMemExclude implements AppdefPagerFilter {

    private AppdefEntityID candidate; 
    private boolean        exclusive;
    private int            filterCount;

    public AppdefEntityID  getCandidate () { return candidate; }
    public int getFilterCount () { return filterCount; }
    public  boolean isExclusive () { return exclusive; }

    public AppdefGroupPagerFilterMemExclude  ( AppdefEntityID candidate ) {
        this.candidate = candidate;
        this.exclusive = true;
        this.filterCount = 0;
    }
    public AppdefGroupPagerFilterMemExclude  ( AppdefEntityID candidate,
        boolean negate ) {
        this.candidate = candidate;
        this.exclusive = (! negate);
        this.filterCount = 0;
    }

   /** Evaluate an object against the filter.
    * @param object instance of AppdefGroupValue
    * @return flag - true if caught (unless negated)  */
    public boolean isCaught ( Object id ) {

        if (!(id instanceof AppdefGroupValue)) {
            throw new IllegalArgumentException("Invalid appdef entity id");
        }

        AppdefGroupValue gVo = (AppdefGroupValue) id;
      
        boolean caught = isMemberOf (gVo);

        if (exclusive == caught) {
            filterCount++;
        }
        return (exclusive == caught);
    }

    protected boolean isMemberOf ( AppdefGroupValue groupVo ) {
        try {
            if (groupVo.existsAppdefEntity( candidate ) ) {
                return true;
            }
        } catch (Exception e) {
          // this shouldn't happen in a paging context! xxx - log
        }
        return false;
    }
}
