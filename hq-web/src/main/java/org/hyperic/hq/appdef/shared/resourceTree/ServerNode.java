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

import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.Service;

public class ServerNode 
    implements java.io.Serializable
{
    private ResourceTree tree;
    private PlatformNode ownerPlatform;
    private Server  server;
    private ArrayList<ServiceNode>    services;

    ServerNode(ResourceTree tree, PlatformNode ownerPlatform, Server server)
    {
        this.tree          = tree;
        this.ownerPlatform = ownerPlatform;
        this.server        = server;
        this.services      = new ArrayList<ServiceNode>();
    }

    public Server getServer(){
        return this.server;
    }

    public PlatformNode getOwnerPlatform(){
        return this.ownerPlatform;
    }

    public ServiceNode addService(Service service){
        ServiceNode node;

        node = new ServiceNode(this.tree, this, service);
        this.services.add(node);
        return node;
    }

    public List<ServiceNode> getServices(){
        return this.services;
    }

    public int getNumServices(){
        return this.services.size();
    }

    public Iterator<ServiceNode> getServiceIterator(){
        return this.services.iterator();
    }
    
    public String toString(){
        return this.server.toString() + " services=" + this.services;
    }
}

