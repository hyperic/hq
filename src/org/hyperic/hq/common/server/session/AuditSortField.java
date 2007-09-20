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

package org.hyperic.hq.common.server.session;

import java.util.ResourceBundle;

import org.hyperic.hibernate.SortField;
import org.hyperic.util.HypericEnum;

public abstract class AuditSortField 
    extends HypericEnum
    implements SortField
{
    private static final String BUNDLE = "org.hyperic.hq.common.Resources";
    
    public static final AuditSortField START_TIME = 
        new AuditSortField(0, "startTime", "audit.sortField.startTime") 
    {
        public boolean isSortable() {
            return true;
        }

        String getSortString(String audit, String resource, String subject) {
            return audit + ".startTime"; 
        }
    };
        
    public static final AuditSortField END_TIME = 
        new AuditSortField(1, "endTime", "audit.sortField.endTime") 
    {
        public boolean isSortable() {
            return true;
        }

        String getSortString(String audit, String resource, String subject) {
            return audit + ".endTime"; 
        }
    };

    public static final AuditSortField IMPORTANCE = 
        new AuditSortField(2, "importance", "audit.sortField.importance") 
    {
        public boolean isSortable() {
            return true;
        }
        
        String getSortString(String audit, String resource, String subject) {
            return audit + ".importanceEnum"; 
        }
    };

    public static final AuditSortField KLAZZ = 
        new AuditSortField(3, "klazz", "audit.sortField.klazz") 
    {
        public boolean isSortable() {
            return true;
        }

        String getSortString(String audit, String resource, String subject) {
            return audit + ".klazz"; 
        }
    };

    public static final AuditSortField PURPOSE = 
        new AuditSortField(4, "purpose", "audit.sortField.purpose") 
    {
        public boolean isSortable() {
            return true;
        }

        String getSortString(String audit, String resource, String subject) {
            return audit + ".purposeEnum"; 
        }
    };

    public static final AuditSortField RESOURCE = 
        new AuditSortField(5, "resource", "audit.sortField.resource") 
    {
        public boolean isSortable() {
            return true;
        }

        String getSortString(String audit, String resource, String subject) {
            return resource + ".sortName"; 
        }
    };

    public static final AuditSortField SUBJECT = 
        new AuditSortField(6, "subject", "audit.sortField.subject") 
    {
        public boolean isSortable() {
            return true;
        }

        String getSortString(String audit, String resource, String subject) {
            return subject + ".sortName"; 
        }
    };

    public static final AuditSortField DURATION = 
        new AuditSortField(7, "duration", "audit.sortField.duration") 
    {
        public boolean isSortable() {
            return true;
        }

        String getSortString(String audit, String resource, String subject) {
            return audit + ".endTime - " + audit + ".startTime"; 
        }
    };


    private AuditSortField(int code, String desc, String localeProp) {
        super(AuditSortField.class, code, desc, localeProp,
              ResourceBundle.getBundle(BUNDLE));
    }
    
    abstract String getSortString(String audit, String resource, 
                                  String subject);
    
    public static AuditSortField findByCode(int code) {
        return (AuditSortField)
            HypericEnum.findByCode(AuditSortField.class, code); 
    }
}
