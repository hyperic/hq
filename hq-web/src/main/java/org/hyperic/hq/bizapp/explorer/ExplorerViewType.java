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
package org.hyperic.hq.bizapp.explorer;

import java.util.ResourceBundle;

import org.hyperic.util.HypericEnum;

/**
 * An ExplorerViewType is a way to get information about how to display
 * content on the right-hand side of an Explorer. 
 */
public class ExplorerViewType extends HypericEnum {
    private static final ResourceBundle BUNDLE =
        ResourceBundle.getBundle("org.hyperic.hq.bizapp.Resources");
    
    public static final ExplorerViewType HQU_GROUP_VIEW =
        new ExplorerViewType(1, "hquGroupView",
                             "explorer.viewType.hquGroupView");

    protected ExplorerViewType(int code, String desc, String localeProp) {
        super(ExplorerViewType.class, code, desc, localeProp, BUNDLE);
    }

    public static ExplorerViewType findByCode(int code) {
        return (ExplorerViewType)findByCode(ExplorerViewType.class, code);  
    }
}
