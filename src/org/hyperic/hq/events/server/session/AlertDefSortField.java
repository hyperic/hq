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

public abstract class AlertDefSortField 
    extends HypericEnum
    implements SortField
{
    private static final String BUNDLE = "org.hyperic.hq.events.Resources";
    
    public static final AlertDefSortField NAME = 
        new AlertDefSortField(0, "Name", "alertDef.sortField.name") 
    {
        public boolean isSortable() {
            return true;
        }

        String getSortString(String def, String resource) {
            return def + ".name";
        }
    };

    public static final AlertDefSortField CTIME = 
        new AlertDefSortField(1, "Create Time", "alertDef.sortField.ctime") 
    {
        public boolean isSortable() {
            return true;
        }

        String getSortString(String def, String resource) {
            return def + ".ctime";
        }
    };

    public static final AlertDefSortField MTIME = 
        new AlertDefSortField(2, "Modify Time", "alertDef.sortField.mtime") 
    {
        public boolean isSortable() {
            return true;
        }
        
        String getSortString(String def, String resource) {
            return def + ".mtime";
        }
    };

    public static final AlertDefSortField PRIORITY = 
        new AlertDefSortField(3, "Priority", "alertDef.sortField.priority") 
    {
        public boolean isSortable() {
            return true;
        }
        
        String getSortString(String def, String resource) {
            return def + ".priority";
        }
    };

    public static final AlertDefSortField RESOURCE = 
        new AlertDefSortField(4, "Resource", "alertDef.sortField.resource") 
    {
        public boolean isSortable() {
            return true;
        }
        
        String getSortString(String def, String resource) {
            return resource + ".name";
        }
    };

    public static final AlertDefSortField ACTIVE = 
        new AlertDefSortField(5, "Active", "alertDef.sortField.active") 
    {
        public boolean isSortable() {
            return true;
        }
        
        String getSortString(String def, String resource) {
            return def + ".active";
        }
    };

    public static final AlertDefSortField LAST_FIRED = 
        new AlertDefSortField(6, "Last Fired", "alertDef.sortField.lastFired") 
    {
        public boolean isSortable() {
            return true;
        }
        
        String getSortString(String def, String resource) {
            return def + ".alertDefinitionState.lastFired";
        }
    };

    public static final AlertDefSortField ESCALATION  = 
        new AlertDefSortField(7, "Escalation", "alertDef.sortField.escalation") 
    {
        public boolean isSortable() {
            return false;
        }
        
        String getSortString(String def, String resource) {
            return def + ".escalation.name";
        }
    };

    private AlertDefSortField(int code, String desc, String localeProp) {
        super(AlertDefSortField.class, code, desc, localeProp,
              ResourceBundle.getBundle(BUNDLE));
    }
    
    abstract String getSortString(String def, String resource);
    
    public static AlertDefSortField findByCode(int code) {
        return (AlertDefSortField)
            HypericEnum.findByCode(AlertDefSortField.class, code); 
    }
}
