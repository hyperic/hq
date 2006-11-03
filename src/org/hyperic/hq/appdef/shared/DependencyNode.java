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

package org.hyperic.hq.appdef.shared;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

public class DependencyNode implements Serializable {

    private List childNodes;
    private List removedNodes;
    private AppServiceValue appService;

    public DependencyNode(AppServiceValue appService, List deps) {
        this.appService = appService;
        this.childNodes = deps;
        this.removedNodes = new ArrayList();
    }

    public boolean hasChildren() {
        if (childNodes == null) return false;
        else return childNodes.size() > 0;
    }

    public List getChildren() {
        if (childNodes == null) {
            // return an empty list
            return new ArrayList();
        } else {
            return childNodes;
        }
    }
        
    public int getChildCount() {
        if (childNodes == null) return 0;
        else return childNodes.size();
    }

    public AppServiceValue getAppService() {
        return this.appService;
    }

    public boolean isCluster() {
        return getAppService().getIsCluster();
    }

    public Integer getServicePK() {
        return getAppService().getService().getId();
    }

    public Integer getServiceClusterPK() {
        return getAppService().getServiceCluster().getId();
    }

    public Integer getId() {
        if (isCluster()) {
            return getAppService().getServiceCluster().getGroupId();
        } else {
            return this.appService.getService().getId();
        }
    }

    public ServiceLightValue getService() {
        return this.appService.getService();
    }

    public AppdefEntityID getEntityId() {
        if(isCluster())
            return new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_GROUP, this.getAppService().getServiceCluster().getGroupId().intValue());
        else
            return this.getAppService().getService().getEntityId();
    }

    public String getName() {
        if(isCluster())
            return this.getAppService().getServiceCluster().getName();
        else
            return this.getAppService().getService().getName();
    }

    public String getDescription() {
        if(isCluster())
            return this.getAppService().getServiceCluster().getDescription();
        else
            return this.getAppService().getService().getDescription();
    }

    public boolean isEntryPoint() {
        return this.appService.getIsEntryPoint();
    }

    public void addChild(AppServiceValue aDep) {
        if (childNodes == null) {
            childNodes = new ArrayList();
        }
        // Fix for 5958. Dont add the child dep twice
        if(!childNodes.contains(aDep)) {
            childNodes.add(aDep);
        }
    }

    public List getRemovedChildren() {
        if(this.removedNodes == null) {
            removedNodes = new ArrayList();
        }
        return removedNodes;
    }

    public void removeChild(AppServiceValue aDep) {
        this.getChildren().remove(aDep);
        this.getRemovedChildren().add(aDep);
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer(DependencyNode.class.getName());
        sb.append("[");
        sb.append(" appServiceId=").append(appService.getId());
        if(!isCluster()) {
            sb.append(" serviceId=").append(appService.getService().getId());
            sb.append(" serviceName=").append(appService.getService().getName());
        }
        else  {
            sb.append(" serviceClusterId=").append(appService.getServiceCluster().getGroupId());
            sb.append(" serviceClusterName=").append(appService.getServiceCluster().getName());
        }

        if (hasChildren()) {
            sb.append(" \nchildren(\n");
            for (Iterator iter = getChildren().iterator(); iter.hasNext();) {
                AppServiceValue child = (AppServiceValue) iter.next();
                sb.append(" childAppServiceId=").append(child.getId()).append("\n");
                if(!child.getIsCluster()) {
                    sb.append(" \tchildServiceId=").append(child.getService().getId()).append("\n");
                    sb.append(" \tchildServiceName=").append(child.getService().getName()).append("\n");
                } else {
                    sb.append(" \tchildServiceClusterId=").append(child.getServiceCluster().getGroupId()).append("\n");
                    sb.append(" \tchildServiceClusterName=").append(child.getServiceCluster().getName()).append("\n");
                }
            }
            sb.append(")"); 
        } else {
            sb.append("(no children)");
        }
        sb.append("]\n");
        return sb.toString();
    }
    
    public boolean equals(Object o) {
        boolean equal = false;
        DependencyNode aNode = (DependencyNode)o;
        // a node is equal if it's appSvcId is and it's set of children is
        if (aNode.getAppService().getId().equals(getAppService().getId())
                && aNode.compChildren().equals(compChildren()))
            equal = true;
        return equal;
    }
    
    private HashSet compChildren() {
        HashSet childSet = new HashSet();
        for (Iterator i = getChildren().iterator(); i.hasNext();) {
                AppServiceValue child = (AppServiceValue) i.next();
                childSet.add(child.getId());            
        }
        return childSet;        
    }

}
