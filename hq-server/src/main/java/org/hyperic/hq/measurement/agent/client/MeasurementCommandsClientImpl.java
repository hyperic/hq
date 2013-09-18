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

package org.hyperic.hq.measurement.agent.client;

import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.client.AbstractCommandsClient;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.measurement.agent.commands.DeleteProperties_args;
import org.hyperic.hq.measurement.agent.commands.GetMeasurements_args;
import org.hyperic.hq.measurement.agent.commands.GetMeasurements_result;
import org.hyperic.hq.measurement.agent.commands.ScheduleMeasurements_args;
import org.hyperic.hq.measurement.agent.commands.ScheduleTopn_args;
import org.hyperic.hq.measurement.agent.commands.SetProperties_args;
import org.hyperic.hq.measurement.agent.commands.UnscheduleMeasurements_args;
import org.hyperic.hq.transport.AgentProxyFactory;
import org.hyperic.util.config.ConfigResponse;

/**
 * The Measurement Commands client that uses the new transport.
 */
public class MeasurementCommandsClientImpl 
    extends AbstractCommandsClient implements MeasurementCommandsClient {

    public MeasurementCommandsClientImpl(Agent agent, AgentProxyFactory factory) {
        super(agent, factory);
    }
    
    /**
     * @see org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient#addTrackPlugin(java.lang.String, java.lang.String, java.lang.String, org.hyperic.util.config.ConfigResponse)
     */
    public void addTrackPlugin(String id, 
                               String pluginType,
                               String resourceName, 
                               ConfigResponse response)
            throws AgentRemoteException, AgentConnectionException {
        
        MeasurementCommandsClient proxy = null;
        
        try {
            proxy = (MeasurementCommandsClient)getSynchronousProxy(MeasurementCommandsClient.class);
            proxy.addTrackPlugin(id, pluginType, resourceName, response);
        } finally {
            safeDestroyService(proxy);
        }
    }

    /**
     * @see org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient#deleteProperties(org.hyperic.hq.measurement.agent.commands.DeleteProperties_args)
     */
    public void deleteProperties(DeleteProperties_args args)
            throws AgentRemoteException, AgentConnectionException {

        MeasurementCommandsClient proxy = null;
        
        try {
            proxy = (MeasurementCommandsClient)getAsynchronousProxy(MeasurementCommandsClient.class, false);
            proxy.deleteProperties(args);   
        } finally {
            safeDestroyService(proxy);
        }
    }

    /**
     * @see org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient#getMeasurements(org.hyperic.hq.measurement.agent.commands.GetMeasurements_args)
     */
    public GetMeasurements_result getMeasurements(GetMeasurements_args args)
            throws AgentRemoteException, AgentConnectionException {
        
        MeasurementCommandsClient proxy = null;
        boolean debug = _log.isDebugEnabled();
        
        try {
            if (debug) _log.debug("getMeasurements: Getting synchronous proxy");
            proxy = (MeasurementCommandsClient)getSynchronousProxy(MeasurementCommandsClient.class);
            if (debug) _log.debug("getMeasurements: Calling getMeasurements using synchronous proxy");
            return proxy.getMeasurements(args);       
        } finally {
            if (debug) _log.debug("getMeasurements: proxy.getMeasurements returned");
            safeDestroyService(proxy);
        }
    }

    /**
     * @see org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient#removeTrackPlugin(java.lang.String, java.lang.String)
     */
    public void removeTrackPlugin(String id, 
                                  String pluginType)
            throws AgentRemoteException, AgentConnectionException {
        
        MeasurementCommandsClient proxy = null;
        
        try {
            proxy = (MeasurementCommandsClient)getAsynchronousProxy(MeasurementCommandsClient.class, false);
            proxy.removeTrackPlugin(id, pluginType);    
        } finally {
            safeDestroyService(proxy);
        }
    }

    /**
     * @see org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient#scheduleMeasurements(org.hyperic.hq.measurement.agent.commands.ScheduleMeasurements_args)
     */
    public void scheduleMeasurements(ScheduleMeasurements_args args)
            throws AgentRemoteException, AgentConnectionException {
        
        MeasurementCommandsClient proxy = null;
        
        try {
            proxy = (MeasurementCommandsClient)getAsynchronousProxy(MeasurementCommandsClient.class, false);
            proxy.scheduleMeasurements(args);     
        } finally {
            safeDestroyService(proxy);
        }
    }

    /**
     * @see org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient#setProperties(org.hyperic.hq.measurement.agent.commands.SetProperties_args)
     */
    public void setProperties(SetProperties_args args)
            throws AgentRemoteException, AgentConnectionException {
        
        MeasurementCommandsClient proxy = null;
        
        try {
            proxy = (MeasurementCommandsClient)getAsynchronousProxy(MeasurementCommandsClient.class, false);
            proxy.setProperties(args);   
        } finally {
            safeDestroyService(proxy);
        }
    }

    /**
     * @see org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient#unscheduleMeasurements(org.hyperic.hq.measurement.agent.commands.UnscheduleMeasurements_args)
     */
    public void unscheduleMeasurements(UnscheduleMeasurements_args args)
            throws AgentRemoteException, AgentConnectionException {
        
        MeasurementCommandsClient proxy = null;
        
        try {
            proxy = (MeasurementCommandsClient)getSynchronousProxy(MeasurementCommandsClient.class);
            proxy.unscheduleMeasurements(args);           
        } finally {
            safeDestroyService(proxy);
        }
    }

    public void scheduleTopn(ScheduleTopn_args args) throws AgentRemoteException, AgentConnectionException {
    
        MeasurementCommandsClient proxy = null;
        
        try {
            proxy = (MeasurementCommandsClient)getAsynchronousProxy(MeasurementCommandsClient.class, false);
            proxy.scheduleTopn(args);    
        } finally {
            safeDestroyService(proxy);
        }
    
    }

    public void unscheduleTopn() throws AgentRemoteException, AgentConnectionException {      
        MeasurementCommandsClient proxy = null;
        
        try {
            proxy = (MeasurementCommandsClient)getAsynchronousProxy(MeasurementCommandsClient.class, false);
            proxy.unscheduleTopn();  
        } finally {
            safeDestroyService(proxy);
        }
    
    }

}
