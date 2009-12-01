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
import java.util.List;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.ServiceValue;

/** Pager Processor filter that filters object instances of
*   AppdefEntityID representing services based on whether 
*   or not the service has been assigned to service clusters
*   and/or applications. This is necessary since authz can't 
*   join on appdef tables :-(.
*
*/
public class AppdefPagerFilterAssignSvc implements AppdefPagerFilter {

    private List services;
    private boolean exclusive;
    private static final int UNDEFINED = -1;
    private int filterCount;

    public List getServices () { return services; }
    public int getFilterCount () { return filterCount; }
    public  boolean isExclusive () { return exclusive; }

    public AppdefPagerFilterAssignSvc ( List services ) {
        this.services     = services;
        this.exclusive    = true;
        filterCount       = 0;
    }

    public AppdefPagerFilterAssignSvc ( List services, boolean negate ) {
        this.services     = services;
        this.exclusive    = (! negate);
        filterCount       = 0;
    }

   /** Evaluate an object against the filter.
    * @param o - object instance of AppdefEntityID
    * @return flag - true if caught (unless negated)  */
    public boolean isCaught ( Object o ) {
      AppdefEntityID entity;

      if (!(o instanceof AppdefEntityID)) {
          throw new IllegalArgumentException("Expecting instance of "+
                                             "AppdefEntityID");
      }

      try {
          entity = (AppdefEntityID) o;

          boolean caught = isInSet(entity);
          if (exclusive == caught) {
              filterCount++;
          }
          return exclusive == caught;

      } catch (Exception e) {
          // In a paging context, we swallow all exceptions.
          return exclusive == false;
      }
    }

    private boolean isInSet ( AppdefEntityID id ) {
        if (services != null) {
            for (Iterator i=services.iterator();i.hasNext();) {
               AppdefEntityID sid = ((ServiceValue)i.next()).getEntityId();
               if (sid.equals(id)){
                   return true;
               }
            }
        }
        return false;
    }
}
