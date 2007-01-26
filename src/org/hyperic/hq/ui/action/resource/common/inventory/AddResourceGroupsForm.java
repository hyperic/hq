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

// -*- Mode: Java; indent-tabs-mode: nil; -*-

/*
 * AddResourceGroupsForm.java
 *
 */

package org.hyperic.hq.ui.action.resource.common.inventory;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.ui.action.BaseValidatorForm;

/**
 * removes a list of groups from a resource
 * 
 *
 */
public class AddResourceGroupsForm extends BaseValidatorForm {

    //-------------------------------------instance variables

    private Integer[] availableGroups;
    private Integer[] pendingGroups;
    private Integer psa;
    private Integer psp;
    private Integer rid;
    private Integer type;
    
    //-------------------------------------constructors

    public AddResourceGroupsForm() {
        super();
    }

    //-------------------------------------public methods

    public Integer[] getAvailableGroup() {
        return this.availableGroups;
    }

    public Integer[] getAvailableGroups() {
        return getAvailableGroup();
    }

    public void setAvailableGroup(Integer[] availableGroups) {
        this.availableGroups = availableGroups;
    }

    public void setAvailableGroups(Integer[] availableGroups) {
        setAvailableGroup(availableGroups);
    }

    public Integer[] getPendingGroup() {
        return this.pendingGroups;
    }

    public Integer[] getPendingGroups() {
        return getPendingGroup();
    }

    public void setPendingGroup(Integer[] pendingGroups) {
        this.pendingGroups = pendingGroups;
    }

    public void setPendingGroups(Integer[] pendingGroups) {
        setPendingGroup(pendingGroups);
    }

    public Integer getPsa() {
        return this.psa;
    }

    public void setPsa(Integer ps) {
        this.psa = ps;
    }

    public Integer getPsp() {
        return this.psp;
    }

    public void setPsp(Integer ps) {
        this.psp = ps;
    }

    public Integer getRid() {
        return rid;
    }

    public void setRid(Integer rid) {
        this.rid = rid;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getEid() {
        if (type != null && rid != null)
            return new AppdefEntityID(type.intValue(), rid).toString();

        return null;
    }
    
    public void setEid(String eidStr) {
        AppdefEntityID eid = new AppdefEntityID(eidStr);
        rid = eid.getId();
        type = new Integer(eid.getType());
    }
    
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        availableGroups = new Integer[0];
        pendingGroups = new Integer[0];
        psa = null;
        psp = null;
        rid = null;
        type = null;
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());

        s.append(" rid=");
        s.append(rid);
        s.append(" type=");
        s.append(type);
        s.append(" psa=");
        s.append(psa);
        s.append(" psp=");
        s.append(psp);
        s.append(" availableGroups=");
        if (availableGroups != null && availableGroups.length > 0) {
            s.append(Arrays.asList(availableGroups));
        } else {
            s.append("{}");
        }
        s.append(" pendingGroups=");
        if (pendingGroups != null && pendingGroups.length > 0) {
            s.append(Arrays.asList(pendingGroups));
        } else {
            s.append("{}");
        }
        return s.toString();
    }
}
