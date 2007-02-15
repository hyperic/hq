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

/*
 * Created on Feb 14, 2003
 *
 */
package org.hyperic.hq.ui.action.resource.group.inventory;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.ui.action.resource.ResourceForm;

public class RemoveGroupResourcesForm extends ResourceForm {

    private String[] _resources;
    protected Integer _resourceType;
    protected List _resourceTypes;

    public RemoveGroupResourcesForm() {
        super();
    }

    public Integer getF() {
        return getResourceType();
    }

    public void setF(Integer f) {
        setResourceType(f);
    }

    public String[] getR() {
        return getResources();
    }

    public void setR(String[] r) {
        setResources(r);
    }

    public String[] getResources() {
        return _resources;
    }

    public void setResources(String[] resources) {
        _resources = resources;
    }

    public Integer getResourceType() {
        return _resourceType;
    }


    public void setResourceType(Integer resourceType) {
        _resourceType = resourceType;
    }

    public List getResourceTypes() {
        return _resourceTypes;
    }

    public void setResourceTypes(List resourceTypes) {
        _resourceTypes = resourceTypes;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        _resources = new String[0];
        _resourceType = null;
        _resourceTypes = null;
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());

        s.append(" resources=");
        s.append(_resources);
        s.append(" resourceType=");
        s.append(_resourceType);
        s.append(" resourceTypes=");
        s.append(_resourceTypes);

        return s.toString();
    }

}
