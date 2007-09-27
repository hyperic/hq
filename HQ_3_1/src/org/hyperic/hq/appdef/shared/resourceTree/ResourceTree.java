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

package org.hyperic.hq.appdef.shared.resourceTree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.PlatformValue;

/**
 * A tree which represents portions of a hierarchy.  The tree
 * representation describes the relationships between all the 
 * different types (platform, server, service, application), 
 */
public class ResourceTree 
    implements java.io.Serializable
{
    private ArrayList apps;
    private ArrayList platforms;

    public ResourceTree(){
        this.apps      = new ArrayList();
        this.platforms = new ArrayList();
    }
    
    public ApplicationNode addApplication(ApplicationValue app){
        ApplicationNode node;

        node = new ApplicationNode(this, app);
        this.apps.add(app);
        return node;
    }

    public Iterator getApplications(){
        return this.apps.iterator();
    }

    public PlatformNode addPlatform(PlatformValue platform){
        PlatformNode node;

        node = new PlatformNode(this, platform);
        this.platforms.add(node);
        return node;
    }

    public List getPlatforms(){
        return this.platforms;
    }

    public Iterator getPlatformIterator(){
        return this.platforms.iterator();
    }

    public String toString(){
        return "Resource Tree: Platforms=" + this.platforms +
            " Apps=" + this.apps;
    }
}
