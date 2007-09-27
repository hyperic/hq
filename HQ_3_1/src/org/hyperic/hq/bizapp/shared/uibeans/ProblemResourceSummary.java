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
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.measurement.ext.ProblemResourceInfo;

/**
 * 
 * This bean is for end user representions of problem resources
 *
 */
public class ProblemResourceSummary implements Serializable {
    private String entityName = null;
    private ProblemResourceInfo prInfo = null;
    private double availability = Double.NaN;
    
    public ProblemResourceSummary(AppdefResourceValue entity,
                                  ProblemResourceInfo problem) {
        if (! entity.getEntityId().equals(problem.getEntityId())){
            throw new IllegalArgumentException("resource id " +
                entity.getEntityId() + " is not the one with problems, " +
                problem.getEntityId());
        }
        
        this.entityName = entity.getName();
        this.prInfo = problem;
    }

    public Integer getAlertCount() {
        if (prInfo.isAlertCountSet())
            return new Integer(prInfo.getAlertCount());
        
        return new Integer(0);
    }

    public Integer getOobCount() {
        if (prInfo.isOobCountSet())
            return new Integer(prInfo.getOobCount());
        
        return new Integer(0);
    }

    public Long getEarliest() {
        return new Long(prInfo.getEarliest());
    }

    public Long getLatest() {
        return new Long(prInfo.getLatest());
    }

    public AppdefEntityID getEntityId() {
        return prInfo.getEntityId();
    }

    public String getEntityName() {
        return entityName;
    }

    public int getResourceId() {
        return prInfo.getEntityId().getID();
    }

    public int getResourceType() {
        return prInfo.getEntityId().getType();
    }

    public double getAvailability() {
        return availability;
    }
    
    public void setAvailability(double availability) {
        this.availability = availability;
    }
    
    public void addProblemInfo(ProblemResourceInfo info) {
        this.prInfo.setAlertCount(this.prInfo.getAlertCount() +
                                  info.getAlertCount());
        this.prInfo.setOobCount(this.prInfo.getOobCount() + info.getOobCount());
        this.prInfo.setEarliest(Math.min(this.prInfo.getEarliest(),
                                         info.getEarliest()));
    }
}
