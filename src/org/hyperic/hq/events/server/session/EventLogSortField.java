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

public abstract class EventLogSortField
    extends HypericEnum
    implements SortField
{
    private static final String BUNDLE = "org.hyperic.hq.events.Resources";
    
    public static final EventLogSortField RESOURCE = 
        new EventLogSortField(0, "Resource", "eventLog.sortField.resource")  
    {
        public boolean isSortable() {
            return true;
        }

        String getSortString(String resource, String eventLog) {
            return resource + ".name";
        }
    };
    
    public static final EventLogSortField DATE =  
        new EventLogSortField(1, "Date", "eventLog.sortField.date")  
    {
        public boolean isSortable() {
            return true;
        }

        String getSortString(String resource, String eventLog) {
            return eventLog + ".timestamp";
        }
    };
    
    public static final EventLogSortField STATUS =  
        new EventLogSortField(2, "Status", "eventLog.sortField.status")  
    {
        public boolean isSortable() {
            return true;
        }

        String getSortString(String resource, String eventLog) {
            return eventLog + ".status";
        }
    };

    public static final EventLogSortField SUBJECT =  
        new EventLogSortField(3, "Subject", "eventLog.sortField.subject")  
    {
        public boolean isSortable() {
            return true;
        }

        String getSortString(String resource, String eventLog) {
            return eventLog + ".subject";
        }
    };

    public static final EventLogSortField TYPE =  
        new EventLogSortField(4, "Type", "eventLog.sortField.type")  
    {
        public boolean isSortable() {
            return true;
        }

        String getSortString(String resource, String eventLog) {
            return eventLog + ".type";
        }
    };

    public static final EventLogSortField DETAIL =  
        new EventLogSortField(5, "Detail", "eventLog.sortField.detail")  
    {
        public boolean isSortable() {
            return true;
        }

        String getSortString(String resource, String eventLog) {
            return eventLog + ".detail";
        }
    };

    
    private EventLogSortField(int code, String desc, String localeProp) {
        super(EventLogSortField.class, code, desc, localeProp,
              ResourceBundle.getBundle(BUNDLE));
    }
    
    abstract String getSortString(String resource, String eventLog);
    
    public static EventLogSortField findByCode(int code) {
        return (EventLogSortField)
            HypericEnum.findByCode(EventLogSortField.class, code); 
    }
}
