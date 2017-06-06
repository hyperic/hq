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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.dispatcher.mapper.ActionMapping;
import org.hyperic.hq.ui.action.BaseValidatorFormNG;


/**
 * This is used to managed the lists of AppServiceValue objects used for screen
 * "2.1.6.4 Application: Service Dependencies"
 * 
 * The screen has a list of AppServiceValues that this AppServiceValue depends
 * on (children or "dependers"). That list may grow or shrink. Growing it is
 * accomplished by invoking AddApplicationServicesPrepareFormAction to paint
 * screen "2.1.6.5 Edit Application: Add Dependencies", performing add and
 * commit operations (AddApplicationServicesAction).
 * 
 * The screen has another list of what the AppServiceValue depends on
 * ("dependees"). That is a read-only list for display purposes.
 */
public class ListServiceDependenciesFormNG extends BaseValidatorFormNG {
    // the keys are AppServiceNodeBean.getId():Integer's
    // the values are AppServiceNodeBean objects
    private List appSvcDependers = new ArrayList();
    private List appSvcDependees = new ArrayList();
    // the AppService's id
    private Integer appSvcId;
    // the id for the application under discussion
    private Integer rid;
    // the AppdefResourceType id of the application under discussion
    private Integer type;
    // the name of the service our current AppSvcValue is bound to needs
    // to be displayed
    private String serviceLabel;

    /**
     * Constructor for ListServiceDependenciesForm.
     */
    public ListServiceDependenciesFormNG() {
        super();
    }

    /**
     * Returns the appSvcDependees.
     * @return List
     */
    public List getAppSvcDependees() {
        return appSvcDependees;
    }

    /**
     * Returns the appSvcDependers.
     * @return List
     */
    public List getAppSvcDependers() {
        return appSvcDependers;
    }

    /**
     * Returns the type.
     * @return Integer
     */
    public Integer getType() {
        return type;
    }

    /**
     * Sets the appSvcDependees.
     * @param appSvcDependees The appSvcDependees to set
     */
    public void setAppSvcDependees(List appSvcDependees) {
        this.appSvcDependees = appSvcDependees;
    }

    /**
     * Sets the appSvcDependers.
     * @param appSvcDependers The appSvcDependers to set
     */
    public void setAppSvcDependers(List appSvcDependers) {
        this.appSvcDependers = appSvcDependers;
    }

    /**
     * Sets the type.
     * @param type The type to set
     */
    public void setType(Integer type) {
        this.type = type;
    }

    /**
     * Returns the serviceLabel.
     * @return String
     */
    public String getServiceLabel() {
        return serviceLabel;
    }

    /**
     * Sets the serviceLabel.
     * @param serviceLabel The serviceLabel to set
     */
    public void setServiceLabel(String serviceLabel) {
        this.serviceLabel = serviceLabel;
    }

    /**
     * Returns the rid.
     * @return Integer
     */
    public Integer getRid() {
        return rid;
    }

    /**
     * Sets the rid.
     * @param rid The rid to set
     */
    public void setRid(Integer rid) {
        this.rid = rid;
    }

    /**
     * Returns the appSvcId.
     * @return Integer
     */
    public Integer getAppSvcId() {
        return appSvcId;
    }

    /**
     * Sets the appSvcId.
     * @param appSvcId The appSvcId to set
     */
    public void setAppSvcId(Integer appSvcId) {
        this.appSvcId = appSvcId;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(super.toString());
        sb.append(" appSvcDependers=[");
        sb.append(appSvcDependers);
        sb.append("],appSvcDependees=[");
        sb.append(appSvcDependees);
        sb.append(",appSvcId=");
        sb.append(appSvcId);
        sb.append(",rid=");
        sb.append(rid);
        sb.append(",type=");
        sb.append(type);
        sb.append(",serviceLabel=");
        sb.append(serviceLabel);
        return sb.toString();
    }

    public void reset() {
        super.reset();
        appSvcDependers = new ArrayList();
        appSvcDependees = new ArrayList();
        appSvcId = null;
        rid = null;
        type = null;
        serviceLabel = null;
    }
}
