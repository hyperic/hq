/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2011], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.appdef.server.session;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.appdef.Agent;

@SuppressWarnings("serial")
public class AgentPluginStatus extends PersistedObject {

    private long lastCheckin;
    private String md5;
    private String fileName;
    private String pluginName;
    private String productName;
    private Agent agent;
    private String lastSyncStatus;
    private long lastSyncAttempt;
    
    public AgentPluginStatus() {
    }

    public long getLastCheckin() {
        return lastCheckin;
    }

    public void setLastCheckin(long lastCheckin) {
        this.lastCheckin = lastCheckin;
    }

    public String getMD5() {
        return md5;
    }

    public void setMD5(String md5) {
        this.md5 = md5;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }
    
    public String getLastSyncStatus() {
        return lastSyncStatus;
    }

    public void setLastSyncStatus(String lastSyncStatus) {
        this.lastSyncStatus = lastSyncStatus;
    }

    public long getLastSyncAttempt() {
        return lastSyncAttempt;
    }

    public void setLastSyncAttempt(long lastSyncAttempt) {
        this.lastSyncAttempt = lastSyncAttempt;
    }

    public int hashCode() {
        return md5.hashCode();
    }
    
    public boolean equals(Object rhs) {
        if (this == rhs) {
            return true;
        }
        if (!(rhs instanceof AgentPluginStatus)) {
            return false;
        }
        AgentPluginStatus o = (AgentPluginStatus) rhs;
        return md5.equals(o.md5) &&
               fileName.equals(o.fileName) &&
               pluginName.equals(o.pluginName) &&
               productName.equals(o.productName);
    }

}
