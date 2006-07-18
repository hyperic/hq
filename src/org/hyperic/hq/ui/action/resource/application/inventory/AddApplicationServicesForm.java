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

package org.hyperic.hq.ui.action.resource.application.inventory;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;

import org.hyperic.hq.ui.action.BaseValidatorForm;

public class AddApplicationServicesForm extends BaseValidatorForm {
    private String[] availableServices;
    private String[] pendingServices;
    private Integer psa;
    private Integer psp;
    private Integer rid;
    private Integer type;
    private Integer appSvcId;
    private String nameFilter;
    
    public AddApplicationServicesForm() {
        super();
    }

    public String[] getAvailableServices() {
        return this.availableServices;
    }

    public void setAvailableServices(String[] availableServices) {
        this.availableServices = availableServices;
    }

    public String[] getPendingServices() {
        return this.pendingServices;
    }

    public void setPendingServices(String[] pendingServices) {
        this.pendingServices = pendingServices;
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
    
    /**
     * @return
     */
    public Integer getAppSvcId() {
        return appSvcId;
    }

    /**
     * @param integer
     */
    public void setAppSvcId(Integer integer) {
        appSvcId = integer;
    }

    public String getNameFilter() {
        return nameFilter;   
    }
    
    public void setNameFilter(String s) {
        this.nameFilter = s;
    }
    
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        availableServices = new String[0];
        pendingServices = new String[0];
        psa = null;
        psp = null;
        rid = null;
        type = null;
        appSvcId = null;
        nameFilter = null;
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
        s.append(" appSvcId=");
        s.append(appSvcId);
        s.append(" nameFilter=").append(nameFilter);
        s.append(" availableServices=");
        if (availableServices == null) {
            s.append("<NULL>");
        } else {
            s.append(Arrays.asList(availableServices));
        }
        s.append(" pendingServices=");
        if (pendingServices== null) {
            s.append("<NULL>");
        } else {
            s.append(Arrays.asList(pendingServices));
        }
        return s.toString();
    }
}
