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

import org.hyperic.hq.appdef.shared.ApplicationValue;

import java.util.ArrayList;
import java.util.Iterator;

public class ApplicationNode 
    implements java.io.Serializable
{
    private ResourceTree     tree;
    private ApplicationValue app;
    private ArrayList        services;

    ApplicationNode(ResourceTree tree, ApplicationValue app){
        this.tree     = tree;
        this.services = new ArrayList();
    }

    public ApplicationValue getApplication(){
        return this.app;
    }

    public void linkToService(ServiceNode service){
        if(service.getTree() != this.tree){
            throw new IllegalArgumentException("Services may only be linked " +
                                               "to applications within the " +
                                               "same ResourceTree");
        }

        this.services.add(service);
    }

    public Iterator getServices(){
        return this.services.iterator();
    }

    public String toString(){
        return this.app.toString() + " services=" + this.services;
    }
}
