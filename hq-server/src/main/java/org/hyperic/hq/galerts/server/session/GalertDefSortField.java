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
        public String getSortString() {
            return "creationTime";
        }

        public boolean isSortable() {
            return true;
        }
    };
        
    public static final GalertDefSortField MTIME = 
        new GalertDefSortField(1, "MTime", "galertDef.sortField.mtime") 
    {
        public String getSortString() {
            return "modifiedTime";
        }

        public boolean isSortable() {
            return true;
        }
    };
   
    
    public static final GalertDefSortField SEVERITY = 
        new GalertDefSortField(4, "Severity", "galertDef.sortField.severity")
    {
        public String getSortString() {
            return "severity";
        }

        public boolean isSortable() {
            return true;
        }
    };

 

    public static final GalertDefSortField ESCALATION = 
        new GalertDefSortField(6, "Escalation", "galertDef.sortField.escalation")
    {
        public String getSortString() {
            return "escalation.name";
        }

        public boolean isSortable() {
            return true;
        }
    };

    public static final GalertDefSortField LAST_FIRED = 
        new GalertDefSortField(7, "LastFired", "galertDef.sortField.lastFired")
    {
        public String getSortString() {
            return "lastFired";
        }

        public boolean isSortable() {
            return true;
        }
    };

    private GalertDefSortField(int code, String desc, String localeProp) {
        super(GalertDefSortField.class, code, desc, localeProp,
              ResourceBundle.getBundle(BUNDLE));
    }
    
    
    public static GalertDefSortField findByCode(int code) {
        return (GalertDefSortField)
            HypericEnum.findByCode(GalertDefSortField.class, code); 
    }
}
