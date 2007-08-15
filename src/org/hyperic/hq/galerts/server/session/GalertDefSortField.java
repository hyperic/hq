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

package org.hyperic.hq.galerts.server.session;

import java.util.ResourceBundle;

import org.hyperic.hibernate.SortField;
import org.hyperic.util.HypericEnum;

public abstract class GalertDefSortField 
    extends HypericEnum
    implements SortField
{
    private static final String BUNDLE = "org.hyperic.hq.galerts.Resources";
    
    public static final GalertDefSortField CTIME = 
        new GalertDefSortField(0, "CTime", "galertDef.sortField.ctime") 
    {
        String getSortString(String def, String group, String escalation) {
            return def + ".ctime";
        }

        public boolean isSortable() {
            return true;
        }
    };
        
    public static final GalertDefSortField MTIME = 
        new GalertDefSortField(1, "MTime", "galertDef.sortField.mtime") 
    {
        String getSortString(String def, String group, String escalation) {
            return def + ".mtime";
        }

        public boolean isSortable() {
            return true;
        }
    };
    
    public static final GalertDefSortField NAME = 
        new GalertDefSortField(2, "Name", "galertDef.sortField.name")
    {
        String getSortString(String def, String group, String escalation) {
            return group + ".name";
        }

        public boolean isSortable() {
            return true;
        }
    };
    
    public static final GalertDefSortField ENABLED = 
        new GalertDefSortField(3, "Enabled", "galertDef.sortField.enabled")
    {
        String getSortString(String def, String group, String escalation) {
            return def + ".enabled";
        }

        public boolean isSortable() {
            return true;
        }
    };
    
    public static final GalertDefSortField SEVERITY = 
        new GalertDefSortField(4, "Severity", "galertDef.sortField.severity")
    {
        String getSortString(String def, String group, String escalation) {
            return def + ".severityEnum";
        }

        public boolean isSortable() {
            return true;
        }
    };

    public static final GalertDefSortField GROUP = 
        new GalertDefSortField(5, "Group", "galertDef.sortField.group")
    {
        String getSortString(String def, String group, String escalation) {
            return group + ".name";
        }

        public boolean isSortable() {
            return true;
        }
    };

    public static final GalertDefSortField ESCALATION = 
        new GalertDefSortField(6, "Escalation", "galertDef.sortField.escalation")
    {
        String getSortString(String def, String group, String escalation) {
            return escalation + ".name";
        }

        public boolean isSortable() {
            return true;
        }
    };

    private GalertDefSortField(int code, String desc, String localeProp) {
        super(GalertDefSortField.class, code, desc, localeProp,
              ResourceBundle.getBundle(BUNDLE));
    }
    
    /**
     * Returns HQL which can be used to tack onto an HQL query, to sort
     */
    abstract String getSortString(String def, String group, String escalation);
    
    public static GalertDefSortField findByCode(int code) {
        return (GalertDefSortField)
            HypericEnum.findByCode(GalertDefSortField.class, code); 
    }
}
