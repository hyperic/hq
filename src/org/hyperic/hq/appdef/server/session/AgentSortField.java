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

package org.hyperic.hq.appdef.server.session;

import java.util.ResourceBundle;

import org.hyperic.hibernate.SortField;
import org.hyperic.util.HypericEnum;

public abstract class AgentSortField 
    extends HypericEnum
    implements SortField
{
    private static final String BUNDLE = "org.hyperic.hq.appdef.Resources";
    
    public static final AgentSortField ADDR = 
        new AgentSortField(0, "address", "agent.sortField.addr") 
    {
        public boolean isSortable() {
            return true;
        }

        public String getSortString(String agent) {
            return agent + ".address";
        }
    };
    
    public static final AgentSortField PORT = 
        new AgentSortField(1, "port", "agent.sortField.port") 
    {
        public boolean isSortable() {
            return true;
        }

        public String getSortString(String agent) {
            return agent + ".port";
        }
    };
    
    public static final AgentSortField VERSION = 
        new AgentSortField(2, "version", "agent.sortField.version") 
    {
        public boolean isSortable() {
            return true;
        }

        public String getSortString(String agent) {
            return agent + ".version";
        }
    };

    public static final AgentSortField CTIME = 
        new AgentSortField(3, "ctime", "agent.sortField.ctime") 
    {
        public boolean isSortable() {
            return true;
        }

        public String getSortString(String agent) {
            return agent + ".creationTime";
        }
    };
    
    private AgentSortField(int code, String desc, String localeProp) {
        super(AgentSortField.class, code, desc, localeProp,
              ResourceBundle.getBundle(BUNDLE));
    }
    
    public abstract String getSortString(String agent);
    
    public static AgentSortField findByCode(int code) {
        return (AgentSortField)
            HypericEnum.findByCode(AgentSortField.class, code); 
    }
    
    public static AgentSortField findByDescription(String desc){
        return (AgentSortField)
            HypericEnum.findByDescription(AgentSortField.class, desc);
    }
}
