/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.bizapp.server.action.integrate.OpenNMSAction;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.ui.action.resource.ResourceForm;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;


/**
 * Form for editing the OpenNMS action for an alert definition.
 *
 */
public final class OpenNMSForm extends ResourceForm  {
    private Integer _id; // nullable
    private Integer _ad; // nullable
    private boolean _shouldBeRemoved;
    private String _server;
    private String _ip;
    private String _port;

    //-------------------------------------constructors

    public String getServer() {
        return _server;
    }


    public void setServer(String server) {
        _server = server;
    }


    public String getIp() {
        return _ip;
    }


    public void setIp(String ip) {
        _ip = ip;
    }


    public String getPort() {
        return _port;
    }


    public void setPort(String port) {
        _port = port;
    }


    public OpenNMSForm() {
        // do nothing
    }

    public Integer getId() {
        return _id;
    }

    public void setId(Integer id) {
        _id = id;
    }

    public Integer getAd() {
        return _ad;
    }

    public void setAd(Integer ad) {
        _ad = ad;
    }

    public boolean isShouldBeRemoved() {
        return _shouldBeRemoved;
    }

    public void setShouldBeRemoved(boolean shouldBeRemoved) {
        _shouldBeRemoved = shouldBeRemoved;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        _shouldBeRemoved = false;
        _ad = null;
        _id = null;
        _server = null;
        _ip = null;
        _port = null;
    }
    
    void importAction(AlertDefinitionValue def)
        throws InvalidActionDataException, EncodingException {
        setAd(def.getId());
        
        // Find the OpenNMS action
        ActionValue[] actions = def.getActions();
        for (int i = 0; i < actions.length; ++i) {
            if ( actions[i].classnameHasBeenSet() &&
                 !( actions[i].getClassname().equals(null) ||
                    actions[i].getClassname().equals("") ) ) {
                try {
                    Class clazz = Class.forName(actions[i].getClassname());
                    if (OpenNMSAction.class.isAssignableFrom(clazz)) {
                        setId(actions[i].getId());

                        OpenNMSAction onms = new OpenNMSAction();
                        onms.init(ConfigResponse.decode(actions[i].getConfig()));
                        setServer(onms.getServer());
                        setIp(onms.getIp());
                        setPort(onms.getPort());

                        break;
                    }
                } catch (ClassNotFoundException e) {
                    continue;
                }
            }
        }
        
    }
}

// EOF
