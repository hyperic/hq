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

package org.hyperic.hq.ui.action.resource.application;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionMapping;

import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.ui.action.resource.ResourceForm;

/**
 * This is a type 4 resource per
 * org.hyperic.hq.appdef.shared.AppdefEntityConstants
 */
public class ApplicationForm
    extends ResourceForm {

    private String engContact;
    private String opsContact;
    private String busContact;

    /**
     * Returns the busContact.
     * @return String
     */
    public String getBusContact() {
        return busContact;
    }

    /**
     * Returns the engContact.
     * @return String
     */
    public String getEngContact() {
        return engContact;
    }

    /**
     * Returns the opsContact.
     * @return String
     */
    public String getOpsContact() {
        return opsContact;
    }

    /**
     * Sets the busContact.
     * @param busContact The busContact to set
     */
    public void setBusContact(String busContact) {
        this.busContact = busContact;
    }

    /**
     * Sets the engContact.
     * @param engContact The engContact to set
     */
    public void setEngContact(String engContact) {
        this.engContact = engContact;
    }

    /**
     * Sets the opsContact.
     * @param opsContact The opsContact to set
     */
    public void setOpsContact(String opsContact) {
        this.opsContact = opsContact;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        this.busContact = null;
        this.engContact = null;
        this.opsContact = null;
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());
        s.append(" busContact=").append(busContact);
        s.append(" engContact=").append(engContact);
        s.append(" opsContact=").append(opsContact);
        return s.toString();
    }

    public void loadResourceValue(AppdefResourceValue resourceValue) {
        super.loadResourceValue(resourceValue);
        ApplicationValue application = (ApplicationValue) resourceValue;
        this.setBusContact(application.getBusinessContact());
        this.setEngContact(application.getEngContact());
        this.setOpsContact(application.getOpsContact());
        this.setResourceType(application.getApplicationType().getId());
    }

    public void updateResourceValue(AppdefResourceValue resourceValue) {
        super.updateResourceValue(resourceValue);
        ApplicationValue application = (ApplicationValue) resourceValue;
        if (busContact != null)
            application.setBusinessContact(busContact);
        if (engContact != null)
            application.setEngContact(engContact);
        if (opsContact != null)
            application.setOpsContact(opsContact);
    }
}
