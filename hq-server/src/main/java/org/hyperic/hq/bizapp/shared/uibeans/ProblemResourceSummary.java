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
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.measurement.ext.ProblemResourceInfo;

/**
 * 
 * This bean is for end user representations of problem resources
 *
 */
public class ProblemResourceSummary implements Serializable {
    private String _entityName = null;
    private ProblemResourceInfo _prInfo = null;
    private double _availability = Double.NaN;
    
    public ProblemResourceSummary(AppdefEntityValue entity,
                                  ProblemResourceInfo problem)
        throws AppdefEntityNotFoundException, PermissionException {
        if (! entity.getID().equals(problem.getEntityId())){
            throw new IllegalArgumentException("resource id " +
                entity.getID() + " is not the one with problems, " +
                problem.getEntityId());
        }
        
        _entityName = entity.getName();
        _prInfo = problem;
    }

    public Integer getAlertCount() {
        if (_prInfo.isAlertCountSet())
            return new Integer(_prInfo.getAlertCount());
        
        return new Integer(0);
    }

    public Integer getOobCount() {
        if (_prInfo.isOobCountSet())
            return new Integer(_prInfo.getOobCount());
        
        return new Integer(0);
    }

    public Long getEarliest() {
        return new Long(_prInfo.getEarliest());
    }

    public Long getLatest() {
        return new Long(_prInfo.getLatest());
    }

    public AppdefEntityID getEntityId() {
        return _prInfo.getEntityId();
    }

    public String getEntityName() {
        return _entityName;
    }

    public int getResourceId() {
        return _prInfo.getEntityId().getID();
    }

    public int getResourceType() {
        return _prInfo.getEntityId().getType();
    }

    public double getAvailability() {
        return _availability;
    }
    
    public void setAvailability(double availability) {
        _availability = availability;
    }
    
    public void addProblemInfo(ProblemResourceInfo info) {
        _prInfo.setAlertCount(_prInfo.getAlertCount() + info.getAlertCount());
        _prInfo.setOobCount(_prInfo.getOobCount() + info.getOobCount());
        _prInfo.setEarliest(Math.min(_prInfo.getEarliest(),
                                     info.getEarliest()));
    }
}
