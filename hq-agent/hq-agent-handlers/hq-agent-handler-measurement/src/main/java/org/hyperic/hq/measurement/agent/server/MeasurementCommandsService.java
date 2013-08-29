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

package org.hyperic.hq.measurement.agent.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.PropertyPair;
import org.hyperic.hq.agent.server.AgentStorageException;
import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.measurement.agent.ScheduledMeasurement;
import org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient;
import org.hyperic.hq.measurement.agent.commands.DeleteProperties_args;
import org.hyperic.hq.measurement.agent.commands.GetMeasurements_args;
import org.hyperic.hq.measurement.agent.commands.GetMeasurements_result;
import org.hyperic.hq.measurement.agent.commands.ScheduleMeasurements_args;
import org.hyperic.hq.measurement.agent.commands.ScheduleMeasurements_metric;
import org.hyperic.hq.measurement.agent.commands.ScheduleTopn_args;
import org.hyperic.hq.measurement.agent.commands.SetProperties_args;
import org.hyperic.hq.measurement.agent.commands.TrackPluginAdd_args;
import org.hyperic.hq.measurement.agent.commands.TrackPluginRemove_args;
import org.hyperic.hq.measurement.agent.commands.UnscheduleMeasurements_args;
import org.hyperic.hq.measurement.server.session.SRN;
import org.hyperic.hq.product.ConfigTrackPluginManager;
import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.LogTrackPluginManager;
import org.hyperic.hq.product.MeasurementPluginManager;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginExistsException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.schedule.UnscheduledItemException;
import org.hyperic.util.timer.StopWatch;

/**
 * The Measurement Commands service.
 */
public class MeasurementCommandsService implements MeasurementCommandsClient {
    
    private static final Log _log = LogFactory.getLog(MeasurementCommandsService.class);
    
    private final AgentStorageProvider _storage;
    private final Map _validProps;
    private final MeasurementSchedule _schedStorage;
    private final MeasurementPluginManager _pluginManager;
    private final LogTrackPluginManager _ltPluginManager;
    private final ConfigTrackPluginManager _ctPluginManager;
    private final ScheduleThread _scheduleObject;
    private final TopNScheduler _topNScheduler;
    private final LinkedBlockingQueue<ScheduleMeasurements_args> argQueue =
        new LinkedBlockingQueue<ScheduleMeasurements_args>();
    private final Scheduler _scheduler;
    
    public MeasurementCommandsService(AgentStorageProvider storage, 
                                      Map validProps, 
                                      MeasurementSchedule schedStorage, 
                                      MeasurementPluginManager pluginManager, 
                                      LogTrackPluginManager ltPluginManager, 
                                      ConfigTrackPluginManager ctPluginManager, 
 ScheduleThread scheduleObject, TopNScheduler topNScheduler) {
        _storage = storage;
        _validProps = validProps;
        _schedStorage = schedStorage;
        _pluginManager = pluginManager;
        _ltPluginManager = ltPluginManager;
        _ctPluginManager = ctPluginManager;
        _scheduleObject = scheduleObject;
        _topNScheduler = topNScheduler;
        _scheduler = new Scheduler();
        _scheduler.start();
    }
    
