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

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.MiniResourceValue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MiniServerNode 
    implements java.io.Serializable
{
    private MiniResourceTree   tree;
    private MiniPlatformNode   ownerPlatform;
    private MiniResourceValue  server;
    private ArrayList          services;

    MiniServerNode(MiniResourceTree tree, MiniPlatformNode ownerPlatform,
                   MiniResourceValue server)
    {
        this.tree          = tree;
        this.ownerPlatform = ownerPlatform;
        this.server        = server;
        this.services      = new ArrayList();
    }

    public MiniResourceValue getServer(){
        return this.server;
    }

    public MiniPlatformNode getOwnerPlatform(){
        return this.ownerPlatform;
    }

    public MiniServiceNode addService(MiniResourceValue service){
        MiniServiceNode node;

        node = new MiniServiceNode(this.tree, this, service);
        this.services.add(node);
        return node;
    }

    public List getServices(){
        return this.services;
    }

    public int getNumServices(){
        return this.services.size();
    }

    public Iterator getServiceIterator(){
        return this.services.iterator();
    }

    // Accessors directly into the value objects.

    public String getName(){
        return ((MiniResourceValue)getServer()).name;
    }

    public Integer getId(){
        return new Integer(((MiniResourceValue)getServer()).id);
    }

    public Long getCTime(){
        return new Long(((MiniResourceValue)getServer()).ctime);
    }

    public String toString(){
        return this.server.toString() + " services=" + this.services;
    }
    
}

