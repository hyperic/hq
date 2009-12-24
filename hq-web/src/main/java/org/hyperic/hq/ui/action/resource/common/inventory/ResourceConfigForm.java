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

package org.hyperic.hq.ui.action.resource.common.inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.ui.action.resource.ResourceForm;

import org.apache.struts.action.ActionMapping;

public class ResourceConfigForm
    extends ResourceForm {

    private List resourceConfigOptions = new ArrayList();
    private List monitorConfigOptions = new ArrayList();
    private List controlConfigOptions = new ArrayList();
    private List rtConfigOptions = new ArrayList();
    private boolean serverBasedAutoInventory;
    protected boolean validationErrors;
    private boolean serviceRTEnabled;
    private boolean euRTEnabled;

    /**
     * A subclass of <code> ResourceForm </code> that adds convenience methods
     * for dealing with the Configuration Options.This form has built-in methods
     * to help in retrieval of configOptions from the plugin.
     */
    public Collection getResourceConfigOptions() {
        return resourceConfigOptions;
    }

    public void setResourceConfigOptions(List resourceConfigOptions) {
        if (validationErrors)
            return;
        this.resourceConfigOptions = resourceConfigOptions;
    }

    public Collection getControlConfigOptions() {
        return controlConfigOptions;
    }

    public void setControlConfigOptions(List controlConfigOptions) {
        if (validationErrors)
            return;
        this.controlConfigOptions = controlConfigOptions;
    }

    public Collection getMonitorConfigOptions() {
        return monitorConfigOptions;
    }

    public void setMonitorConfigOptions(List monitorConfigOptions) {
        if (validationErrors)
            return;
        this.monitorConfigOptions = monitorConfigOptions;
    }

    public Collection getRtConfigOptions() {
        return rtConfigOptions;
    }

    public void setRtConfigOptions(List rtConfigOptions) {
        if (validationErrors)
            return;
        this.rtConfigOptions = rtConfigOptions;
    }

    public boolean getServerBasedAutoInventory() {
        return this.serverBasedAutoInventory;
    }

    public void setServerBasedAutoInventory(boolean serverBasedAutoInventory) {
        if (validationErrors) {
            // do nothing as this gets it from the form
        } else
            this.serverBasedAutoInventory = serverBasedAutoInventory;
    }

    public boolean getServiceRTEnabled() {
        return this.serviceRTEnabled;
    }

    public void setServiceRTEnabled(boolean serviceRTEnabled) {
        this.serviceRTEnabled = serviceRTEnabled;
    }

    public boolean getEuRTEnabled() {
        return this.euRTEnabled;
    }

    public void setEuRTEnabled(boolean euRTEnabled) {
        this.euRTEnabled = euRTEnabled;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        serverBasedAutoInventory = false;
        validationErrors = false;
        serviceRTEnabled = false;
        euRTEnabled = false;
    }

}