    /**
     * @see org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient#addTrackPlugin(java.lang.String, java.lang.String, java.lang.String, org.hyperic.util.config.ConfigResponse)
     */
    public void addTrackPlugin(String id, 
                               String pluginType,
                               String resourceName, 
                               ConfigResponse response) 
        throws AgentRemoteException {
        
        PluginManager manager;
        
        if (pluginType.equals(ProductPlugin.TYPE_LOG_TRACK)) {
            manager = _ltPluginManager;
        }
        else if (pluginType.equals(ProductPlugin.TYPE_CONFIG_TRACK)) {
            manager = _ctPluginManager;
        }
        else {
            throw new AgentRemoteException("Unknown plugin type=" + pluginType);
        }
        
        GenericPlugin plugin;
        boolean isUpdate;
        
        try {
            plugin = manager.getPlugin(id);
            isUpdate = true;
        } catch (PluginNotFoundException e) {
            plugin = null;
            isUpdate = false;
        }
        
        try {
            _log.info((isUpdate ? "Updating" : "Creating") +
                          " " + manager.getName() +
                          " plugin " + id +
                          " [" + resourceName + "]");
            if (isUpdate) {
                manager.updatePlugin(plugin, response);
            }
            else {
                manager.createPlugin(id, resourceName, response);
            }
        } catch (PluginNotFoundException e) {
            _log.error(e.getMessage(), e);
        } catch (PluginExistsException e) {
            _log.error(e.getMessage(), e);
        } catch (PluginException e) {
            _log.error(e.getMessage(), e);
            throw new AgentRemoteException(e.getMessage());
        }

    }
    
    void addTrackPlugin(TrackPluginAdd_args ta) throws AgentRemoteException {
        String id = ta.getName();
        String pluginType = ta.getType();
        String resourceName = ta.getResourceName();
        ConfigResponse response = ta.getConfigResponse();
        addTrackPlugin(id, pluginType, resourceName, response);
    }

    /**
     * @see org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient#deleteProperties(org.hyperic.hq.measurement.agent.commands.DeleteProperties_args)
     */
    public void deleteProperties(DeleteProperties_args args) 
        throws AgentRemoteException {
        
        int i, nProps = args.getNumProperties();

        // Validate all properties before doing any deleting
        for(i=0; i<nProps; i++){
            if(_validProps.get(args.getPropertyName(i)) == null) {
                throw new AgentRemoteException("Unknown measurement " +
                                               "property name, '" + 
                                               args.getPropertyName(i) +"'");
            }
        }

        for(i=0; i<nProps; i++){
            _storage.setValue(args.getPropertyName(i), null);
        }
    }

    /**
     * @see org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient#getMeasurements(org.hyperic.hq.measurement.agent.commands.GetMeasurements_args)
     */
    public GetMeasurements_result getMeasurements(GetMeasurements_args args) 
        throws AgentRemoteException {
        
        GetMeasurements_result res = new GetMeasurements_result();
        int i, nArgs = args.getNumMeasurements();
        boolean isDebug = _log.isDebugEnabled();

        _log.debug("Getting real time measurements");
        
        for(i=0; i<nArgs; i++){
            Exception tExc = null;
            String arg = args.getMeasurement(i);
            String excMsg = null;
            String tmpl = arg.substring(arg.indexOf(':')+1);

            try {
                MetricValue val;

                val = _pluginManager.getValue(arg);
                res.addMeasurement(val);
                if(isDebug){
                    tmpl = Metric.parse(tmpl).toDebugString();
                    _log.debug("Getting real time measurement: " + tmpl);
                    _log.debug("Result was: " + val);
                }
            } catch(PluginNotFoundException exc){
                excMsg = "Plugin not found: ";
                tExc = exc;
            } catch(PluginException exc){
                excMsg = "Plugin error: ";
                tExc = exc;
            } catch(MetricInvalidException exc){
                excMsg = "Invalid request: ";
                tExc = exc;
            } catch(MetricNotFoundException exc){
                excMsg = "Error retrieving value: ";
                tExc = exc;
            } catch(MetricUnreachableException exc){
                excMsg = "Error contacting resource: ";
                tExc = exc;
            }

            if(excMsg != null){
                if(tExc.getMessage() == null){
                    excMsg = excMsg + tExc;
                } else {
                    excMsg = excMsg + tExc.getMessage();
                }
                    
                if(isDebug){
                    tmpl = Metric.parse(tmpl).toDebugString();
                    _log.debug("Error getting real time measurement '" + 
                               tmpl + "': " + excMsg, tExc);
                } else {
                    _log.error("Error getting real time measurement: " +
                                   excMsg);
                }
                res.addException(excMsg);
            }
        }

        _log.debug("Finished getting real time measurements");
        
        return res;
    }

