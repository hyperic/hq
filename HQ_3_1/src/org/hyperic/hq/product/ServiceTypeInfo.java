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

package org.hyperic.hq.product;

public class ServiceTypeInfo
    extends TypeInfo
    implements Cloneable {
    
    private ServerTypeInfo server;
    
    private boolean internal = false;
    
    public ServiceTypeInfo() {
    }
    
    public ServiceTypeInfo(String name, String description,
                           ServerTypeInfo server) {
        setName(name);
        setDescription(description);
        this.server = server;
    }
    
    public Object clone() {
        ServiceTypeInfo service =
            new ServiceTypeInfo(getName(),
                                getDescription(),
                                getServerTypeInfo());
        service.setInternal(getInternal());
        return service;
    }
    
    public boolean getInternal() {
        return internal;
    }
    
    public void setInternal(boolean flag) {
        internal = flag;
    }
    
    public int getType() {
        return TypeInfo.TYPE_SERVICE;
    }
    
    public String getServerName() {
        return this.server.getName();
    }
    
    public String getServerVersion() {
        return this.server.getVersion();
    }
    
    public void setServerTypeInfo(ServerTypeInfo server) {
        this.server = server;
    }

    public ServerTypeInfo getServerTypeInfo() {
        return this.server;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Service(");
        sb.append("name=").append(getName()).append(", ");
        sb.append("serverVersion=").append(getServerVersion()).append(", ");
        sb.append("description=").append(getDescription()).append(", ");
        sb.append("internal=").append(internal).append(", ");
        sb.append("serverName=").append(getServerName());
        sb.append(")");
        return sb.toString();
    }

}
