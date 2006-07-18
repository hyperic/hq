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

/**
 * Bean to hold agent value while displaying the editConfigProperties
 * **/

public final class AgentBean 
    extends java.lang.Object 
        implements java.io.Serializable{

    
    private String ip;
    private Integer port;
    private String agentIpPort ;
    private String name;

    public AgentBean(String ip, Integer port, String name)   {
        this.ip = ip;
        this.port = port;
        this.name = name;
    }
    
    public AgentBean(String ip, Integer port) {
        this.ip = ip;
        this.port = port;
    }

    public AgentBean() {
    //empty constructor
    }

    
    public String getIp() {
        return ip;
    }
    
    public void setIp(String ip) {
        this.ip = ip;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Integer getPort() {
        return port;
    }
    
    public void setPort(Integer port) {
        this.port=port;
    }

    public void setIpPort(String ip, Integer port) {
        agentIpPort = ip + ":" + port.intValue();
    }

    public String getIpPort()   {
        return ip  + ":" + port.intValue();
    }

    public String getIpPortAndName() {
        return ip + ":" + port + " on " +name;
    }
    
    public String toString()
    {
	  StringBuffer str = new StringBuffer("{");

	  str.append("ip=" + getIp() + " " + "port =" + getPort());
	  str.append('}');

	  return(str.toString());
    }

}

// EOF
