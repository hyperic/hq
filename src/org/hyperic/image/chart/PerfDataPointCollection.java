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

package org.hyperic.image.chart;

import org.hyperic.util.data.IStackedDataPoint;

public class PerfDataPointCollection extends DataPointCollection
{
    public static final int UNKNOWN   = 0;
    public static final int ENDUSER   = 1;
    public static final int WEBSERVER = 2;
    public static final int APPSERVER = 3;

    private static final String ENDUSER_NAME   = "End User";
    private static final String WEBSERVER_NAME = "Virtual Host";
    private static final String APPSERVER_NAME = "Web Application";
   
    private String  m_url;
    private int     m_type;
    private String  m_typeName;
    private int     m_requests;
        
    public int getRequest() {
        return m_requests; 
    }
    
    public int getType() {
        return m_type;
    }
    
    public String getTypeName() {
        return this.m_typeName;
    }
    
    public String getTypeString() {
        String result;
           
        switch(m_type) {
        case ENDUSER:
            result = ENDUSER_NAME;
            break;
        case WEBSERVER:
            result = WEBSERVER_NAME;
            break;
        case APPSERVER:
            result = APPSERVER_NAME;
            break;
        default:
            result = new String();
        }
        
        return result; 
    }
    
    public String getURL() {
        return m_url;
    }
    
    public void setRequest(int requests) {
        m_requests = requests; 
    }
    
    public void setType(int type) {
        m_type = type;
    }
    
    public void setType(int type, String name) {
        this.setType(type);
        this.setTypeName(name);
    }
    
    public void setTypeName(String name) {
        m_typeName = name;
    }
    
    public void setURL(String url) {
        m_url = url;
    }
    
    public boolean isStacked() {
        return
            ( (this.size() > 0) && (this.get(0) instanceof IStackedDataPoint) );
    }
}
