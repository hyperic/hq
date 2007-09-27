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

package org.hyperic.hq.ui.action.resource.server.inventory;

import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.ui.action.resource.ResourceForm;

import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

/**
 * A subclass of <code>ResourceForm</code> that adds convenience
 * methods for dealing with Server.
 */
public class ServerForm extends ResourceForm {

    //-------------------------------------instance variables
    
    private String installPath;

    //-------------------------------------constructors
    
    //-------------------------------------public methods
    
    /**
     * Returns the installPath.
     * @return String
     */
    
    public String getInstallPath() {
	return installPath;
    } 

    /** 
     * Sets the installPath.
     * @param installPath The installPath to set
     */
    
    public void setInstallPath(String installPath) {
	this.installPath = installPath;
    }

    /** set the server type here.  Can't access the
     * server type in the super class.
     * 
     * @param sValue
     */    
    public void loadResourceValue(AppdefResourceValue rValue)
    {
        super.loadResourceValue(rValue);
        
        ServerValue sValue = (ServerValue)rValue;
        setResourceType(sValue.getServerType().getId());                
    }
    
    /** loads the server value
     * 
     * @param sValue
     */    
    public void loadResourceValue(ServerValue sValue)
    {

        super.loadResourceValue(sValue);
                
        setInstallPath(sValue.getInstallPath());
        
    }

    /** updates the server value
     * 
     * @param sValue
     */    
    public void updateServerValue(ServerValue sValue)
    {
        super.updateResourceValue(sValue);
        
        if (installPath != null) {
            sValue.setInstallPath(installPath);
        }

    }

}
