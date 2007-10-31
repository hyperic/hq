/*                                                                 
 * NOTE: This copyright does *not* cover user programs that use HQ 
 * program services by normal system calls through the application 
 * program interfaces provided as part of the Hyperic Plug-in Development 
 * Kit or the Hyperic Client Development Kit - this is merely considered 
 * normal use of the program, and does *not* fall under the heading of 
 * "derived work". 
 *  
 * Copyright (C) [2004-2007], Hyperic, Inc. 
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

package org.hyperic.hq.appdef.server.session;

import java.util.ResourceBundle;

import org.hyperic.hibernate.SortField;
import org.hyperic.util.HypericEnum;

public abstract class CPropResourceSortField
    extends HypericEnum 
    implements SortField 
{
    private static final String BUNDLE = "org.hyperic.hq.appdef.Resources";
    
    public static final CPropResourceSortField PROPERTY = 
        new CPropResourceSortField(0, "property", 
                                   "cpropResource.sortField.property") 
    {
        public boolean isSortable() {
            return true;
        }
    };

    public static final CPropResourceSortField RESOURCE = 
        new CPropResourceSortField(1, "resource", 
                                   "cpropResource.sortField.resource") 
    {
        public boolean isSortable() {
            return true;
        }
    };
    
    public static final CPropResourceSortField METRIC_VALUE = 
        new CPropResourceSortField(2, "metricValue", 
                                   "cpropResource.sortField.metricValue") 
    {
        public boolean isSortable() {
            return true;
        }
    };

    public static final CPropResourceSortField METRIC_TIMESTAMP = 
        new CPropResourceSortField(3, "metricTimestamp", 
                                   "cpropResource.sortField.metricTimestamp") 
    {
        public boolean isSortable() {
            return true;
        }
    };

    public static final CPropResourceSortField EVENT_LOG = 
        new CPropResourceSortField(5, "eventLog", 
                                   "cpropResource.sortField.eventLog") 
    {
        public boolean isSortable() {
            return false;
        }
    };

    private CPropResourceSortField(int code, String desc, String localeProp) {
        super(CPropResourceSortField.class, code, desc, localeProp,
              ResourceBundle.getBundle(BUNDLE));
    }
}
