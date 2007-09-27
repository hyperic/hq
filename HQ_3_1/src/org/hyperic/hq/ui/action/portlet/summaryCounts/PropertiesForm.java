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

package org.hyperic.hq.ui.action.portlet.summaryCounts;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

// XXX: remove when ImageBeanButton works
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.action.portlet.DashboardBaseForm;

/**
 * A subclass of <code>ValidatorForm</code> that adds convenience
 * methods for dealing with image-based form buttons.
 */
public class PropertiesForm extends DashboardBaseForm {
    
    /** Holds value of property application. */
    private boolean application;
    
    /** Holds value of property server. */
    private boolean server;
    
    /** Holds value of property service. */
    private boolean service;
    
    /** Holds value of property platform. */
    private boolean platform;
    
    /** Holds value of property cluster. */
    private boolean cluster;
    
    /** Holds value of property applicationTypes. */
    private String[] applicationTypes;
    
    /** Holds value of property clusterTypes. */
    private String[] clusterTypes;
    
    /** Holds value of property platformTypes. */
    private String[] platformTypes;
    
    /** Holds value of property serverTypes. */
    private String[] serverTypes;
    
    /** Holds value of property serviceTypes. */
    private String[] serviceTypes;
    
    /** Holds value of property groupMixed. */
    private boolean groupMixed;
    
    /** Holds value of property groupGroups. */
    private boolean groupGroups;
    
    /** Holds value of property groupPlatServerService. */
    private boolean groupPlatServerService;
    
    /** Holds value of property groupApplication. */
    private boolean groupApplication;
    
    //-------------------------------------instance variables

    //-------------------------------------constructors

    public PropertiesForm() {
        super();
}

    //-------------------------------------public methods


    public void reset(ActionMapping mapping,
                      HttpServletRequest request) {
        super.reset(mapping, request);
        
        applicationTypes = new String[]{};
        platformTypes  = new String[]{};
        clusterTypes = new String[]{};
        serverTypes = new String[]{};
        serviceTypes = new String[]{};
        
    }

    public String toString() {
        StringBuffer s = new StringBuffer();
        return s.toString();
    }
    
    /** Getter for property application.
     * @return Value of property application.
     *
     */
    public boolean isApplication() {
        return this.application;
    }
    
    /** Setter for property application.
     * @param application New value of property application.
     *
     */
    public void setApplication(boolean application) {
        this.application = application;
    }
    
    /** Getter for property server.
     * @return Value of property server.
     *
     */
    public boolean isServer() {
        return this.server;
    }
    
    /** Setter for property server.
     * @param server New value of property server.
     *
     */
    public void setServer(boolean server) {
        this.server = server;
    }
    
    /** Getter for property service.
     * @return Value of property service.
     *
     */
    public boolean isService() {
        return this.service;
    }
    
    /** Setter for property service.
     * @param service New value of property service.
     *
     */
    public void setService(boolean service) {
        this.service = service;
    }
    
    /** Getter for property platform.
     * @return Value of property platform.
     *
     */
    public boolean isPlatform() {
        return this.platform;
    }
    
    /** Setter for property platform.
     * @param platform New value of property platform.
     *
     */
    public void setPlatform(boolean platform) {
        this.platform = platform;
    }
    
    /** Getter for property cluster.
     * @return Value of property cluster.
     *
     */
    public boolean isCluster() {
        return this.cluster;
    }
    
    /** Setter for property cluster.
     * @param cluster New value of property cluster.
     *
     */
    public void setCluster(boolean cluster) {
        this.cluster = cluster;
    }
    
    /** Getter for property applicationTypes.
     * @return Value of property applicationTypes.
     *
     */
    public String[] getApplicationTypes() {
        return this.applicationTypes;
    }
    
    /** Setter for property applicationTypes.
     * @param applicationTypes New value of property applicationTypes.
     *
     */
    public void setApplicationTypes(String[] applicationTypes) {
        this.applicationTypes = applicationTypes;
    }
    
    /** Getter for property clusterTypes.
     * @return Value of property clusterTypes.
     *
     */
    public String[] getClusterTypes() {
        return this.clusterTypes;
    }
    
    /** Setter for property clusterTypes.
     * @param clusterTypes New value of property clusterTypes.
     *
     */
    public void setClusterTypes(String[] clusterTypes) {
        this.clusterTypes = clusterTypes;
    }
    
    /** Getter for property platformTypes.
     * @return Value of property platformTypes.
     *
     */
    public String[] getPlatformTypes() {
        return this.platformTypes;
    }
    
    /** Setter for property platformTypes.
     * @param platformTypes New value of property platformTypes.
     *
     */
    public void setPlatformTypes(String[] platformTypes) {
        this.platformTypes = platformTypes;
    }
    
    /** Getter for property serverTypes.
     * @return Value of property serverTypes.
     *
     */
    public String[] getServerTypes() {
        return this.serverTypes;
    }
    
    /** Setter for property serverTypes.
     * @param serverTypes New value of property serverTypes.
     *
     */
    public void setServerTypes(String[] serverTypes) {
        this.serverTypes = serverTypes;
    }
    
    /** Getter for property serviceTypes.
     * @return Value of property serviceTypes.
     *
     */
    public String[] getServiceTypes() {
        return this.serviceTypes;
    }
    
    /** Setter for property serviceTypes.
     * @param serviceTypes New value of property serviceTypes.
     *
     */
    public void setServiceTypes(String[] serviceTypes) {
        this.serviceTypes = serviceTypes;
    }
    
    /** Getter for property groupsMixed.
     * @return Value of property groupsMixed.
     *
     */
    public boolean isGroupMixed() {
        return this.groupMixed;
    }
    
    /** Setter for property groupsMixed.
     * @param groupsMixed New value of property groupsMixed.
     *
     */
    public void setGroupMixed(boolean groupMixed) {
        this.groupMixed = groupMixed;
    }
    
    /** Getter for property groupsGroups.
     * @return Value of property groupsGroups.
     *
     */
    public boolean isGroupGroups() {
        return this.groupGroups;
    }
    
    /** Setter for property groupsGroups.
     * @param groupsGroups New value of property groupsGroups.
     *
     */
    public void setGroupGroups(boolean groupGroups) {
        this.groupGroups = groupGroups;
    }
    
    /** Getter for property groupPlatServerService.
     * @return Value of property groupPlatServerService.
     *
     */
    public boolean isGroupPlatServerService() {
        return this.groupPlatServerService;
    }
    
    /** Setter for property groupPlatServerService.
     * @param groupPlatServerService New value of property groupPlatServerService.
     *
     */
    public void setGroupPlatServerService(boolean groupPlatServerService) {
        this.groupPlatServerService = groupPlatServerService;
    }
    
    /** Getter for property groupApplication.
     * @return Value of property groupApplication.
     *
     */
    public boolean isGroupApplication() {
        return this.groupApplication;
    }
    
    /** Setter for property groupApplication.
     * @param groupApplication New value of property groupApplication.
     *
     */
    public void setGroupApplication(boolean groupApplication) {
        this.groupApplication = groupApplication;
    }
    
}
