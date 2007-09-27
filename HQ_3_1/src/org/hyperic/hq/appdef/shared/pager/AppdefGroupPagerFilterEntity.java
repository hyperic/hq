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

/** Pager Processor filter that filters appdef entities based on
 *  their type.
 */
public class AppdefGroupPagerFilterEntity implements AppdefPagerFilter {

    private boolean exclusive;         // true=exclusive|false=inclusive    
    private int     filterCount;
    private int     entityTypeId;
    public  int     getEntityTypeId () { return entityTypeId; }
    public  int     getFilterCount ()  { return filterCount; }
    public  boolean isExclusive ()     { return exclusive; }

    public AppdefGroupPagerFilterEntity ( int entityTypeId ) { 
      this.entityTypeId = entityTypeId;
      this.exclusive    = true;
    }
    public AppdefGroupPagerFilterEntity ( int entityTypeId, boolean negate) { 
      this.entityTypeId = entityTypeId;
      this.exclusive    = (! negate);
    }

   /** Evaluate an object against the filter.
    * @param object instance of either AppdefGroupValue or AppdefEntityID
    * @return flag - true if caught (unless negated)  */
    public boolean isCaught ( Object id ) {

      if (!(id instanceof AppdefEntityID))
        throw new IllegalArgumentException("Invalid appdef entity id");

      AppdefEntityID entityId = (AppdefEntityID) id;

      if (entityTypeId == -1) 
        throw new RuntimeException ("Entity entity type must be set.");

      return (exclusive) ?  entityTypeIdMatches(entityId) : 
                           !entityTypeIdMatches(entityId) ;
    }

    /* Test to see if entity id matches our instance value. */
    protected boolean entityTypeIdMatches ( AppdefEntityID id ) {
      return (id.getType() == getEntityTypeId()) ? true : false ;
    }

}

