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

import java.util.Comparator;
import java.util.ResourceBundle;

import org.hyperic.hibernate.SortField;
import org.hyperic.util.HypericEnum;

public abstract class DownResSortField
    extends HypericEnum implements SortField {

    private static final String BUNDLE = "org.hyperic.hq.appdef.Resources";
    
    public static final DownResSortField RESOURCE = 
        new DownResSortField(0, "resource", "down.sortField.resource") 
    {
        public boolean isSortable() {
            return true;
        }

        public Comparator getComparator() {
            return new Comparator() {

                public int compare(Object arg0, Object arg1) {
                    // Have to be DownResource objects
                    DownResource dr1 = (DownResource) arg0;
                    DownResource dr2 = (DownResource) arg1;
                    
                    return dr1.getName().compareTo(dr2.getName());
                }
                
            };
        }
    };
    
    public static final DownResSortField TYPE = 
        new DownResSortField(1, "type", "down.sortField.type") 
    {
        public boolean isSortable() {
            return true;
        }

        public Comparator getComparator() {
            return new Comparator() {

                public int compare(Object arg0, Object arg1) {
                    // Have to be DownResource objects
                    DownResource dr1 = (DownResource) arg0;
                    DownResource dr2 = (DownResource) arg1;
                    
                    if (dr1.getType().equals(dr2.getType()))
                        return -1;
                    
                    return dr1.getType().compareTo(dr2.getType());
                }
                
            };
        }
    };
    
    public static final DownResSortField SINCE = 
        new DownResSortField(2, "since", "down.sortField.since") 
    {
        public boolean isSortable() {
            return true;
        }

        public Comparator getComparator() {
            return new Comparator() {

                public int compare(Object arg0, Object arg1) {
                    // Have to be DownResource objects
                    DownResource dr1 = (DownResource) arg0;
                    DownResource dr2 = (DownResource) arg1;
                    
                    if (dr1.getDuration() == dr2.getDuration())
                        return -1;
                    
                    return Long.valueOf(dr2.getDuration())
                        .compareTo(Long.valueOf(dr1.getDuration()));
                }
                
            };
        }
    };
    
    public static final DownResSortField DOWNTIME = 
        new DownResSortField(3, "downtime", "down.sortField.downtime") 
    {
        public boolean isSortable() {
            return true;
        }

        public Comparator getComparator() {
            return new Comparator() {

                public int compare(Object arg0, Object arg1) {
                    // Have to be DownResource objects
                    DownResource dr1 = (DownResource) arg0;
                    DownResource dr2 = (DownResource) arg1;
                    
                    if (dr1.getDuration() == dr2.getDuration())
                        return -1;
                    
                    return Long.valueOf(dr2.getDuration())
                        .compareTo(Long.valueOf(dr1.getDuration()));
                }
                
            };
        }
    };
    
    private DownResSortField(int code, String desc, String localeProp) {
        super(DownResSortField.class, code, desc, localeProp,
              ResourceBundle.getBundle(BUNDLE));
    }
    
    public abstract Comparator getComparator();
}
