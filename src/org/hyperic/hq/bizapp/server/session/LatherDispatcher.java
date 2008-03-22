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

package org.hyperic.hq.bizapp.server.session;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.AgentManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.ResourceRefreshZevent;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.AgentConnections.AgentConnection;
import org.hyperic.hq.appdef.shared.AgentCreateException;
import org.hyperic.hq.appdef.shared.AgentManagerLocal;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AgentUnauthorizedException;
import org.hyperic.hq.appdef.shared.AgentValue;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.resourceTree.PlatformNode;
import org.hyperic.hq.appdef.shared.resourceTree.ResourceTree;
import org.hyperic.hq.appdef.shared.resourceTree.ServerNode;
import org.hyperic.hq.appdef.shared.resourceTree.ServiceNode;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.ScanStateCore;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManagerLocal;
import org.hyperic.hq.bizapp.agent.client.SecureAgentConnection;
import org.hyperic.hq.bizapp.shared.lather.AiSendReport_args;
import org.hyperic.hq.bizapp.shared.lather.AiSendRuntimeReport_args;
import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
import org.hyperic.hq.bizapp.shared.lather.ControlGetPluginConfig_args;
import org.hyperic.hq.bizapp.shared.lather.ControlGetPluginConfig_result;
import org.hyperic.hq.bizapp.shared.lather.ControlSendCommandResult_args;
import org.hyperic.hq.bizapp.shared.lather.MeasurementGetConfigs_args;
import org.hyperic.hq.bizapp.shared.lather.MeasurementGetConfigs_result;
import org.hyperic.hq.bizapp.shared.lather.MeasurementSendReport_args;
import org.hyperic.hq.bizapp.shared.lather.MeasurementSendReport_result;
import org.hyperic.hq.bizapp.shared.lather.RegisterAgent_args;
import org.hyperic.hq.bizapp.shared.lather.RegisterAgent_result;
import org.hyperic.hq.bizapp.shared.lather.SecureAgentLatherValue;
import org.hyperic.hq.bizapp.shared.lather.TrackSend_args;
import org.hyperic.hq.bizapp.shared.lather.UpdateAgent_args;
import org.hyperic.hq.bizapp.shared.lather.UpdateAgent_result;
import org.hyperic.hq.bizapp.shared.lather.UserIsValid_args;
import org.hyperic.hq.bizapp.shared.lather.UserIsValid_result;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.control.shared.ControlManagerLocal;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.measurement.data.TrackEventReport;
import org.hyperic.hq.measurement.server.session.DataInserterException;
import org.hyperic.hq.measurement.shared.ConfigChangedEvent;
import org.hyperic.hq.measurement.shared.MeasurementConfigEntity;
import org.hyperic.hq.measurement.shared.MeasurementConfigList;
import org.hyperic.hq.measurement.shared.ResourceLogEvent;
import org.hyperic.hq.product.ConfigTrackPlugin;
import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.lather.LatherContext;
import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;
import org.hyperic.lather.NullLatherValue;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.security.SecurityUtil;

