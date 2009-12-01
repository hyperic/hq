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

/** Pager Processor filter employed during paging of AppdefEntityIDs
 *  and compares each one to a default set of AppdefEntityIDs -
 *  ("the exclusionary set").
 */
public class AppdefPagerFilterExclude implements AppdefPagerFilter {

    private AppdefEntityID[] exclusionarySet;
    private boolean exclusive;
    private int filterCount;

    public AppdefEntityID[]  getExclusionarySet () { return exclusionarySet; }
    public int getFilterCount () { return filterCount; }
    public boolean isExclusive () { return exclusive; }

    public AppdefPagerFilterExclude  ( AppdefEntityID[] excludes ) {
        this.exclusionarySet  = excludes;
        this.exclusive = true;
    }

    public AppdefPagerFilterExclude  ( AppdefEntityID[] excludes, boolean negate ) {
        this.exclusionarySet = excludes;
        this.exclusive = (! negate);
    }

   /** Evaluate an object against this filter.
    * @param o - object instance of AppdefEntityID
    * @return flag - true if caught (unless negated)  
    */
    public boolean isCaught ( Object o ) {

        if (! (o instanceof AppdefEntityID))  {
            throw new IllegalArgumentException("Argument must be a valid "+
                                              "instance of AppdefEntityID.");
        }

        AppdefEntityID entity = (AppdefEntityID) o;

        return exclusive == isInSet (entity);
    }

    private boolean isInSet ( AppdefEntityID entity ) {

        for (int i=0;i<exclusionarySet.length;i++) {
            if ( exclusionarySet[i].equals(entity) ) {
                return true;
            }
        }
        return false;
    }
}
