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

import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.autoinventory.AICompare;
import org.hyperic.util.ArrayUtil;
import org.hyperic.util.StringUtil;

/**
 * A RuntimeResourceReport represents the results of a single server's
 * runtime scan.  It is comprised of an array of platforms.
 * The platforms are matched against appdef by FQDN.
 * Within each platform, the servers are matched against appdef
 * by ID (only if it is the same as the reporting server), and then
 * by autoinventory-identifier.  Within each server, the services
 * are matched against appdef by name.
 */
public class RuntimeResourceReport {

    private int _serverId = -1;
    private AIPlatformValue[] _aiplatforms;

    public RuntimeResourceReport () {}
    public RuntimeResourceReport (int serverId) {
        _serverId = serverId;
    }

    public int getServerId () {
        return _serverId;
    }
    public void setServerId (int serverId) {
        _serverId = serverId;
    }

    public AIPlatformValue[] getAIPlatforms () {
        return _aiplatforms;
    }
    public void setAIPlatforms (AIPlatformValue[] aiplatforms) {
        _aiplatforms = aiplatforms;
    }
    public void addAIPlatform(AIPlatformValue aiplatform) {
        AIPlatformValue[] newArray = {aiplatform};
        _aiplatforms = (AIPlatformValue[]) 
            ArrayUtil.combine(_aiplatforms, newArray);
    }
    public void addAIPlatforms(AIPlatformValue[] aiplatforms) {
        _aiplatforms = (AIPlatformValue[]) 
            ArrayUtil.combine(_aiplatforms, aiplatforms);
    }

    public boolean isSameReport (RuntimeResourceReport other) {

        if (getServerId() != other.getServerId()) {
            // System.err.println("R-ISR: server ids differ");
            return false;
        }
        // System.err.println("\n\nR-ISR: DIFFING:\nthis="+this+"\nother="+other+"\n");

        AIPlatformValue[] p1, p2;
        p1 = getAIPlatforms();
        p2 = other.getAIPlatforms();
        if (p1.length != p2.length) {
            // System.err.println("R-ISR: lengths differ");
            return false;
        }

        boolean foundPlatform;
        for (int i=0; i<p1.length; i++) {
            foundPlatform = false;
            for (int j=0; j<p2.length; j++) {
                if (AICompare.compareAIPlatforms(p1[i], p2[j])) {
                    foundPlatform = true;
                    break;
                }
            }
            if (!foundPlatform) {
                // System.err.println("R-ISR: no matching platform for:"+p1[i]);
                return false;
            }
        }
        return true;
    }

    public String toString () {
        StringBuffer sb = new StringBuffer();
        sb.append("[RuntimeResourceReport serverId=").append(_serverId);
        if (_aiplatforms != null) {
            sb.append(" platforms={");
            for (int i=0; i<_aiplatforms.length; i++) {
                if (i>0) sb.append(", ");
                if (_aiplatforms[i] == null) {
                    sb.append("NULL-PLATFORM");
                    continue;
                }
                sb.append(_aiplatforms[i]);
                AIServerValue[] servers = _aiplatforms[i].getAIServerValues();
                if ( servers != null ) {
                    sb.append(" platform-servers=")
                        .append(StringUtil.arrayToString(servers));
                } else {
                    sb.append(" platform-servers=NONE");
                }
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
