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

package org.hyperic.hq.rt;

import java.util.Set;

public class RtSvcReq {
    // Fixed URL length
    public final static int MAX_URL_LENGTH = 766;

    // Fields
    private Integer _id;
    private long _version_;
    private Integer _serviceId;
    private String _url;
    private Set _rtRequestStat;

    // Constructors
    public RtSvcReq() {
    }

    // Property accessors
    public Integer getId() {
        return _id;
    }

    public void setId(Integer id) {
        _id = id;
    }

    public long get_version_() {
        return _version_;
    }

    public void set_version_(long version) {
        _version_ = version;
    }

    public Integer getSvcID() {
        return _serviceId;
    }

    public void setSvcID(Integer serviceId) {
        _serviceId = serviceId;
    }

    public String getUrl() {
        return _url;
    }

    public void setUrl(String url) {
        _url = truncateUrl(url);
    }

    public Set getRtRequestStat() {
        return _rtRequestStat;
    }

    public void setRtRequestStat(Set s) {
        _rtRequestStat = s;
    }
    
    public static String truncateUrl(String url) {
        // Truncate to MAX_URL_LENGTH characters
        if (url.length() > MAX_URL_LENGTH) {
            url = url.substring(0, MAX_URL_LENGTH - 1);
        }
        return url;
    }
}
