/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.appdef.shared.resourceTree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;

public class PlatformNode 
    implements java.io.Serializable
{
    private ResourceTree  tree;
    private Platform platform;
    private ArrayList<ServerNode>     servers;

    private boolean       showServers = false;

    PlatformNode(ResourceTree tree, Platform platform){
        this.tree     = tree;
        this.platform = platform;
        this.servers  = new ArrayList<ServerNode>();
    }

    public Platform getPlatform(){
        return this.platform;
    }

    public ServerNode addServer(Server server){
        ServerNode node;

        node = new ServerNode(this.tree, this, server);
        this.servers.add(node);
        return node;
    }

    public List<ServerNode> getServers(){
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

        for (Iterator<ServerNode> i = getServerIterator(); i.hasNext();) {
            ServerNode s = i.next();
            num += s.getNumServices();
        }

        return num;
    }
    
    public Iterator<ServerNode> getServerIterator(){
        return this.servers.iterator();
    }

    public String toString(){
        return this.platform.toString() + " servers=" + this.servers;
    }
}
