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

package org.hyperic.hq.ui.action.resource.platform.autodiscovery;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.CollectionUtils;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.ImageButtonBean;
import org.apache.struts.util.LabelValueBean;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.ResourceForm;
import org.hyperic.hq.ui.exception.InvalidOptionValsFoundException;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;

public class PlatformAutoDiscoveryForm extends ResourceForm {

    /**
     * LabelValueBean objects representing the serverTypes
     */
    ArrayList serverTypesLB = new ArrayList();

    private List configOptions = new ArrayList();
    
    /**
     * original serverType objects
     */
    private List sTypes = new ArrayList();
    private boolean validationErrorsFound = false;

    private String scanName = null;
    private String scanDesc = null;
    
    private ImageButtonBean scheduleTypeChange;    
        
    private String scanMethod;

    //-------------------------------------constructors

    public PlatformAutoDiscoveryForm() {
        super();
        this.scheduleTypeChange = new ImageButtonBean();
    }
    /**
     * list of selected server types
     */    
    private Integer[] selectedServerTypeIds = {}; 

    public String getName() {return scanName;}
    public void   setName(String name) {scanName = name;}
    public String getDescription() {return scanDesc;}
    public void   setDescription(String desc) {scanDesc = desc;}

    public Integer[] getSelectedServerTypeIds() { 
      return this.selectedServerTypeIds; 
    } 
    
    public void setSelectedServerTypeIds(Integer[] selectedServerTypes) { 
      this.selectedServerTypeIds = selectedServerTypes; 
    }

    public boolean isScheduleTypeChgSelected() {
        return getScheduleTypeChange().isSelected();
    }
    
    /**
     * @return Collection of LabelValueBean representing the ServerTypeValue
     */
    public Collection getServerTypes() { return serverTypesLB; }

    /**
     * build up a list of label beans of serverType.names as label and
     * server type id as value
     */
    public void setServerTypes(AppdefResourceTypeValue[] serverTypes) {

        this.sTypes = new ArrayList();
        
        CollectionUtils.addAll(sTypes, serverTypes);
        
        for(int i = 0; i< serverTypes.length; i++) {
            AppdefResourceTypeValue stype = serverTypes[i];
            
            if (stype != null) {
                LabelValueBean val = new 
                    LabelValueBean(stype.getName(), 
                                   stype.getId().toString());
                serverTypesLB.add(val);
            }
        }
    }

    /**
     * @return an initial current time to 5 minutes in the future
     */    
    protected Calendar getInitStartTime() {
        Calendar cal = Calendar.getInstance();
        long currTime = cal.getTimeInMillis() + Constants.FIVE_MINUTES;
        cal.setTimeInMillis(currTime);
        return cal;        
    }

    /**
     * build a list of ServerTypeValue objects from a list of
     * ids selected in the form
     */
    public List getSelectedServerTypes(ServerTypeValue[] serverTypeVals) {
        List selectServerTypeVals = new ArrayList();
        for (int i = 0; i < selectedServerTypeIds.length; i++) {
            AppdefResourceTypeValue sType = findResourceTypeValue(serverTypeVals, selectedServerTypeIds[i]);
            if (sType != null)
                selectServerTypeVals.add(sType);
        }
        return selectServerTypeVals; 
    }

    /**
     * find a ServerTypeValue object from a list of serverTypeVals. 
     */
    public static AppdefResourceTypeValue findResourceTypeValue
        (ServerTypeValue[] serverTypeVals,
         Integer id) {
        for (int i = 0; i < serverTypeVals.length; i++) {
            if (serverTypeVals[i].getId().intValue() == id.intValue())
                return serverTypeVals[i];
        }
        return null;
    }
    
    /**
     * select all the server types for creating new auto-discovery
     */
    public void checkServerTypes(List sTypes1) {
        // if errors found, do nothing; reset the errorsFound flag
        if (validationErrorsFound) return;
        
        selectedServerTypeIds = new Integer[sTypes1.size()];
        Iterator sIterator = sTypes1.iterator();
        int i = 0;
        while (sIterator.hasNext())
        {
            ServerTypeValue sType = (ServerTypeValue)sIterator.next();
            selectedServerTypeIds[i++] = sType.getId();
        }
    }

    private Integer[] serverTypeId;
    public Integer[] getServerTypeId() { return serverTypeId; }
    public void setServerTypeId(Integer[] serverTypeId) {
        this.serverTypeId = serverTypeId;
    }

    public ArrayList getServerTypesLB() { return serverTypesLB; }
    public void setServerTypesLB(ArrayList serverTypesLB) {
        this.serverTypesLB = serverTypesLB;
    }

    /**
     * This validate method tries to
     *   
     * over-ride the validate method.  
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = super.validate(mapping, request);

        if (shouldValidate(mapping, request)) 
        {            
            if (errors == null) {
                errors = new ActionErrors();
            }

            // only validate when user clicks on ok & not on reset or cancel.
            HttpSession session = request.getSession();
            ConfigSchema schema = (ConfigSchema)session.getAttribute(Constants.CURR_CONFIG_SCHEMA);
            ConfigResponse resp = (ConfigResponse)session.getAttribute(Constants.OLD_CONFIG_RESPONSE);
            if (schema != null)
            {
                ConfigResponse newResponse = null;
                try {
                    newResponse = BizappUtils.buildSaveConfigOptions(request, 
                                                                        resp, schema, errors);
                } catch (InvalidOptionValsFoundException e) {
                    newResponse = e.getConfigResponse();
                    // do nothing. We are already handling this error by passing
                    // the error object
                } catch (InvalidOptionException e) {
                    throw new SystemException("unknown exception:", e);
                } catch (InvalidOptionValueException e) {
                    throw new SystemException("unknown exception:", e);
                }
                
                List configs = BizappUtils.buildLoadConfigOptions(schema, newResponse ); 
                this.configOptions = configs;
            }
        }

        if (errors != null && !errors.isEmpty() )
            validationErrorsFound = true;
        
        return errors;
    }

    public List getConfigOptions() {
        return configOptions;
    }

    public Integer getConfigOptionsCount() {
        if (configOptions == null)
            return new Integer(0);
        return new Integer(configOptions.size());
    }

    public void buildConfigOptions(ConfigSchema schema, ConfigResponse resp) {
        // if errors found, do nothing; reset the errorsFound flag
        if (validationErrorsFound) return;
            
        List configs = BizappUtils.buildLoadConfigOptions(schema, resp );
        
        this.configOptions = configs;
    }

    public String getScanMethod() { return scanMethod; }
    public void setScanMethod(String name) {scanMethod = name;}

    public ImageButtonBean getScheduleTypeChange() {
        return scheduleTypeChange;
    }

    public void setScheduleTypeChange(ImageButtonBean bean) {
        scheduleTypeChange = bean;
    }
}
