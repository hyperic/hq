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

package org.hyperic.hq.agent;

/**
 * A class which handles generic API information, such as version
 * number calculation, etc.
 */

public abstract class AgentAPIInfo {
    private byte majorVersion  = 0x00;
    private byte minorVersion  = 0x00;
    private byte bugfixVersion = 0x00;

    /**
     * Initialize the AgentAPIInfo object with versioning info.
     *
     * @param majorVersion  Major version #
     * @param minorVersion  Minor version #
     * @param bugfixVersion Bugfix version #
     */

    public AgentAPIInfo(byte majorVersion, byte minorVersion, 
			byte bugfixVersion)
    {
        this.majorVersion  = majorVersion;
        this.minorVersion  = minorVersion;
        this.bugfixVersion = bugfixVersion;
    }

    /**
     * Retrieve the major version number component
     */

    public byte getMajorVersion(){
        return this.majorVersion;
    }

    /**
     * Retrieve the minor version number component
     */

    public byte getMinorVersion(){
        return this.minorVersion;
    }

    /**
     * Retrieve the bugfix version number component
     */

    public byte getBugfixVersion(){
        return this.bugfixVersion;
    }

    /**
     * Get a single integer representation of the full version.
     *
     * @return an integer representation of the version components
     */

    public int getVersion(){
        return (majorVersion << 16) + (minorVersion << 8) + bugfixVersion;
    }

    /**
     * Check to see if another version is compatible with the object
     * containing this method.
     *
     * @param otherVersion Version info as obtained via getVersion() in
     *                     another APIInfo object.
     *
     * @return a true value if the versions are compatible, else false.
     */

    public boolean isCompatible(int otherVersion){
        // This could be done in a much more efficient manner, but this
        // way, it's very obvious what is going on

        byte tmpMajor  = (byte)((otherVersion & 0x00ff0000) >> 16);
        byte tmpMinor  = (byte)((otherVersion & 0x0000ff00) >> 8);
        //byte tmpBugFix = (byte)((otherVersion & 0x000000ff));

        return (tmpMajor == majorVersion) && (tmpMinor == minorVersion);
    }
}
