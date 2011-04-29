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

package org.hyperic.hq.events.server.session;

import java.util.ResourceBundle;

import org.hyperic.hibernate.SortField;
import org.hyperic.util.HypericEnum;

public abstract class AlertSortField 
    extends HypericEnum
    implements SortField
{
    private static final String BUNDLE = "org.hyperic.hq.events.Resources";
    
    public static final AlertSortField DATE = 
        new AlertSortField(0, "Date", "alert.sortField.date") 
    {
        public String getSortString() {
            return "ctime";
        }

        public boolean isSortable() {
            return true;
        }
    };
        
    public static final AlertSortField DEFINITION = 
        new AlertSortField(1, "Definition", "alert.sortField.def") 
    {
        public String getSortString() {
            return "alertDefinition.name";
        }

        public boolean isSortable() {
            return true;
        }
    };
    
 
    
    public static final AlertSortField FIXED = 
        new AlertSortField(3, "Fixed", "alert.sortField.fixed")
    {
        public String getSortString() {
            return "fixed";
        }

        public boolean isSortable() {
            return true;
        }
    };
    
  
    
    public static final AlertSortField SEVERITY = 
        new AlertSortField(5, "Severity", "alert.sortField.severity")
    {
        public String getSortString() {
            return "alertDefinition.priority";
        }

        public boolean isSortable() {
            return true;
        }
    };

 
    private AlertSortField(int code, String desc, String localeProp) {
        super(AlertSortField.class, code, desc, localeProp,
              ResourceBundle.getBundle(BUNDLE));
    }
    
    public static AlertSortField findByCode(int code) {
        return (AlertSortField)HypericEnum.findByCode(AlertSortField.class, 
                                                      code);
    }
}
