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

public class CMsgElement {

    /** text is a key into ApplicationResources.properties */
    public String text = null;

    /** url is plaintext */
    public String url = null;

    /** mouseover is plaintext */
    public String mouseover = null;

    /** params are plaintext, and will subtituted as {0} style
     * parameters into the 'text' message */
    public String[] params = null;

    public CMsgElement (String text) {
        this(text, new String[0]);
    }
    public CMsgElement (String text, String url) {
        this(text, url, new String[0]);
    }
    public CMsgElement (String text, String[] params) {
        this.text   = text;
        this.params = params;
    }
    public CMsgElement (String text, String url, String[] params) {
        this.text   = text;
        this.url    = url;
        this.params = params;
    }

    public String   getText      () { return text; }
    public String   getUrl       () { return url; }
    public String[] getParams    () { return params; }
    public String   getMouseover () { return mouseover; }
}
