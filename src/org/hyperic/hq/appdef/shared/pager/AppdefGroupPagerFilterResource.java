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

import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;

/** Pager Processor filter that filters an appdef group val based on
 *  its resource type. 
 */
public class AppdefGroupPagerFilterResource implements AppdefPagerFilter {

    private boolean exclusive = true;
    private AppdefResourceTypeValue entityResType;
    private int filterCount;

    public int getFilterCount () { return filterCount; }
    public  boolean isExclusive () { return exclusive; }
    public AppdefResourceTypeValue  getEntityResType () {
        return entityResType;
    }

    public AppdefGroupPagerFilterResource  ( AppdefResourceTypeValue
        entityResType ) {
      this.entityResType = entityResType;
      this.exclusive       = true;
    }
    public AppdefGroupPagerFilterResource  ( AppdefResourceTypeValue
        entityResType, boolean negate ) {
      this.entityResType = entityResType;
      this.exclusive       = (! negate);
    }

   /** Evaluate an object against the filter.
    * @param object instance of AppdefGroupValue
    * @return flag - true if caught (unless negated)*/
    public boolean isCaught ( Object o ) {

      if (o==null || !(o instanceof AppdefGroupValue))
        throw new IllegalArgumentException("Invalid appdef group value");

      AppdefGroupValue groupVo = (AppdefGroupValue) o;

      return (exclusive) ? resourceTypeMatches(groupVo) :
                          !resourceTypeMatches(groupVo) ;
    }

    /* Test to see if entity's resourceTypeId matches our instance value. */
    protected boolean resourceTypeMatches ( AppdefGroupValue groupVo ) {
      boolean retVal = false;
      try {
        if ( groupVo.getAppdefResourceTypeValue().getAppdefType() ==
             entityResType.getAppdefType() &&
             groupVo.getAppdefResourceTypeValue().getId() ==
             entityResType.getId())
          return true;
      }
      catch (Exception e) {
        // shouldn't happen in paging context. xxx - log
        e.printStackTrace();
      }
      return false;
    }
}

