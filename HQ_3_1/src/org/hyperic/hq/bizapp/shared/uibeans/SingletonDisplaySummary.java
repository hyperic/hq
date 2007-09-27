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

package org.hyperic.hq.bizapp.shared.uibeans;

import java.io.Serializable;

import org.hyperic.hq.appdef.shared.AppdefEntityID;

/**
 * Represents the resource current health summary displayed on the "Current
 * Health" views for a single resource.
 */
public class SingletonDisplaySummary 
    extends ResourceTypeDisplaySummary
    implements Serializable 
{
    private AppdefEntityID entityId;
    private String         entityName;
    
    public SingletonDisplaySummary() {
        setNumResources(new Integer(1));
    }

    public SingletonDisplaySummary(AppdefEntityID eid, String name) {
        this();
        entityId = eid;
        entityName = name;
    }

    public int getSummaryType() {
        return UIConstants.SUMMARY_TYPE_SINGLETON;
    }

    /**
     * Returns the entity id corresponding to the resource represented
     * by the summary.
     */
    public AppdefEntityID getEntityId() {
        return entityId;
    }

    /**
     * Set the entity id corresponding to the resource represented
     * by the summary.
     *
     *@param eid the entity id of the summarized resource
     */
    public void setEntityId(AppdefEntityID eid) {
        entityId = eid;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer(super.toString());
        sb.append(",entityId=").append(entityId);
        sb.append(",entityName=").append(entityName);
        return sb.toString();
    }
}
