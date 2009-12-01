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

package org.hyperic.hq.ui.util.minitab;

/**
 * A simple bean that represents a minitab in the monitor
 * subsection.
 */
public class MiniTab {
    
    //-------------------------------------instance variables

    private String key;
    private String mode;
    private String name;
    private String param;
    private Boolean selected;
    
    //-------------------------------------constructors

    public MiniTab() {
        super();
    }

    //-------------------------------------public methods

    public String getKey() {
        return key;
    }

    public void setKey(String s) {
        key = s;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String s) {
        mode = s;
    }

    public String getName() {
        return name;
    }

    public void setName(String s) {
        name = s;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String s) {
        param = s;
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean b) {
        selected = b;
    }

    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("key=").append(key);
        b.append(" mode=").append(mode);
        b.append(" name=").append(name);
        b.append(" param=").append(param);
        b.append(" selected=").append(selected);
        return b.toString();
    }
}
