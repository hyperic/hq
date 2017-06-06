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

package org.hyperic.hq.ui.util;

import org.apache.tiles.beans.SimpleMenuItem;

/**
 * A simple bean that extends <code>MenuItem</code> to supply image
 * dimensions for a tab button.
 */
public class Tab extends SimpleMenuItem {
    
    //-------------------------------------instance variables
    
    private Integer height;
    private String mode;
    private Integer width;
    private Boolean visible;
    
    //-------------------------------------constructors

    public Tab() {
        super();
    }

    //-------------------------------------public methods

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer i) {
        height = i;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String i) {
        mode = i;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer i) {
        width = i;
    }

    public Boolean getVisible() {
        // default to true
        if (visible == null) {
            visible = Boolean.TRUE;
        }
        return visible;
    }

    public void setVisible(Boolean visibleFlag) {
        visible = visibleFlag;
    }
}
