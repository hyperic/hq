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

package org.hyperic.hq.control.shared;

import java.io.Serializable;

/**
 * Simple data object for returning data for the Dashboard
 */
public class ControlFrequencyValue implements Serializable {

    private String name;
    private String action;
    private int    id;
    private int    type;
    private int    num;

    public ControlFrequencyValue(String name, int type, int id,
                                 String action, int num) {
        this.name = name;
        this.action = action;
        this.type = type;
        this.id = id;
        this.num = num;
    }

    // Getters
    public String getName() {
        return this.name;
    }

    public String getAction() {
        return this.action;
    }

    public int getNum() {
        return this.num;
    }

    public int getId() {
        return this.id;
    }

    public int getType() {
        return this.type;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setNum(int num) {
        this.num = num;
    }
}

    