    /**
     * @see org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient#removeTrackPlugin(java.lang.String, java.lang.String)
     */
    public void removeTrackPlugin(String id, String pluginType) 
        throws AgentRemoteException {

        try {
            if (pluginType.equals(ProductPlugin.TYPE_LOG_TRACK)) {
                _ltPluginManager.removePlugin(id);
            } else if (pluginType.equals(ProductPlugin.TYPE_CONFIG_TRACK)) {
                _ctPluginManager.removePlugin(id);
            } else {
                throw new AgentRemoteException("Unknown plugin type");
            }
        } catch (PluginNotFoundException e) {
            // Ok if the plugin no longer exists.
        } catch (PluginException e) {
            throw new AgentRemoteException(e.getMessage());
        }

    }
    
    void removeTrackPlugin(TrackPluginRemove_args ta) throws AgentRemoteException {
        String id = ta.getName();
        String pluginType = ta.getType();
        removeTrackPlugin(id, pluginType);
    }

    
    /**
     * @see org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient#scheduleMeasurements(org.hyperic.hq.measurement.agent.commands.ScheduleMeasurements_args)
     */
    public void scheduleMeasurements(ScheduleMeasurements_args args) throws AgentRemoteException {
        argQueue.add(args);
    }
    
    private class Scheduler extends Thread {
        final AtomicBoolean shutdown = new AtomicBoolean(false);
        private Scheduler() {
            super("MeasurementSchedulerThread");
            setDaemon(true);
        }
        @Override
        public void run() {
            try {
                while (!shutdown.get()) {
                    try {
                        final List<ScheduleMeasurements_args> list = new ArrayList<ScheduleMeasurements_args>();
                        argQueue.drainTo(list);
                        if (!list.isEmpty()) {
                            scheduleMeasurements(list);
                        }
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        _log.debug(e,e);
                    }
                }
            } catch (Throwable t) {
                _log.error(t,t);
            }
        }
        private void shutdown() {
            shutdown.set(true);
        }
    }

    private void scheduleMeasurements(Collection<ScheduleMeasurements_args> args) throws AgentRemoteException {
        final Map<AppdefEntityID, SRN> aeids = new HashMap<AppdefEntityID, SRN>();
        final StopWatch watch = new StopWatch();
        final List<ScheduledMeasurement> metrics = new ArrayList<ScheduledMeasurement>(args.size());
        final boolean debug = _log.isDebugEnabled();
        if (debug) {
            watch.markTimeBegin("unscheduleMeasurements");
        }
        for (final ScheduleMeasurements_args arg : args) {
            final SRN srn = arg.getSRN();
            final AppdefEntityID aeid = srn.getEntity();
            aeids.put(aeid, srn);
            try {
                unscheduleMeasurements(aeid);
            } catch (UnscheduledItemException e) {
                // ok to ignore
                _log.debug(e,e);
            }
        }
        if (debug) {
            watch.markTimeEnd("unscheduleMeasurements");
        }
        if (debug) {
            watch.markTimeBegin("deleteMeasurements");
        }
        try {
            _schedStorage.deleteMeasurements(aeids.keySet());
        } catch(AgentStorageException exc){
            _log.error("Failed to delete from measurement storage: " +  exc, exc);
        }
        if (debug) {
            watch.markTimeEnd("deleteMeasurements");
        }
        // this looks weird since the loops are so similar, but they can't be consolidated unless the entire
        // flow is reworked down to the srn
        if (debug) {
            watch.markTimeBegin("updateSRN");
        }
        for (final ScheduleMeasurements_args arg : args) {
            int numMeasurements = arg.getNumMeasurements();
            final SRN srn = arg.getSRN();
            final AppdefEntityID aeid = srn.getEntity();
            for (int i=0; i<numMeasurements; i++){
                final ScheduleMeasurements_metric metric = arg.getMeasurement(i);
                final ScheduledMeasurement sMetric =
                    new ScheduledMeasurement(metric.getDSN(), 
                                             metric.getInterval(),
                                             metric.getDerivedID(),
                                             metric.getDSNID(),
                                             aeid, metric.getCategory());
                metrics.add(sMetric);
            }
            try {
                _schedStorage.updateSRN(srn);
            } catch(AgentStorageException e){
                _log.error("Unable to update SRN in storage: " + e, e);
            }
        }
        if (debug) {
            watch.markTimeEnd("updateSRN");
        }
        if (debug) {
            watch.markTimeBegin("storeMeasurements");
        }
        try {
            _schedStorage.storeMeasurements(metrics);
        } catch(AgentStorageException exc){
            _log.error("Failed to store measurements: " +  exc, exc);
        }
        if (debug) {
            watch.markTimeEnd("storeMeasurements");
        }
        if (debug) {
            watch.markTimeBegin("scheduleMeasurement");
        }
        for (ScheduledMeasurement m : metrics) {
            _scheduleObject.scheduleMeasurement(m);
        }
        if (debug) {
            watch.markTimeEnd("scheduleMeasurement");
        }
        if (debug) {
            _log.debug("scheduled " + metrics.size() + " measurements, " + watch);
        }
    }
    
