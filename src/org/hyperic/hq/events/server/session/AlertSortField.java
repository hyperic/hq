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

import org.hyperic.hibernate.SortField;
import org.hyperic.util.HypericEnum;

public abstract class AlertSortField 
    extends HypericEnum
    implements SortField
{
    public static final AlertSortField DATE = 
        new AlertSortField(0, "Date") 
    {
        String getSortString(String alert, String def, String resource) {
            return alert + ".ctime";
        }
    };
        
    public static final AlertSortField DEFINITION = 
        new AlertSortField(1, "Definition") 
    {
        String getSortString(String alert, String def, String resource) {
            return def + ".name";
        }
    };
    
    public static final AlertSortField RESOURCE = 
        new AlertSortField(2, "Resource")
    {
        String getSortString(String alert, String def, String resource) {
            return resource + ".name";
        }
    };
    
    public static final AlertSortField FIXED = 
        new AlertSortField(3, "Fixed")
    {
        String getSortString(String alert, String def, String resource) {
            return alert + ".fixed";
        }
    };
    
    public static final AlertSortField ACKED_BY = 
        new AlertSortField(4, "AckedBy") 
    {
        String getSortString(String alert, String def, String resource) {
            return alert + ".ackedBy";
        }
    };
    
    public static final AlertSortField SEVERITY = 
        new AlertSortField(5, "Severity")
    {
        String getSortString(String alert, String def, String resource) {
            return def + ".priority";
        }
    };

    private AlertSortField(int code, String desc) {
        super(AlertSortField.class, code, desc);
    }
    
    /**
     * Returns HQL which can be used to tack onto an HQL query, to sort
     */
    abstract String getSortString(String alert, String def, String resource);
}
