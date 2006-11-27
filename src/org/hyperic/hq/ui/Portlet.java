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

package org.hyperic.hq.ui;

/**
 * A class representing an individual component of an overall page
 * layout (<code>Portal</code>).
 *
 */
public class Portlet {
    
    private String _name;
    private String _description;
    private String _url;
    private String _label;
    private String _fullUrl;
    private boolean _isFirst = false;
    private boolean _isLast = false;

    public Portlet() {}
    
    public Portlet(String url) {
        super();
        _url = url;
    }

    public Portlet(String url, String fullUrl) {
        this(url);
        _fullUrl = fullUrl;
    }

    public String getName() { return _name; }
    public void setName(String name) { _name = name; }
    
    public String getDescription() { return _description; }
    public void setDescription(String description) {
        _description = description;
    }
    
    public String getUrl() { return _url; }
    public void setUrl(String url) { _url = url; }
    
    public String getLabel() { return _label; }
    public void setLabel(String label) { _label = label; }

    public void    setIsFirst() { _isFirst = true; }
    public boolean getIsFirst() { return _isFirst; }

    public void    setIsLast () { _isLast = true; }
    public boolean getIsLast () { return _isLast; }

    public String getFullUrl() {
        if (_fullUrl == null) {
            return _url;
        }
        
        return _fullUrl;
    }

    public void setFullUrl(String token) {
        _fullUrl = token;
    }

    public String getToken() {
        if (_fullUrl != null && _url.length() < _fullUrl.length()) {
            return _fullUrl.substring(_url.length());
        }
        return null;
    }
    
    public String toString() {
        return "[ name:" + getName() + " url: " + getUrl() + " ]" ;
    }
}
