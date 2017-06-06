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

import java.io.Serializable;

/**
 * A simple JavaBean to encapsulate the request parameters sent for an HTML
 * input element of type image. Such an element causes two parameters to be
 * sent, one each for the X and Y coordinates of the button press. An instance
 * of this bean within an <code>ActionForm</code> can be used to capture these
 * and provide a simple means of detecting whether or not the corresponding
 * image was selected.
 *
 * @version $Rev: 471754 $ $Date: 2005-05-07 12:11:38 -0400 (Sat, 07 May 2005)
 *          $
 */
public class ImageButtonBean implements Serializable {
    // ------------------------------------------------------------- Properties

    /**
     * The X coordinate of the button press.
     */
    private String x;

    /**
     * The Y coordinate of the button press.
     */
    private String y;

    // ----------------------------------------------------------- Constructors

    /**
     * Construct an instance with empty property values.
     */
    public ImageButtonBean() {
        ; // do nothing
    }

    /**
     * Construct an instance with the supplied property values.
     *
     * @param x The X coordinate of the button press.
     * @param y The Y coordinate of the button press.
     */
    public ImageButtonBean(String x, String y) {
        this.x = x;
        this.y = y;
    }

    public String getX() {
        return (this.x);
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getY() {
        return (this.y);
    }

    public void setY(String y) {
        this.y = y;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * A convenience method to determine whether or not the corresponding
     * image element was selected.
     */
    public boolean isSelected() {
        return ((x != null) || (y != null));
    }

    /**
     * Return a string representation of this object.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer("ImageButtonBean[");

        sb.append(this.x);
        sb.append(", ");
        sb.append(this.y);
        sb.append("]");

        return (sb.toString());
    }
}

