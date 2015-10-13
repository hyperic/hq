package org.hyperic.hq.ui.action.resource.common.inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.dispatcher.mapper.ActionMapping;
import org.hyperic.hq.ui.action.resource.ResourceFormNG;

public class ResourceConfigFormNG extends ResourceFormNG {

	
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
