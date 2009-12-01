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

import java.util.List;

/**
 * This class has one purpose in life:
 * To provide a collection of MiniTab's for the UI to display in such
 * a way that the colspan for the minitab table doesn't break when the
 * number of minitab elements varies
 */
public class MiniTabs {

    private static final int COLSPAN_OFFSET = 2;
    private List minitabs;
    private int offset;
    private boolean offsetIsSet = false;

    public MiniTabs(List minitabs) {
        this.minitabs = minitabs;
    }
    
    public Integer getColspan() {
        return new Integer(minitabs.size() + COLSPAN_OFFSET);
    }

    /**
     * @return List
     */
    public List getList() {
        return minitabs;
    }

    /**
     * Sets the minitabs.
     * @param minitabs The minitabs to set
     */
    public void setList(List minitabs) {
        this.minitabs = minitabs;
    }

    /**
     * @return int
     */
    public int getOffset() {
        if (! offsetIsSet)
            return COLSPAN_OFFSET;
        return offset;
    }

    /**
     * Sets the offset.
     * @param offset The offset to set
     */
    public void setOffset(int offset) {
        offsetIsSet = true;
        this.offset = offset;
    }

}