    void scheduleMeasurement(ScheduledMeasurement m) {
        _scheduleObject.scheduleMeasurement(m);
    }

    /**
     * @see org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient#setProperties(org.hyperic.hq.measurement.agent.commands.SetProperties_args)
     */
    public void setProperties(SetProperties_args args) throws AgentRemoteException {
        int i, nProps = args.getNumProperties();
        Vector tmpVec = new Vector();

        // Validate all properties before doing any assigning
        for(i=0; i<nProps; i++){
            PropertyPair pp = args.getProperty(i);

            if(_validProps.get(pp.getName()) == null) {
                throw new AgentRemoteException("Unknown measurement " +
                                               "property name, '" + 
                                               pp.getName() +"'");
            }
            tmpVec.add(pp);
        }

        for(i=0; i<nProps; i++){
            PropertyPair pp = (PropertyPair) tmpVec.get(i);
            _storage.setValue(pp.getName(), pp.getValue());
        }
    }

    /**
     * @see org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient#unscheduleMeasurements(org.hyperic.hq.measurement.agent.commands.UnscheduleMeasurements_args)
     */
    public void unscheduleMeasurements(UnscheduleMeasurements_args args) 
        throws AgentRemoteException {
        
        UnscheduledItemException resExc = null;
        AppdefEntityID failedEnt = null;
        int i, nEnts = args.getNumEntities();

        _log.debug("Received unschedule request for " + nEnts + 
                       " resources");
        for (i = 0; i < nEnts; i++) {
            AppdefEntityID ent = args.getEntity(i);

            _log.debug("Deleting metrics for " + ent);

            try {
                try {
                    unscheduleMeasurements(ent);
                    _schedStorage.removeSRN(ent);
                } catch(UnscheduledItemException exc){
                    resExc    = exc;
                    failedEnt = ent;
                }

                if(resExc == null){
                    _schedStorage.deleteMeasurements(Collections.singletonMap(ent, null).keySet());
                }
            } catch(AgentStorageException exc){
                _log.error("Failed to delete measurement from storage");
            }
        }
        
        if(resExc != null){
            throw new AgentRemoteException("Failed to unschedule metrics " +
                                           "for entity " + failedEnt + ": " +
                                           resExc.getMessage());
        }
    }
    
    private void unscheduleMeasurements(AppdefEntityID id)
        throws UnscheduledItemException {
        _scheduleObject.unscheduleMeasurements(id);
    }

    public void scheduleTopn(ScheduleTopn_args args) throws AgentRemoteException {
        _topNScheduler.scheduleTopN(args);
    }

    public void unscheduleTopn() throws AgentRemoteException {
        _topNScheduler.unscheduleTopN();
    }

}
