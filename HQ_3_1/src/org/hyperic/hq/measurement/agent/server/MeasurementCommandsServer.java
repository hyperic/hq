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

package org.hyperic.hq.measurement.agent.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.hyperic.hq.agent.AgentAPIInfo;
import org.hyperic.hq.agent.AgentAssertionException;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.PropertyPair;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.agent.server.AgentNotificationHandler;
import org.hyperic.hq.agent.server.AgentRunningException;
import org.hyperic.hq.agent.server.AgentServerHandler;
import org.hyperic.hq.agent.server.AgentStartException;
import org.hyperic.hq.agent.server.AgentStorageException;
import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.agent.CommandsAPIInfo;
import org.hyperic.hq.bizapp.client.MeasurementCallbackClient;
import org.hyperic.hq.bizapp.client.StorageProviderFetcher;
import org.hyperic.hq.measurement.server.session.SRN;
import org.hyperic.hq.measurement.agent.MeasurementCommandsAPI;
import org.hyperic.hq.measurement.agent.ScheduledMeasurement;
import org.hyperic.hq.measurement.agent.commands.DeleteProperties_args;
import org.hyperic.hq.measurement.agent.commands.DeleteProperties_result;
import org.hyperic.hq.measurement.agent.commands.GetMeasurements_args;
import org.hyperic.hq.measurement.agent.commands.GetMeasurements_result;
import org.hyperic.hq.measurement.agent.commands.ScheduleMeasurements_args;
import org.hyperic.hq.measurement.agent.commands.ScheduleMeasurements_metric;
import org.hyperic.hq.measurement.agent.commands.ScheduleMeasurements_result;
import org.hyperic.hq.measurement.agent.commands.SetProperties_args;
import org.hyperic.hq.measurement.agent.commands.SetProperties_result;
import org.hyperic.hq.measurement.agent.commands.TrackPluginAdd_args;
import org.hyperic.hq.measurement.agent.commands.TrackPluginAdd_result;
import org.hyperic.hq.measurement.agent.commands.TrackPluginRemove_args;
import org.hyperic.hq.measurement.agent.commands.TrackPluginRemove_result;
import org.hyperic.hq.measurement.agent.commands.UnscheduleMeasurements_args;
import org.hyperic.hq.measurement.agent.commands.UnscheduleMeasurements_result;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.product.ConfigTrackPluginManager;
import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.LogTrackPluginManager;
import org.hyperic.hq.product.MeasurementPluginManager;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MeasurementCommandsServer 
    implements AgentServerHandler, AgentNotificationHandler {
    private static final long THREAD_JOIN_WAIT = 10 * 1000;

    private MeasurementCommandsAPI   verAPI;         // Common API specifics
    private Thread                   scheduleThread; // Thread of scheduler
    private Thread                   availThread;    // Thread for platform avail
    private ScheduleThread           scheduleObject; // Our scheduler
    private ScheduleThread           availObject;    // Scheduler for avail
    private Thread                   senderThread;   // Thread of sender
    private SenderThread             senderObject;   // Our sender
    private AgentStorageProvider     storage;        // Agent storage
    private HashMap                  validProps;     // Hash of valid props
    private AgentConfig        bootConfig;     // Agent boot config
    private MeasurementSchedule      schedStorage;   // Schedule storage
    private MeasurementPluginManager pluginManager;  // Plugin manager
    private Log                      log;            // Our log

    // Config and Log track
    private ConfigTrackPluginManager ctPluginManager;
    private LogTrackPluginManager    ltPluginManager;
    private Thread                   trackerThread;  // Config and Log tracker thread
    private TrackerThread            trackerObject;  // Config and Log tracker object

    public MeasurementCommandsServer(){
        this.verAPI         = new MeasurementCommandsAPI();
        this.scheduleThread = null;
        this.availThread    = null;
        this.scheduleObject = null;
        this.availObject    = null;
        this.senderThread   = null;
        this.senderObject   = null;
        this.storage        = null;
        this.validProps     = new HashMap();
        this.bootConfig     = null;
        this.schedStorage   = null;
        this.pluginManager  = null;
        this.log            = LogFactory.getLog(MeasurementCommandsServer.class);

        this.ctPluginManager  = null;
        this.ltPluginManager  = null;
        this.trackerThread    = null;
        this.trackerObject    = null;
        
        for(int i=0; i<this.verAPI.propSet.length; i++){
            // Simply setup true object values for properties we know about
            this.validProps.put(this.verAPI.propSet[i], this);
        }
    }

    private void spawnThreads()
        throws AgentStartException 
    {
        this.senderObject   
            = new SenderThread(this.bootConfig.getBootProperties(),
                               this.storage, this.schedStorage);
        this.scheduleObject = new ScheduleThread(this.senderObject, 
                                                 this.pluginManager);

        this.availObject = new ScheduleThread(this.senderObject,
                                              this.pluginManager);

        this.trackerObject =
            new TrackerThread(this.ctPluginManager,
                              this.ltPluginManager,
                              this.storage,
                              this.bootConfig.getBootProperties());

        this.senderThread   = new Thread(this.senderObject, "SenderThread");
        senderThread.setDaemon(true);
        this.scheduleThread = new Thread(this.scheduleObject,"ScheduleThread");
        scheduleThread.setDaemon(true);
        this.availThread = new Thread(this.availObject, "AvailScheduleThread");
        availThread.setDaemon(true);
        this.trackerThread = new Thread(this.trackerObject, "TrackerThread");
        this.trackerThread.setDaemon(true);

        this.senderThread.start();
        this.scheduleThread.start();
        this.availThread.start();
        this.trackerThread.start();
    }

    public AgentAPIInfo getAPIInfo(){
        return this.verAPI;
    }

    public String[] getCommandSet(){
        return MeasurementCommandsAPI.commandSet;
    }

    /**
     * Schedule measurements for collection.  
     */
    private ScheduleMeasurements_result 
        scheduleMeasurements(ScheduleMeasurements_args args)
    {
        AppdefEntityID ent;
        int nMeas = args.getNumMeasurements();
        SRN srn;

        srn = args.getSRN();
        ent = srn.getEntity();

        this.log.debug("Scheduling " + nMeas + " metrics for " + ent + 
                       ": new SRN = " + srn.getRevisionNumber());
        try {
            unscheduleMeasurements(ent);
            this.schedStorage.deleteMeasurements(ent);
        } catch(UnscheduledItemException exc){
            // OK to ignore
        } catch(AgentStorageException exc){
            this.log.error("Unable to remove metrics for entity " + ent + 
                           " from storage: " + exc.getMessage());
        }

        for(int i=0; i<nMeas; i++){
            ScheduleMeasurements_metric metric = args.getMeasurement(i);
            ScheduledMeasurement sMetric;

            sMetric = new ScheduledMeasurement(metric.getDSN(), 
                                               metric.getInterval(),
                                               metric.getDerivedID(),
                                               metric.getDSNID(),
                                               ent,
                                               metric.getCategory());
            try {
                this.schedStorage.storeMeasurement(sMetric);
            } catch(AgentStorageException exc){
                this.log.debug("Failed to put measurement in storage: " + 
                               exc.getMessage());
            }
            scheduleMeasurement(sMetric);
        }

        try {
            this.schedStorage.updateSRN(srn);
        } catch(AgentStorageException exc){
            this.log.error("Unable to update SRN in storage: " + 
                           exc.getMessage());
        }
        return new ScheduleMeasurements_result();
    }

    private UnscheduleMeasurements_result 
      unscheduleMeasurements(UnscheduleMeasurements_args args)
        throws AgentRemoteException 
    {
        UnscheduledItemException resExc = null;
        AppdefEntityID failedEnt = null;
        int i, nEnts = args.getNumEntities();

        this.log.debug("Received unschedule request for " + nEnts + 
                       " resources");
        for (i = 0; i < nEnts; i++) {
            AppdefEntityID ent = args.getEntity(i);

            this.log.debug("Deleting metrics for " + ent);

            try {
                try {
                    unscheduleMeasurements(ent);
                    this.schedStorage.removeSRN(ent);
                } catch(UnscheduledItemException exc){
                    resExc    = exc;
                    failedEnt = ent;
                }

                if(resExc == null){
                        this.schedStorage.deleteMeasurements(ent);
                }
            } catch(AgentStorageException exc){
                this.log.error("Failed to delete measurement from storage");
            }
        }
        
        if(resExc != null){
            throw new AgentRemoteException("Failed to unschedule metrics " +
                                           "for entity " + failedEnt + ": " +
                                           resExc.getMessage());
        }

        return new UnscheduleMeasurements_result();
    }

    private GetMeasurements_result 
        getMeasurements(GetMeasurements_args args)
    {
        GetMeasurements_result res = new GetMeasurements_result();
        int i, nArgs = args.getNumMeasurements();
        
        for(i=0; i<nArgs; i++){
            Exception tExc = null;
            String arg = args.getMeasurement(i);
            String excMsg = null;

            this.log.debug("Getting real time measurement: " + arg);
            try {
                MetricValue val;

                val = this.pluginManager.getValue(arg);
                res.addMeasurement(val);
                this.log.debug("Result was: " + val);
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
                    
                if(this.log.isDebugEnabled()){
                    this.log.debug("Error getting real time measurement '" + 
                                   arg + "': " + excMsg, tExc);
                } else {
                    this.log.error("Error getting real time measurement: " +
                                   excMsg);
                }
                res.addException(excMsg);
            }
        }
        return res;
    }

    private SetProperties_result setProperties(SetProperties_args args)
        throws AgentRemoteException 
    {
        int i, nProps = args.getNumProperties();
        Vector tmpVec = new Vector();

        // Validate all properties before doing any assigning
        for(i=0; i<nProps; i++){
            PropertyPair pp = args.getProperty(i);

            if(this.validProps.get(pp.getName()) == null)
                throw new AgentRemoteException("Unknown measurement " +
                                               "property name, '" + 
                                               pp.getName() +"'");
            tmpVec.add(pp);
        }

        for(i=0; i<nProps; i++){
            PropertyPair pp = (PropertyPair) tmpVec.get(i);
            this.storage.setValue(pp.getName(), pp.getValue());
        }
        
        return new SetProperties_result();
    }

    private DeleteProperties_result 
      deleteProperties(DeleteProperties_args args)
        throws AgentRemoteException 
    {
        int i, nProps = args.getNumProperties();

        // Validate all properties before doing any deleting
        for(i=0; i<nProps; i++){
            if(this.validProps.get(args.getPropertyName(i)) == null)
                throw new AgentRemoteException("Unknown measurement " +
                                               "property name, '" + 
                                               args.getPropertyName(i) +"'");
        }

        for(i=0; i<nProps; i++){
            this.storage.setValue(args.getPropertyName(i), null);
        }
        return new DeleteProperties_result();
    }

    private TrackPluginAdd_result
        trackPluginAdd(TrackPluginAdd_args args)
        throws AgentRemoteException
    {
        ConfigResponse config = args.getConfigResponse();
        String name = args.getName();
        String resourceName = args.getResourceName();
        String type = args.getType();
        PluginManager manager;
        GenericPlugin plugin;
        boolean isUpdate;
        
        if (type.equals(ProductPlugin.TYPE_LOG_TRACK)) {
            manager = this.ltPluginManager;
        }
        else if (type.equals(ProductPlugin.TYPE_CONFIG_TRACK)) {
            manager = this.ctPluginManager;
        }
        else {
            throw new AgentRemoteException("Unknown plugin type=" + type);
        }
        
        try {
            plugin = manager.getPlugin(name);
            isUpdate = true;
        } catch (PluginNotFoundException e) {
            plugin = null;
            isUpdate = false;
        }

        try {
            this.log.info((isUpdate ? "Updating" : "Creating") +
                          " " + manager.getName() +
                          " plugin " + name +
                          " [" + resourceName + "]");
            if (isUpdate) {
                manager.updatePlugin(plugin, config);
            }
            else {
                manager.createPlugin(name, resourceName, config);
            }
        } catch (PluginNotFoundException e) {
            // XXX: for now
            this.log.error(e.getMessage());
        } catch (PluginExistsException e) {
            this.log.error(e.getMessage());
        } catch (PluginException e) {
            this.log.error(e.getMessage(), e);
            throw new AgentRemoteException(e.getMessage());
        }
        return new TrackPluginAdd_result();
    }

    private TrackPluginRemove_result
        trackPluginRemove(TrackPluginRemove_args args)
        throws AgentRemoteException
    {
        String name = args.getName();
        String type = args.getType();

        try {
            if (type.equals(ProductPlugin.TYPE_LOG_TRACK))
                this.ltPluginManager.removePlugin(name);
            else if (type.equals(ProductPlugin.TYPE_CONFIG_TRACK))
                this.ctPluginManager.removePlugin(name);
            else
                throw new AgentRemoteException("Unknown plugin type");
        } catch (PluginNotFoundException e) {
            // Ok if the plugin no longer exists.
        } catch (PluginException e) {
            throw new AgentRemoteException(e.getMessage());
        }
        return new TrackPluginRemove_result();
    }

    public AgentRemoteValue dispatchCommand(String cmd, AgentRemoteValue args,
                                            InputStream in, OutputStream out)
        throws AgentRemoteException 
    {
        if(cmd.equals(this.verAPI.command_scheduleMeasurements)){
            ScheduleMeasurements_args sa = 
                new ScheduleMeasurements_args(args);

            return this.scheduleMeasurements(sa);
        } else if(cmd.equals(this.verAPI.command_unscheduleMeasurements)){
            UnscheduleMeasurements_args sa = 
                new UnscheduleMeasurements_args(args);

            return this.unscheduleMeasurements(sa);
        } else if(cmd.equals(this.verAPI.command_getMeasurements)){
            GetMeasurements_args sa = 
                new GetMeasurements_args(args);

            return this.getMeasurements(sa);
        } else if(cmd.equals(this.verAPI.command_setProperties)){
            SetProperties_args sa = 
                new SetProperties_args(args);

            return this.setProperties(sa);
        } else if(cmd.equals(this.verAPI.command_deleteProperties)){
            DeleteProperties_args sa = 
                new DeleteProperties_args(args);

            return this.deleteProperties(sa);
        } else if(cmd.equals(this.verAPI.command_trackAdd)) {
            TrackPluginAdd_args ta = new TrackPluginAdd_args(args);

            return this.trackPluginAdd(ta);
        } else if(cmd.equals(this.verAPI.command_trackRemove)) {
            TrackPluginRemove_args ta = new TrackPluginRemove_args(args);

            return this.trackPluginRemove(ta);
        } else {
            throw new AgentRemoteException("Unknown command: " + cmd);
        }
    }

    public void startup(AgentDaemon agent)
        throws AgentStartException 
    {
        Iterator i;

        try {
            this.storage      = agent.getStorageProvider();
            this.bootConfig   = agent.getBootConfig();
            this.schedStorage = new MeasurementSchedule(this.storage);
        } catch(AgentRunningException exc){
            throw new AgentAssertionException("Agent should be running here");
        }

        try {
            this.pluginManager =
                (MeasurementPluginManager)agent.
                getPluginManager(ProductPlugin.TYPE_MEASUREMENT);
            this.ctPluginManager =
                (ConfigTrackPluginManager)agent.
                getPluginManager(ProductPlugin.TYPE_CONFIG_TRACK);
            this.ltPluginManager =
                (LogTrackPluginManager)agent.
                getPluginManager(ProductPlugin.TYPE_LOG_TRACK);

        } catch (Exception e) {
            throw new AgentStartException("Unable to get measurement " +
                                          "plugin manager: " + 
                                          e.getMessage());
        }

        spawnThreads();

        i = this.schedStorage.getMeasurementList();
        while(i.hasNext()){
            ScheduledMeasurement meas = (ScheduledMeasurement)i.next();
            scheduleMeasurement(meas);
        }

        agent.registerMonitor("camMetric.schedule", this.scheduleObject);
        agent.registerMonitor("camMetric.sender", this.senderObject);

        // If we have don't have a provider, register a handler until
        // we get one
        if(CommandsAPIInfo.getProvider(this.storage) == null){
            agent.registerNotifyHandler(this,
                                        CommandsAPIInfo.NOTIFY_SERVER_SET);
        } else {
            this.startConfigPopulator();
        }

        this.log.info("Measurement Commands Server started up");
    }

    private void scheduleMeasurement(ScheduledMeasurement m) {
        // Temporary workaround for delayed availability measurements for
        // platforms.  Collect them in their own thread.
        if (m.getEntity().isPlatform() &&
            m.getDSN().endsWith("system.avail:Type=Platform:Availability")) {
            this.availObject.scheduleMeasurement(m);
        } else {
            this.scheduleObject.scheduleMeasurement(m);
        }
    }

    private void unscheduleMeasurements(AppdefEntityID id)
        throws UnscheduledItemException
    {
        this.scheduleObject.unscheduleMeasurements(id);
        this.availObject.unscheduleMeasurements(id);
    }

    public void handleNotification(String msgClass, String msg){
        this.startConfigPopulator();
    }

    private void startConfigPopulator(){
        StorageProviderFetcher fetcher
            = new StorageProviderFetcher(this.storage);
        MeasurementCallbackClient client
            = new MeasurementCallbackClient(fetcher);
        ConfigPopulateThread populator
            = new ConfigPopulateThread(client, this.ltPluginManager,
                                       this.ctPluginManager);
        populator.setDaemon(true);
        populator.start();
    }

    private void interruptThread(Thread t)
        throws InterruptedException
    {
        if(t.isAlive()){
            t.interrupt();
            t.join(THREAD_JOIN_WAIT);
            
            if(t.isAlive()){
                this.log.warn(t.getName() + " did not die within the " +
                              "timeout period.  Killing it");
                t.stop();
            }
        }
    }

    public void shutdown(){
        this.log.info("Measurement Commands Server shutting down");
        this.scheduleObject.die();
        this.availObject.die();
        this.senderObject.die();

        try {
            this.interruptThread(this.senderThread);
            this.interruptThread(this.scheduleThread);
            this.interruptThread(this.availThread);
        } catch(InterruptedException exc){
            // Someone wants us to die badly .... ok 
            this.log.warn("shutdown interrupted");
        }

        this.log.info("Measurement Commands Server shut down");
    }
}
