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

import java.util.HashMap;
import java.util.Map;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;

/** Pager Processor filter that filters object instances of
*   AppdefEntityID based on whether or not the service it represents
*   is an internal service.
*/
public class AppdefPagerFilterInternalService implements AppdefPagerFilter {


    private AuthzSubjectValue subject;
    private boolean exclusive;
    private Map fetchedEntityCache;
    private int filterCount;

    public AuthzSubjectValue getSubject() { return subject; }
    public int getFilterCount () { return filterCount; }
    public boolean isExclusive () { return exclusive; }

    public AppdefPagerFilterInternalService ( AuthzSubjectValue subject) {
        this.subject      = subject;
        this.exclusive    = true;
        fetchedEntityCache = new HashMap();
        filterCount       = 0;
    }

    public AppdefPagerFilterInternalService ( AuthzSubjectValue subject,
        boolean negate ) {
        this.subject      = subject;
        this.exclusive    = (! negate);
        fetchedEntityCache = new HashMap();
        filterCount       = 0;
    }

   /** Evaluate an object against the filter.
    * @param o - object instance of AppdefEntityID
    * @return flag - true if caught (unless negated)  */
    public boolean isCaught ( Object o ) {
      AppdefResourceValue arv;
      AppdefEntityID entity;

      if (!(o instanceof AppdefEntityID)) {
          throw new IllegalArgumentException("Expecting instance of "+
                                             "AppdefEntityID");
      }

      try {
          entity = (AppdefEntityID) o;
          arv = fetchEntityById(entity);
          fetchedEntityCache.put(entity,arv);

          boolean caught = isInternal(arv);
          if (exclusive == caught) {
              filterCount++;
          }
          return exclusive == caught;

      } catch (Exception e) {
          // In a paging context, we swallow all exceptions.
          return exclusive == false;
      }
    }

    private boolean isInternal ( AppdefResourceValue vo ) {
        Object o = vo.getAppdefResourceTypeValue();
        if (o instanceof ServiceTypeValue) {
            if ( ((ServiceTypeValue)o).getIsInternal() ) {
                return true;
            }
        }
        return false;
    }

    // DB fetch the resource value
    private AppdefResourceValue fetchEntityById (AppdefEntityID id)
        throws Exception {
        AppdefEntityValue aev = new AppdefEntityValue (id,this.subject);
        return aev.getResourceValue();
    }

}
