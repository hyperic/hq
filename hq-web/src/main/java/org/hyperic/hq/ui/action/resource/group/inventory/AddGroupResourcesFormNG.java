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

package org.hyperic.hq.ui.action.resource.group.inventory;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.dispatcher.mapper.ActionMapping;
import org.hyperic.hq.ui.action.resource.NonScheduleResourceFormNG;

/**
 * A subclass of <code>ResourceForm</code> representing the
 * <em>AddGroupResources</em> form. The purpose of this form is to add
 * AppdefResourceValues to a AppdefGroupValue
 */
public class AddGroupResourcesFormNG
    extends NonScheduleResourceFormNG {

    private String[] _availableResources;
    private String[] _pendingResources;
    private Integer _psa;
    private Integer _psp;
    private Map<String,String> _availResourceTypes;
    private String _filterBy;
    private String _nameFilter;

    public AddGroupResourcesFormNG() {
        super();
    }

    public String[] getAvailableResource() {
        return _availableResources;
    }

    public String[] getAvailableResources() {
        return getAvailableResource();
    }

    public void setAvailableResource(String[] availableResource) {
        _availableResources = availableResource;
    }

    public void setAvailableResources(String[] availableResources) {
        setAvailableResource(availableResources);
    }

    public String[] getPendingResource() {
        return _pendingResources;
    }

    public String[] getPendingResources() {
        return getPendingResource();
    }

    public void setPendingResource(String[] pendingResource) {
        _pendingResources = pendingResource;
    }

    public void setPendingResources(String[] pendingResources) {
        setPendingResource(pendingResources);
    }

    public Integer getPsa() {
        return _psa;
    }

    public void setPsa(Integer ps) {
        _psa = ps;
    }

    public Integer getPsp() {
        return _psp;
    }

    public void setPsp(Integer ps) {
        _psp = ps;
    }

    public void reset() {
        _availResourceTypes = null;
        _availableResources = new String[0];
        _pendingResources = new String[0];
        _filterBy = null;
        _psa = null;
        _psp = null;
        super.reset();
    }


    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());
        s.append("psa=" + _psa + " ");
        s.append("psp=" + _psp + " ");
        s.append("nameFilter=").append(_nameFilter).append(" ");

        s.append("availableResources={");
        listToString(s, _availableResources);
        s.append("} ");

        s.append("pendingResources={");
        listToString(s, _pendingResources);
        s.append("}");

        return s.toString();
    }

    private void listToString(StringBuffer s, String[] l) {
        if (l != null) {
            for (int i = 0; i < l.length; i++) {
                s.append(l[i]);
                if (i < l.length - 1) {
                    s.append(", ");
                }
            }
        }
    }

    public Map<String,String> getAvailResourceTypes() {
        return _availResourceTypes;
    }

    public void setAvailResourceTypes(Map<String,String> availResourceTypes) {
        _availResourceTypes = availResourceTypes;
    }

    public String getFilterBy() {
        return _filterBy;
    }

    public void setFilterBy(String fs) {
        _filterBy = fs;
    }

    public String getNameFilter() {
        return _nameFilter;
    }

    public void setNameFilter(String s) {
        _nameFilter = s;
    }
}
