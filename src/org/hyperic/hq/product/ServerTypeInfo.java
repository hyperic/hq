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

public class ServerTypeInfo
    extends TypeInfo
    implements Cloneable {
    
    private String version;
    
    private String[] validPlatformTypes =
        TypeBuilder.ALL_PLATFORM_NAMES;
    
    /** 
     *  the virtual flag is used to mark a servertype as one 
     *  who contains platform services
     */
    private boolean isVirtual = false;
    
    public ServerTypeInfo() {
    }

    public ServerTypeInfo(String name,
                          String description,
                          String version)
    {
        setName(name);
        setDescription(description);
        this.version = version;
    }

    public Object clone() {
        ServerTypeInfo server =
            new ServerTypeInfo(getName(),
                               getDescription(),
                               getVersion());
        server.setValidPlatformTypes(getValidPlatformTypes());
        server.setVirtual(isVirtual());
        return server;
    }

    public boolean isVirtual() {
        return isVirtual;
    }

    public void setVirtual(boolean isVirtual) {
        this.isVirtual = isVirtual;
    }
    
    public int getType() {
        return TypeInfo.TYPE_SERVER;
    }
    
    public String getVersion() {
        return this.version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String[] getValidPlatformTypes(){
        return this.validPlatformTypes;
    }

    public void setValidPlatformTypes(String[] validPlatformTypes){
        this.validPlatformTypes = validPlatformTypes;
    }

    boolean isPlatformDevice() {
        String name = this.validPlatformTypes[0];
        return PlatformTypeInfo.isDevicePlatform(name);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Server(");
        sb.append("name=").append(getName()).append(", ");
        sb.append("version=").append(version).append(", ");
        sb.append("description=").append(getDescription()).append(", ");
        sb.append("platforms(");
        for (int i=0; i<validPlatformTypes.length; i++) {
            sb.append(validPlatformTypes[i]);
            if (i != validPlatformTypes.length - 1) {
                sb.append( ", ");
            }
        }
        sb.append("))");
        return sb.toString();
    }
}
