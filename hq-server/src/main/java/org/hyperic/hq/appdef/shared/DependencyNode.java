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

package org.hyperic.hq.appdef.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.server.session.Service;

public class DependencyNode implements Serializable, Comparable {

    private List _childNodes;
    private List _removedNodes;
    private AppService _appService;

    public DependencyNode(AppService appService, List deps) {
        _appService = appService;
        _childNodes = deps;
        _removedNodes = new ArrayList();
    }

    public boolean hasChildren() {
        if (_childNodes == null)
            return false;
        else
            return _childNodes.size() > 0;
    }

    public List<AppService> getChildren() {
        if (_childNodes == null)
            // return an empty list
            return new ArrayList();
        else
            return _childNodes;
    }
        
    public int getChildCount() {
        if (_childNodes == null)
            return 0;
        else
            return _childNodes.size();
    }

    public AppService getAppService() {
        return _appService;
    }

    public boolean isCluster() {
        return getAppService().isIsGroup();
    }

    public Integer getServicePK() {
        return getAppService().getService().getId();
    }

    public Integer getId() {
        if (isCluster()) {
            return getAppService().getResourceGroup().getId();
        } else {
            return getAppService().getService().getId();
        }
    }

    public Service getService() {
        return getAppService().getService();
    }

    public AppdefEntityID getEntityId() {
        if(isCluster())
            return AppdefEntityID.newGroupID(
                    getAppService().getResourceGroup().getId());
        else
            return getAppService().getService().getEntityId();
    }

    public String getName() {
        if (isCluster())
            return getAppService().getResourceGroup().getName();
        else
            return getAppService().getService().getName();
    }

    public String getDescription() {
        if(isCluster())
            return getAppService().getResourceGroup().getDescription();
        else
            return getAppService().getService().getDescription();
    }

    public boolean isEntryPoint() {
        return getAppService().isEntryPoint();
    }

    public void addChild(AppService depSvc) {
        if (_childNodes == null) {
            _childNodes = new ArrayList();
        }
        // Fix for 5958. Dont add the child dep twice
        if(!_childNodes.contains(depSvc)) {
            _childNodes.add(depSvc);
        }
    }

    public List getRemovedChildren() {
        if(_removedNodes == null) {
            _removedNodes = new ArrayList();
        }
        return _removedNodes;
    }

    public void removeChild(AppService aDep) {
        getChildren().remove(aDep);
        getRemovedChildren().add(aDep);
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer(DependencyNode.class.getName());
        sb.append("[");
        sb.append(" appServiceId=").append(_appService.getId());
        if(!isCluster()) {
            sb.append(" serviceId=").append(_appService.getService().getId());
            sb.append(" serviceName=").append(_appService.getService().getName());
        }
        else  {
            sb.append(" serviceClusterId=")
              .append(_appService.getResourceGroup().getId())
              .append(" serviceClusterName=")
              .append(_appService.getResourceGroup().getName());
        }

        if (hasChildren()) {
            sb.append(" \nchildren(\n");
            for (Iterator iter = getChildren().iterator(); iter.hasNext();) {
                AppService child = (AppService) iter.next();
                sb.append(" childAppServiceId=").append(child.getId()).append("\n");
                if(!child.isIsGroup()) {
                    sb.append(" \tchildServiceId=").append(child.getService().getId()).append("\n");
                    sb.append(" \tchildServiceName=").append(child.getService().getName()).append("\n");
                } else {
                    sb.append(" \tchildServiceClusterId=").append(child.getResourceGroup().getId()).append("\n");
                    sb.append(" \tchildServiceClusterName=").append(child.getResourceGroup().getName()).append("\n");
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
            AppService child = (AppService) i.next();
            childSet.add(child.getId());
        }
        return childSet;        
    }

    public int compareTo(Object arg0) {
        return getName().compareTo(((DependencyNode) arg0).getName());
    }
}
