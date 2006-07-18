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

package org.hyperic.hq.ui.beans;

import java.io.Serializable;
import org.hyperic.hq.appdef.shared.AppdefEntityID;

/**
 * Bean to hold &lt;area&gt; information for an image map.
 *
 */
public class ImageMapRectAreaBean implements Serializable {
    private static final int NO_CTYPE = -1;
    private static final String NO_AUTOGROUPTYPE = "NO_AUTOGROUPTYPE";
    private int x1;
    private int y1;
    private int x2;
    private int y2;
    private AppdefEntityID[] entityIds;
    private int ctype = NO_CTYPE;
    private String autogrouptype = NO_AUTOGROUPTYPE;
    private String alt;

    public ImageMapRectAreaBean() {
    }

    public ImageMapRectAreaBean(int x1, int y1, int x2, int y2,
                                AppdefEntityID[] entityIds,
                                String alt) {
        this(x1, y1, x2, y2, entityIds, NO_CTYPE, alt);
    }

    public ImageMapRectAreaBean(int x1, int y1, int x2, int y2,
                                AppdefEntityID[] entityIds,
                                int ctype, String alt) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.entityIds = entityIds;
        this.ctype = ctype;
        this.alt = alt;
    }

    public ImageMapRectAreaBean(int x1, int y1, int x2, int y2,
                                AppdefEntityID[] entityIds,
                                int ctype, String alt, String autogrouptype) {
                                    
        this(x1, y1, x2, y2, entityIds, ctype, alt);
        
        this.autogrouptype = autogrouptype;
    }

    public int getX1() {
        return x1;
    }

    public void setX1(int x1) {
        this.x1 = x1;
    }

    public int getY1() {
        return y1;
    }

    public void setY1(int y1) {
        this.y1 = y1;
    }

    public int getX2() {
        return x2;
    }

    public void setX2(int x2) {
        this.x2 = x2;
    }

    public int getY2() {
        return y2;
    }

    public void setY2(int y2) {
        this.y2 = y2;
    }

    public AppdefEntityID[] getEntityIds() {
        return entityIds;
    }

    public void setEntityIds(AppdefEntityID[] entityIds) {
        this.entityIds = entityIds;
    }

    public int getCtype() {
        return ctype;
    }

    public void setCtype(int ctype) {
        this.ctype = ctype;
    }

    public boolean getHasCtype() {
        return (NO_CTYPE != ctype);
    }

    public String getAlt() {
        return alt;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }
    
    public String getAutogrouptype() {
        return autogrouptype;
    }
    public void setAutogrouptype(String i) {
        autogrouptype = i;
    }

    public boolean getHasAutogrouptype() {
        return (!NO_AUTOGROUPTYPE.equals(autogrouptype));
    }

}

// EOF
