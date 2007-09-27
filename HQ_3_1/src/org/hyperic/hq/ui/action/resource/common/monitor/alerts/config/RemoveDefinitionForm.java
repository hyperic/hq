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

package org.hyperic.hq.ui.action.resource.common.monitor.alerts.config;

import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.ui.action.resource.ResourceForm;

import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

/**
 * A subclass of <code>ResourceForm</code> representing the
 * <em>RemoveDefinition</em> form.
 */
public class RemoveDefinitionForm extends ResourceForm  {

    /** Holds value of  alert definitions. */
    private Integer[] definitions;
    private Integer ad;
    private Integer active;
    private String setActiveInactive;
    private String aetid;    

    public RemoveDefinitionForm() {
    }

    public String toString() {
        if (definitions == null)
            return "empty";
        else
            return definitions.toString();    
    }
    
    /** Getter for alert definitionss
     * @return alert definitions in an array 
     *
     */
    public Integer[] getDefinitions() {
        return this.definitions;
    }
    
    /** Setter for alert definitions
     * @param alert definitions As an Integer array  
     *
     */
    public void setDefinitions(Integer[] definitions) {
        this.definitions = definitions;
    }

    public Integer getAd() {
        return this.ad;
    }
    
    public void setAd(Integer ad) {
        this.ad = ad;
    }
    
    public Integer getActive() {
        return this.active;
    }
    
    public void setActive(Integer active) {
        this.active = active;
    }
    
    public String getSetActiveInactive() {
        return this.setActiveInactive; 
    }
    
    public void setSetActiveInactive(String setActiveInactive) {
        this.setActiveInactive = setActiveInactive;
    }

    public String getAetid() {
        return aetid;
    }
    
    public void setAetid(String aetid) {
        this.aetid = aetid;
    }
}
