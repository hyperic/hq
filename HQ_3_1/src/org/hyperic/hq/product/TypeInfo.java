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

import org.hyperic.util.StringUtil;

/**
 * Carry information about appdef entity type
 */
public abstract class TypeInfo {

    // XXX: These must match AppdefEntityConstants
    public static final int TYPE_PLATFORM = 1;
    public static final int TYPE_SERVER   = 2;    
    public static final int TYPE_SERVICE  = 3;
    
    private String name;
    private String description;

    private String formattedName = null;
    
    public TypeInfo() {
    }
    
    public abstract int getType();
    
    public static String formatName(String name) {
        return StringUtil.replace(name.toLowerCase(), " ", "-");
    }

    /** Getter for formatted property name, lower cased
     *  spaces converted to hypens.
     * @return Value of formatted property name.
     *
     */
    public String getFormattedName() {
        if (this.formattedName == null) {
            this.formattedName = formatName(getName());
        }

        return this.formattedName;
    }

    /** Getter for property name.
     * @return Value of property name.
     *
     */
    public String getName() {
        return this.name;
    }
    
    /** Setter for property name.
     * @param name New value of property name.
     *
     */
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean equals(Object otherObj){
        TypeInfo other = (TypeInfo)otherObj;
        String[] oPlatformTypes, mPlatformTypes;

        oPlatformTypes = other.getPlatformTypes();
        mPlatformTypes = this.getPlatformTypes();
        if(oPlatformTypes.length != mPlatformTypes.length)
            return false;

        for(int i=0; i<oPlatformTypes.length; i++){
            if(!oPlatformTypes[i].equals(mPlatformTypes[i])){
                return false;
            }
        }

        return other.getName().equals(this.name) &&
            other.getType() == this.getType();
    }

    /**
     * Test that entity is a server type that matches given server name.
     */
    public boolean isServer(String name) {
        return isServer(name, null);
    }

    /**
     * Test that entity is a server type that matches given server name
     * and version.
     */
    public boolean isServer(String name, String version) {
        if (getType() != TYPE_SERVER) {
            return false;
        }

        if (version == null) {
            version = ((ServerTypeInfo)this).getVersion();
        }

        return getName().equals(name + " " + version);
    }

    /**
     * Test that entity is a service type that matches given service name.
     */
    public boolean isService(String name) {
        return isService(name, null);
    }

    /**
     * Test that entity is a service type that matches given service name
     * and server version.
     */
    public boolean isService(String name, String version) {
        if (getType() != TYPE_SERVICE) {
            return false;
        }

        ServiceTypeInfo si = (ServiceTypeInfo)this;

        if (version != null) {
            if (!version.equals(si.getServerVersion())) {
                return false;
            }
        }

        return getName().equals(si.getServerName() + " " + name);
    }

    /**
     * Get the version for a server
     * @throws IllegalArgumentException If invoked on a type other
     *         than TYPE_SERVER or TYPE_SERVICE.
     */
    public String getVersion() {
        String sVersion;

        switch (getType()) {
          case TYPE_SERVICE:
            sVersion = ((ServiceTypeInfo)this).getServerVersion();
            break;
          case TYPE_SERVER:
            sVersion = ((ServerTypeInfo)this).getVersion();
            break;
          default:
            throw new IllegalArgumentException("not a server or service");
        }

        return sVersion;
    }

    /**
     * Test that entity server or service type version matches
     * the given version.
     * @throws IllegalArgumentException If invoked on a type other
     * than TYPE_SERVER or TYPE_SERVICE.
     */
    public boolean isVersion(String version) {

        return getVersion().equals(version);
    }

    /**
     * @return list of supported platforms for this type.
     */
    //XXX some overlap with ServerTypeInfo.getValidPlatformTypes
    //but this is handy.  perhaps we should fold the two into this one.
    public String[] getPlatformTypes() {
        ServerTypeInfo server;

        switch (getType()) {
          case TYPE_SERVER:
            server = (ServerTypeInfo)this;
            break;
          case TYPE_SERVICE:
            server = ((ServiceTypeInfo)this).getServerTypeInfo();
            break;
          case TYPE_PLATFORM:
            return new String[] { getName() };
          default:
            return null;
        }

        return server.getValidPlatformTypes();
    }

    /**
     * @return true if the platforms for this type are all in the Unix family.
     */
    public boolean isUnixPlatform() {
        return
            (getPlatformTypes().length != 1) ||
            !PlatformDetector.isWin32(getPlatformTypes()[0]);
    }

    /**
     * @return true if the platforms for this type are all in the Win32 family.
     */
    public boolean isWin32Platform() {
        return
            (getPlatformTypes().length == 1) &&
            PlatformDetector.isWin32(getPlatformTypes()[0]);
    }
}
