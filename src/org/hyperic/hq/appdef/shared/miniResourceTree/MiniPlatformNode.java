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

package org.hyperic.hq.appdef.shared.miniResourceTree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.MiniResourceValue;

public class MiniPlatformNode 
    implements java.io.Serializable
{
    private MiniResourceTree  tree;
    private MiniResourceValue platform;
    private ArrayList         servers;

    private boolean       showServers = false;

    MiniPlatformNode(MiniResourceTree tree, MiniResourceValue platform){
        this.tree     = tree;
        this.platform = platform;
        this.servers  = new ArrayList();
    }

    public MiniResourceValue getPlatform(){
        return this.platform;
    }

    public MiniServerNode addServer(MiniResourceValue server){
        MiniServerNode node;

        node = new MiniServerNode(this.tree, this, server);
        this.servers.add(node);
        return node;
    }

    public List getServers(){
        return this.servers;
    }

    public boolean getShowServers() {
        return this.showServers;
    }

    public void setShowServers(boolean flag) {
        this.showServers = flag;
    }

    public int getNumServers(){
        return this.servers.size();
    }

    public int getNumResources(){
        int num = getNumServers();

        for (Iterator i = getServerIterator(); i.hasNext();) {
            MiniServerNode s = (MiniServerNode)i.next();
            num += s.getNumServices();
        }

        return num;
    }
    
    public Iterator getServerIterator(){
        return this.servers.iterator();
    }

    // Accessors directly into the value objects.

    public String getName(){
        return ((MiniResourceValue)getPlatform()).name;
    }

    public Integer getId(){
        return new Integer(((MiniResourceValue)getPlatform()).id);
    }

    public Long getCTime(){
        return new Long(((MiniResourceValue)getPlatform()).ctime);
    }

    /**
     * Get the newest timestamp from all resources on a platform
     */
    public Long getResourceCTime(){
        Long cTime = getCTime();

        for (Iterator s = getServerIterator(); s.hasNext();) {
            MiniServerNode sNode = (MiniServerNode)s.next();
            if (cTime.compareTo(sNode.getCTime()) < 0) {
                // Server is newer, update
                cTime = sNode.getCTime();
            }

            for (Iterator v = sNode.getServiceIterator(); v.hasNext();) {
                MiniServiceNode vNode = (MiniServiceNode)v.next();
                if (cTime.compareTo(vNode.getCTime()) < 0) {
                    // Service is newer, update
                    cTime = vNode.getCTime();
                }
            }
        }

        return cTime;
    }

    public String toString(){
        return this.platform.toString() + " servers=" + this.servers;
    }
}