public class LatherDispatcher
    extends BizappSessionEJB
{
    protected final Log log = 
        LogFactory.getLog(LatherDispatcher.class.getName());

    private SessionManager  sessionManager  = SessionManager.getInstance();
    protected HashSet       secureCommands  = new HashSet();
    private Set             noTxCommands    = new HashSet();
    private final Object    tConnLock       = new Object();
    private TopicConnection tConn;
    private TopicSession    tSession;

    public LatherDispatcher(){
        for(int i=0; i<CommandInfo.SECURE_COMMANDS.length; i++){
            secureCommands.add(CommandInfo.SECURE_COMMANDS[i]);
        }
        
        for (int i=0; i<CommandInfo.NOTX_COMMANDS.length; i++) {
            noTxCommands.add(CommandInfo.NOTX_COMMANDS[i]);
        }
    }
    
    public boolean methIsTransactional(String meth) {
        return !noTxCommands.contains(meth);
    }

    private void sendTopicMessage(String msgTopic, Serializable data)
        throws LatherRemoteException
    {
        Messenger sender;

        synchronized(tConnLock){
            if(tConn == null) {
                TopicConnectionFactory factory;
                InitialContext ctx;
                
                try {
                    ctx = new InitialContext();
                    factory = (TopicConnectionFactory) 
                        ctx.lookup(Messenger.CONN_FACTORY_JNDI);
                } catch(NamingException exc){
                    log.error("Error looking up " + 
                              Messenger.CONN_FACTORY_JNDI +
                              " while sending a message to " + msgTopic, exc);
                    throw new LatherRemoteException("Unable to lookup " +
                                                    "message queue '" + 
                                                    msgTopic + "'");
                }
                
                try {
                    // Use ConnectionFactory to create JMS connection
                    tConn = factory.createTopicConnection();
                
                    // Use Connection to create session
                    tSession = tConn.createTopicSession(false, 
                                                    Session.AUTO_ACKNOWLEDGE);
                } catch(JMSException exc){
                    log.error("Error creating topic connection to '" +
                                   msgTopic + "'", exc);
                    throw new LatherRemoteException("Error creating msgTopic "+
                                                    "connection to '" + 
                                                    msgTopic + "'");
                }
            }
        }
        sender = new Messenger(tConn, tSession);
        sender.publishMessage(msgTopic, data);
    }

    protected void validateAgent(LatherContext ctx, String agentToken)
        throws LatherRemoteException
    {
        validateAgent(ctx, agentToken, true);
    }

    protected void validateAgent(LatherContext ctx, String agentToken, 
                                 boolean useCache)
        throws LatherRemoteException
    {
        log.debug("Validating agent token");
        try {
            getAgentManager().checkAgentAuth(agentToken);
        } catch (AgentUnauthorizedException exc) {
            log.warn("Unauthorized agent from " +
                ctx.getCallerIP() + " denied");
            throw new LatherRemoteException("Unauthorized agent denied");
        }
    }

    private void checkUserCanManageAgent(LatherContext ctx, String user, 
                                         String pword, String operation)
        throws PermissionException
    {
        try {
            AuthzSubject subject;
            int sessionId;

            sessionId = getAuthManager().getSessionId(user, pword);
            subject   = sessionManager.getSubjectPojo(sessionId);
            getServerManager().checkCreatePlatformPermission(subject);
        } catch(SecurityException exc){
            log.warn("Security exception when '" + user + 
                          "' tried to " + operation + " an Agent @ " +
                          ctx.getCallerIP(), exc);
            throw new PermissionException();
        } catch(LoginException exc){
            log.warn("Login exception when '" + user + 
                          "' tried to " + operation + " an Agent @ " +
                          ctx.getCallerIP() + ":  " + exc.getMessage());
            throw new PermissionException();
        } catch(SessionTimeoutException exc){
            log.warn("Session timeout when '" + user + 
                          "' tried to " + operation + " an Agent @ " +
                          ctx.getCallerIP() + ":  " + exc.getMessage());
            throw new PermissionException();
        } catch(SessionNotFoundException exc){
            log.warn("Session not found when '" + user + 
                          "' tried to " + operation + " an Agent @ " +
                          ctx.getCallerIP() + ":  " + exc.getMessage());
            throw new PermissionException();
        } catch(PermissionException exc){
            log.warn("Permission denied when '" + user + 
                          "' tried to " + operation + " an Agent @ " +
                          ctx.getCallerIP());
            throw new PermissionException();
        } catch(ApplicationException exc){
            log.warn("Application exception when '" + user + 
                          "' tried to " + operation + " an Agent @ " +
                          ctx.getCallerIP(), exc);
            throw new PermissionException();
        } catch(ConfigPropertyException exc){
            log.warn("Config property exception when '" + user + 
                          "' tried to " + operation + " an Agent @ " +
                          ctx.getCallerIP(), exc);
            throw new PermissionException();
        } catch(SystemException exc){
            log.warn("System exception when '" + user + 
                          "' tried to " + operation + " an Agent @ " +
                          ctx.getCallerIP(), exc);
            throw new PermissionException();
        }
    }

    private String testAgentConn(String agentIP, int agentPort, 
                                 String authToken)
    {
        AgentCommandsClient agtCmds;
        SecureAgentConnection agentConn;
        
        agentConn = new SecureAgentConnection(agentIP, agentPort, authToken);
        agtCmds   = new AgentCommandsClient(agentConn);

        try {
            agtCmds.ping();
        } catch(AgentConnectionException exc){
            return "Failed to connect to agent: " + exc.getMessage();
        } catch(AgentRemoteException exc){
            return "Error communicating with agent: " + exc.getMessage();
        }
        
        return null;
    }

    /**
     * Register an Agent with the server.  This method calls out to verify it
     * can talk to the agent, and then adds it to the database.  With all
     * the connection info used to make further connections.
     */
    private RegisterAgent_result cmdRegisterAgent(LatherContext ctx, 
                                                  RegisterAgent_args args)
        throws LatherRemoteException
    {
        AgentValue agentVal;
        String agentToken, errRes, agentIP, version;
        int port;

        try {
            checkUserCanManageAgent(ctx, args.getUser(), args.getPword(), 
                                         "register");
        } catch(PermissionException exc){
            return new RegisterAgent_result("Permission denied");
        }

        agentIP = args.getAgentIP();
        port    = args.getAgentPort();
        version = args.getVersion();

        errRes = testAgentConn(agentIP, port, args.getAuthToken());
        if(errRes != null){
            return new RegisterAgent_result(errRes);
        }

        agentToken = SecurityUtil.generateRandomToken();

        agentVal = new AgentValue();
        agentVal.setAddress(agentIP);
        agentVal.setPort(port);
        agentVal.setVersion(version);
        agentVal.setAuthToken(args.getAuthToken());
        agentVal.setAgentToken(agentToken);

        // Check the to see if it already exists
        Collection ids = null;
        try {
            AgentValue origAgent =
                getAgentManager().getAgent(agentIP, port);
            
            try {
                ids = getPlatformManager().
                    getPlatformPksByAgentToken(getOverlord(),
                                               origAgent.getAgentToken());
            } catch (Exception e) {
                // No platforms found, no a big deal
            }

            log.info("Updating agent information for " + agentIP + ":" +
                          port);
            getAgentManager().updateAgent(agentIP, port, agentVal);
        } catch(AgentNotFoundException exc){
            log.info("Registering agent at " + agentIP + ":" + port);
            try {
                getAgentManager().createAgent(agentIP, new Integer(port), 
                                              args.getAuthToken(),
                                              agentToken, version);
            } catch(AgentCreateException oexc){
                log.error("Error creating agent", oexc);
                return new RegisterAgent_result("Error creating agent: " + 
                                                oexc.getMessage());
            } catch(SystemException oexc){
                log.error("Error creating agent", oexc);
                return new RegisterAgent_result("Error creating agent:  " +
                                                "Internal system error");
            }            
        } catch(SystemException exc){
            log.error("Error updating agent", exc);
            return new RegisterAgent_result("Error updating agent:  " +
                                            "Internal system error");
        }

        RegisterAgent_result result =
            new RegisterAgent_result("token:" + agentToken);

        /**
         * Reschedule all metrics on a platform when it is started for the
         * first time.  This allows the schedule to be updated immediately
         * on either agent updates, or if the user removes the agent data
         * directory.
         */
        if (ids != null) {
            try {
                List zevents = new ArrayList();
                ResourceRefreshZevent zevent;
                AuthzSubjectValue overlord = getOverlordVal();
                for (Iterator it = ids.iterator(); it.hasNext();) {
                    Integer id = (Integer) it.next();
                    Platform platform = getPlatformManager()
                        .findPlatformById(id);

                    zevent = new ResourceRefreshZevent(overlord,
                                                       platform.getEntityId());
                    zevents.add(zevent);

                    Collection servers = platform.getServers();
                    for (Iterator i = servers.iterator(); i.hasNext(); ) {
                        Server server = (Server)i.next();

                        zevent = new ResourceRefreshZevent(overlord,
                                                           server.getEntityId());
                        zevents.add(zevent);

                        Collection services = server.getServices();
                        for (Iterator serviceItr = services.iterator();
                             serviceItr.hasNext(); )
                        {
                            Service service = (Service)serviceItr.next();
                            zevent = new ResourceRefreshZevent(overlord,
                                                               service.getEntityId());
                            zevents.add(zevent);
                        }
                    }
                }

                ZeventManager.getInstance().enqueueEvents(zevents);

            } catch (Exception e) {
                // Not fatal, the metrics will eventually be rescheduled...
                log.error("Unable to refresh agent schedule", e);
            }
        }
        return result;
    }

    /**
     * Update an Agent's setup information.  When an agent's 'setup' is
     * called after it has already been setup, the connection information 
     * (i.e. address to contact, port, etc.) are updated but the same agent
     * token is kept.
     */
    private UpdateAgent_result cmdUpdateAgent(LatherContext ctx,
                                              UpdateAgent_args args)
        throws LatherRemoteException
    {
        AgentValue agentVal;
        String errRes;

        try {
            checkUserCanManageAgent(ctx, args.getUser(), args.getPword(), 
                                         "update");
        } catch(PermissionException exc){
            return new UpdateAgent_result("Permission denied");
        }

        validateAgent(ctx, args.getAgentToken(), false);
        try {
            agentVal = getAgentManager().getAgent(args.getAgentToken());
        
            if((errRes = testAgentConn(args.getAgentIP(), 
                                            args.getAgentPort(), 
                                            agentVal.getAuthToken())) != null)
            {
                return new UpdateAgent_result(errRes);
            }
        
            agentVal.setAddress(args.getAgentIP());
            agentVal.setPort(args.getAgentPort());
            getAgentManager().updateAgent(args.getAgentToken(), agentVal);
        } catch(AgentNotFoundException exc){
            return new UpdateAgent_result("Agent not found for update");
        }
        return new UpdateAgent_result();
    }

    /**
     * A simple ping, to make sure the server is running.
     */
    private LatherValue cmdPing(LatherValue arg){
        return arg;
    }

    /**
     * Command to make sure that a user is valid.  
     */
    private UserIsValid_result cmdUserIsValid(LatherContext ctx, 
                                              UserIsValid_args arg)
    {
        try {
            getAuthManager().getSessionId(arg.getUser(), arg.getPword());
        } catch(Exception exc){
            log.warn("An invalid user(" + arg.getUser() + 
                          ") connected from " + ctx.getCallerIP());
            return new UserIsValid_result(false);
        }
        return new UserIsValid_result(true);
    }

    /**
     * Command to process measurements received from the agent.
     */
    private MeasurementSendReport_result 
        cmdMeasurementSendReport(MeasurementSendReport_args args)
        throws LatherRemoteException
    {
        MeasurementSendReport_result res =
            new MeasurementSendReport_result();

        try {
            getReportProcessor().handleMeasurementReport(args.getReport());
        } catch(DataInserterException e) {
            throw new LatherRemoteException("Unable to insert data " + 
                                            e.getMessage());
        }

        res.setTime(System.currentTimeMillis());
        return res;
    }

    /**
     * Called by agents to report platforms, servers, and services
     * detected via autoinventory scans.
     */
    private NullLatherValue cmdAiSendReport(AiSendReport_args args)
        throws LatherRemoteException
    {
        AutoinventoryManagerLocal aiManagerLocal;
        ScanStateCore core;

        core = args.getCore();

        aiManagerLocal = getAutoInventoryManager();
        try {
            aiManagerLocal.reportAIData(args.getAgentToken(), core);
        } catch(AutoinventoryException exc){
            log.error("Error in AiSendReport: " + exc.getMessage(), exc);
            throw new LatherRemoteException(exc.getMessage());
        }

        return NullLatherValue.INSTANCE;
    }

    /**
     * Called by agents to report resources detected via runtime autoinventory 
     * scans, using the monitoring interfaces to a server.
     */
    private NullLatherValue cmdAiSendRuntimeReport(AiSendRuntimeReport_args arg)
        throws LatherRemoteException
    {
        AutoinventoryManagerLocal aiManagerLocal;

        aiManagerLocal = getAutoInventoryManager();
        try {
            aiManagerLocal.reportAIRuntimeReport(arg.getAgentToken(), 
                                                 arg.getReport());
        } catch(Exception exc){
            log.error("Runtime report error: " + exc.getMessage(), exc);
        }
        
        return NullLatherValue.INSTANCE;
    }

    /**
     * Get config information about all the entities which an agent
     * is servicing.
     */
    private MeasurementGetConfigs_result
        cmdMeasurementGetConfigs(MeasurementGetConfigs_args args)
        throws LatherRemoteException
    {
        MeasurementGetConfigs_result res;
        MeasurementConfigList cList;
        ResourceTree tree;
        AuthzSubject overlord;
        ArrayList ents;

        overlord = getAuthzSubjectManager().getOverlordPojo();
        try {
            tree = getAgentManager().getEntitiesForAgent(overlord,
                                                         args.getAgentToken());
        } catch(PermissionException exc){
            throw new SystemException("Overlord unable to get resource " +
                                      "tree list");
        } catch(AgentNotFoundException exc){
            throw new SystemException("Validated an agent which could " +
                                      "not be found");
        }

        ents = new ArrayList();
        for(Iterator p=tree.getPlatformIterator(); p.hasNext(); ){
            PlatformNode pNode = (PlatformNode)p.next();
            addMeasurementConfig(ents, pNode.getPlatform());

            try {
                AppdefEntityValue aeval = new AppdefEntityValue(
                    pNode.getPlatform().getEntityId(), overlord);
                List services =
                    aeval.getAssociatedServices(PageControl.PAGE_ALL);
                for (int i = 0; i < services.size(); i++) {
                    ServiceValue val = (ServiceValue) services.get(i);

                    AppdefEntityID id = val.getEntityId();
                                           
                    addMeasurementConfig(ents, id,
                                         val.getServiceType().getName());
                }
            } catch (Exception e) {
                //Shouldn't happen
                log.error("Encountered exception looking up platform " +
                          "services: " + e.getMessage(), e);
            }

            for(Iterator s=pNode.getServerIterator(); s.hasNext(); ){
                ServerNode sNode = (ServerNode)s.next();

                addMeasurementConfig(ents, sNode.getServer());

                for(Iterator v=sNode.getServiceIterator(); v.hasNext(); ){
                    ServiceNode vNode = (ServiceNode)v.next();

                    addMeasurementConfig(ents, vNode.getService());
                }
            }
        }

        cList = new MeasurementConfigList();
        cList.setEntities((MeasurementConfigEntity[])
                          ents.toArray(new MeasurementConfigEntity[ents.size()]));
        res = new MeasurementGetConfigs_result();
        res.setConfigs(cList);
        return res;
    }

    private void addMeasurementConfig(List ents, AppdefResourceValue resource){
        addMeasurementConfig(ents, resource.getEntityId(),
                             resource.getAppdefResourceTypeValue().getName());
    }

    private void addMeasurementConfig(List ents, AppdefEntityID id, 
                                      String typeName) {
        MeasurementConfigEntity ent = new MeasurementConfigEntity();
        ConfigResponse response;
        byte[] config;

        try {
            response = getConfigManager().
                getMergedConfigResponse(getOverlordVal(),
                                        ProductPlugin.TYPE_MEASUREMENT,
                                        id, true);

            // Only send the configuration to the agent if log or
            // config file tracking has been enabled.
            if(ConfigTrackPlugin.isEnabled(response, id.getType()) ||
               LogTrackPlugin.isEnabled(response, id.getType())) {

                config = response.encode();

                ent.setPluginName(id.getAppdefKey());
                ent.setPluginType(typeName);
                ent.setConfig(config);

                ents.add(ent);
            }

        } catch(Exception exc) {
            return; // Not a fatal condition
        }
    }

    /**
     * Called by agents to report log statements
     */
    private NullLatherValue cmdTrackLogMessage(TrackSend_args args)
        throws LatherRemoteException {
        TrackEventReport report = args.getEvents();

        TrackEvent[] events = report.getEvents();
        
        if (events.length > 0) {
            ArrayList logEvents = new ArrayList(events.length);
            
            for (int i = 0; i < events.length; i++) {
                // Create a ResourceLogEvent to send
                log.debug("TrackEvent: " + events[i]);
                ResourceLogEvent rle = new ResourceLogEvent(events[i]);
                logEvents.add(rle);
            }
            
            sendTopicMessage(EventConstants.EVENTS_TOPIC, logEvents);
        }
        
        return new NullLatherValue();
    }

    /**
     * Called by agents to report config changes
     */
    private NullLatherValue cmdTrackConfigChange(TrackSend_args args)
        throws LatherRemoteException {
        TrackEventReport report = args.getEvents();

        TrackEvent[] events = report.getEvents();

        if (events.length > 0) {
            ArrayList ccEvents = new ArrayList(events.length);
            
            for (int i = 0; i < events.length; i++) {
                // Create a ConfigChangedEvent to send
                log.debug("TrackEvent: " + events[i]);
                ConfigChangedEvent cce = new ConfigChangedEvent(events[i]);
                ccEvents.add(cce);
            }
            
            sendTopicMessage(EventConstants.EVENTS_TOPIC, ccEvents);
        }
        
        return new NullLatherValue();
    }

    /**
     * Main dispatch method called from the LatherBoss.
     */
    public LatherValue dispatch(LatherContext ctx, String method, 
                                LatherValue arg)
        throws LatherRemoteException
    {
        AgentManagerLocal agentMan = AgentManagerEJBImpl.getOne();
        Integer agentId = null;
        
        log.debug("Request for " + method + "() from " + ctx.getCallerIP());
        
        if(secureCommands.contains(method)){
            if(!(arg instanceof SecureAgentLatherValue)){
                log.warn("Authenticated call made from " +
                         ctx.getCallerIP() + " which did not subclass " +
                         "the correct authentication class");
                throw new LatherRemoteException("Unauthorized agent denied");
            }

            String agentToken = ((SecureAgentLatherValue)arg).getAgentToken(); 
            validateAgent(ctx, agentToken);
            try {
                Agent a = agentMan.getAgentPojo(agentToken);
                agentId = a.getId();
            } catch(Exception e) {
            }
        }
        
        AgentConnection conn = null;
        try {
            conn = agentMan.agentConnected(method, ctx.getCallerIP(), agentId);             
            return _dispatch(ctx, method, arg);
        } finally {
            if (conn != null)
                agentMan.agentDisconnected(conn);
        }
    }
    
    private LatherValue _dispatch(LatherContext ctx, String method,
                                  LatherValue arg)
        throws LatherRemoteException
    {
        if(method.equals(CommandInfo.CMD_PING)){
            return cmdPing(arg);
        } else if(method.equals(CommandInfo.CMD_USERISVALID)){
            return cmdUserIsValid(ctx, (UserIsValid_args) arg);
        } else if(method.equals(CommandInfo.CMD_MEASUREMENT_SEND_REPORT)){
            return cmdMeasurementSendReport((MeasurementSendReport_args) arg);
        } else if(method.equals(CommandInfo.CMD_MEASUREMENT_GET_CONFIGS)){
            return cmdMeasurementGetConfigs((MeasurementGetConfigs_args) arg);
        } else if(method.equals(CommandInfo.CMD_REGISTER_AGENT)){
            return cmdRegisterAgent(ctx, (RegisterAgent_args)arg);
        } else if(method.equals(CommandInfo.CMD_UPDATE_AGENT)){
            return cmdUpdateAgent(ctx, (UpdateAgent_args) arg);
        } else if(method.equals(CommandInfo.CMD_AI_SEND_REPORT)){
            return cmdAiSendReport((AiSendReport_args) arg);
        } else if(method.equals(CommandInfo.CMD_AI_SEND_RUNTIME_REPORT)){
            return cmdAiSendRuntimeReport((AiSendRuntimeReport_args) arg);
        } else if(method.equals(CommandInfo.CMD_TRACK_SEND_LOG)){
            return cmdTrackLogMessage((TrackSend_args) arg);
        } else if(method.equals(CommandInfo.CMD_TRACK_SEND_CONFIG_CHANGE)){
            return cmdTrackConfigChange((TrackSend_args) arg);
        } else if(method.equals(CommandInfo.CMD_CONTROL_GET_PLUGIN_CONFIG)){
            return cmdControlGetPluginConfig((ControlGetPluginConfig_args) arg);
        } else if(method.equals(CommandInfo.CMD_CONTROL_SEND_COMMAND_RESULT)){
            return cmdControlSendCommandResult((ControlSendCommandResult_args)
                                               arg);
        } else {
            log.warn(ctx.getCallerIP() + " attempted to invoke '" + method +
                     "' which could not be found");
            throw new LatherRemoteException("Unknown method, '" + method + "'");
        }
    }

    public void destroy() {
    }

    /**
     * Send an agent a plugin configuration.  This is needed when agents
     * restart, since they do not persist control plugin configuration.
     */
    private ControlGetPluginConfig_result
        cmdControlGetPluginConfig(ControlGetPluginConfig_args args)
        throws LatherRemoteException {
        ControlGetPluginConfig_result res;
        ControlManagerLocal cLocal;
        byte[] cfg;
    
        cLocal = getControlManager();
        try {
            cfg = cLocal.getPluginConfiguration(args.getPluginName(), 
                                                args.getMerge());
        } catch(PluginException exc){
            log.warn("Error getting control config for plugin '" +
                          args.getPluginName() + "'", exc);
            throw new LatherRemoteException("Error getting control config "
                                            + "for plugin '" + 
                                            args.getPluginName() +"': " +
                                            exc.getMessage());
        }
    
        res = new ControlGetPluginConfig_result();
        res.setConfig(cfg);
        return res;
    }

    /**
     * Receive status information about a previous control action
     */
    private NullLatherValue
        cmdControlSendCommandResult(ControlSendCommandResult_args args) {
        getControlManager().sendCommandResult(args.getId(), args.getResult(), 
                                              args.getStartTime(), 
                                              args.getEndTime(),
                                              args.getMessage());
    
        //Get live measurements on the resource
        String name = args.getName();
        if (name != null) {
            log.info("Getting live measurements for " + name);
            AppdefEntityID id = new AppdefEntityID(name);
            try {
                getMetricManager().getLiveMeasurementValues(getOverlord(), id);
            } catch (Exception e) {
                log.error("Unable to fetch live measurements: " + e, e);
            }
        } else {
            log.error("No plugin name found, not fetching live measurements");
        }
        
        return NullLatherValue.INSTANCE;
    }
}
