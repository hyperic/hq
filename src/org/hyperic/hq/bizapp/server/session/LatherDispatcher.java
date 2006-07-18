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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.login.LoginException;

import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.appdef.shared.AgentCreateException;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AgentUnauthorizedException;
import org.hyperic.hq.appdef.shared.AgentValue;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEvent;
import org.hyperic.hq.appdef.shared.MiniResourceValue;
import org.hyperic.hq.appdef.shared.PlatformPK;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerLightValue;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceLightValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.appdef.shared.miniResourceTree.MiniPlatformNode;
import org.hyperic.hq.appdef.shared.miniResourceTree.MiniResourceTree;
import org.hyperic.hq.appdef.shared.miniResourceTree.MiniServerNode;
import org.hyperic.hq.appdef.shared.miniResourceTree.MiniServiceNode;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.ScanStateCore;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManagerLocal;
import org.hyperic.hq.bizapp.agent.client.SecureAgentConnection;
import org.hyperic.hq.bizapp.shared.lather.AiSendReport_args;
import org.hyperic.hq.bizapp.shared.lather.AiSendRuntimeReport_args;
import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
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
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.measurement.data.TrackEventReport;
import org.hyperic.hq.measurement.shared.ConfigChangedEvent;
import org.hyperic.hq.measurement.shared.MeasurementConfigEntity;
import org.hyperic.hq.measurement.shared.MeasurementConfigList;
import org.hyperic.hq.measurement.shared.ResourceLogEvent;
import org.hyperic.hq.product.ConfigTrackPlugin;
import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.lather.LatherContext;
import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;
import org.hyperic.lather.NullLatherValue;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.security.SecurityUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LatherDispatcher
    extends BizappSessionEJB
{
    private static final int AGENTCACHE_TIMEOUT = 60 * 1000;

    protected final Log log = 
        LogFactory.getLog(LatherDispatcher.class.getName());

    private SessionManager  sessionManager  = SessionManager.getInstance();
    private Hashtable       agentTokenCache = new Hashtable();
    protected HashSet       secureCommands  = new HashSet();
    private Object          tConnLock       = new Object();
    private TopicConnection tConn;
    private TopicSession    tSession;

    public LatherDispatcher(){
        for(int i=0; i<CommandInfo.SECURE_COMMANDS.length; i++){
            this.secureCommands.add(CommandInfo.SECURE_COMMANDS[i]);
        }
    }

    private void sendTopicMessage(String msgTopic, Serializable data)
        throws LatherRemoteException
    {
        Messenger sender;

        synchronized(this.tConnLock){
            if(this.tConn == null) {
                TopicConnectionFactory factory;
                InitialContext ctx;
                
                try {
                    ctx = new InitialContext();
                    factory = (TopicConnectionFactory) 
                        ctx.lookup(Messenger.CONN_FACTORY_JNDI);
                } catch(NamingException exc){
                    this.log.error("Error looking up " + 
                                   Messenger.CONN_FACTORY_JNDI + 
                                   " while sending a message to " + msgTopic,
                                   exc);
                    throw new LatherRemoteException("Unable to lookup " +
                                                    "message queue '" + 
                                                    msgTopic + "'");
                }
                
                try {
                    // Use ConnectionFactory to create JMS connection
                    this.tConn = factory.createTopicConnection();
                
                    // Use Connection to create session
                    this.tSession = this.tConn.createTopicSession(false, 
                                                    Session.AUTO_ACKNOWLEDGE);
                } catch(JMSException exc){
                    this.log.error("Error creating topic connection to '" +
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
        this.validateAgent(ctx, agentToken, true);
    }

    protected void validateAgent(LatherContext ctx, String agentToken, 
                                 boolean useCache)
        throws LatherRemoteException
    {
        Long initTime = (Long)this.agentTokenCache.get(agentToken);
        long now;

        now = System.currentTimeMillis();
        if(useCache == false || initTime == null ||
           (initTime.longValue() + AGENTCACHE_TIMEOUT) < now)
        {
            this.log.debug("Validating agent token");
            try {
                this.getAgentManager().checkAgentAuth(agentToken);
            } catch(AgentUnauthorizedException exc){
                this.log.warn("Unauthorized agent from " +
                              ctx.getCallerIP() + " denied");
                throw new LatherRemoteException("Unauthorized agent denied");
            }
            this.agentTokenCache.put(agentToken, new Long(now));
        }
    }

    private void checkUserCanManageAgent(LatherContext ctx, String user, 
                                         String pword, String operation)
        throws PermissionException
    {
        try {
            AuthzSubjectValue subject;
            int sessionId;

            sessionId = this.getAuthManager().getSessionId(user, pword);
            subject   = this.sessionManager.getSubject(sessionId);
            this.getServerManager().checkCreatePlatformPermission(subject);
        } catch(SecurityException exc){
            this.log.warn("Security exception when '" + user + 
                          "' tried to " + operation + " an Agent @ " +
                          ctx.getCallerIP(), exc);
            throw new PermissionException();
        } catch(LoginException exc){
            this.log.warn("Login exception when '" + user + 
                          "' tried to " + operation + " an Agent @ " +
                          ctx.getCallerIP() + ":  " + exc.getMessage());
            throw new PermissionException();
        } catch(SessionTimeoutException exc){
            this.log.warn("Session timeout when '" + user + 
                          "' tried to " + operation + " an Agent @ " +
                          ctx.getCallerIP() + ":  " + exc.getMessage());
            throw new PermissionException();
        } catch(SessionNotFoundException exc){
            this.log.warn("Session not found when '" + user + 
                          "' tried to " + operation + " an Agent @ " +
                          ctx.getCallerIP() + ":  " + exc.getMessage());
            throw new PermissionException();
        } catch(PermissionException exc){
            this.log.warn("Permission denied when '" + user + 
                          "' tried to " + operation + " an Agent @ " +
                          ctx.getCallerIP());
            throw new PermissionException();
        } catch(ApplicationException exc){
            this.log.warn("Application exception when '" + user + 
                          "' tried to " + operation + " an Agent @ " +
                          ctx.getCallerIP(), exc);
            throw new PermissionException();
        } catch(ConfigPropertyException exc){
            this.log.warn("Config property exception when '" + user + 
                          "' tried to " + operation + " an Agent @ " +
                          ctx.getCallerIP(), exc);
            throw new PermissionException();
        } catch(SystemException exc){
            this.log.warn("System exception when '" + user + 
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
                                                  LatherValue lArgs)
        throws LatherRemoteException
    {
        RegisterAgent_args args = (RegisterAgent_args)lArgs;
        AgentValue agentVal;
        String agentToken, errRes, agentIP, version;
        int port;

        try {
            this.checkUserCanManageAgent(ctx, args.getUser(), args.getPword(), 
                                         "register");
        } catch(PermissionException exc){
            return new RegisterAgent_result("Permission denied");
        }

        agentIP = args.getAgentIP();
        port    = args.getAgentPort();
        version = args.getVersion();

        errRes = this.testAgentConn(agentIP, port, args.getAuthToken());
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
        PlatformPK pk = null;
        try {
            AgentValue origAgent =
                this.getAgentManager().getAgent(agentIP, port);
            
            try {
                pk = getPlatformManager().
                    getPlatformPkByAgentToken(this.getOverlord(),
                                              origAgent.getAgentToken());
            } catch (Exception e) {
                // No platforms found, no a big deal
            }

            this.log.info("Updating agent information for " + agentIP + ":" +
                          port);
            this.getAgentManager().updateAgent(agentIP, port, agentVal);
        } catch(AgentNotFoundException exc){
            this.log.info("Registering agent at " + agentIP + ":" + port);
            try {
                this.getAgentManager().createAgent(agentVal);
            } catch(AgentCreateException oexc){
                this.log.error("Error creating agent", oexc);
                return new RegisterAgent_result("Error creating agent: " + 
                                                oexc.getMessage());
            } catch(SystemException oexc){
                this.log.error("Error creating agent", oexc);
                return new RegisterAgent_result("Error creating agent:  " +
                                                "Internal system error");
            }            
        } catch(SystemException exc){
            this.log.error("Error updating agent", exc);
            return new RegisterAgent_result("Error updating agent:  " +
                                            "Internal system error");
        }

        RegisterAgent_result result =
            new RegisterAgent_result("token:" + agentToken);

        if (pk != null)
            try {
                PlatformValue platform = getPlatformManager()
                    .getPlatformById(this.getOverlord(), pk.getId());
                
                // Send the agent the schedule
                // Tell HQ we have a new agent starting.  This forces an update
                // of the metric schedule.
                getPlatformManager().sendAppdefEvent(this.getOverlord(), 
                                                     new AppdefEntityID(pk),
                                                     AppdefEvent.ACTION_UPDATE);
                
                ServerLightValue[] servers = platform.getServerValues();
                for (int i = 0; i < servers.length; i++) {
                    getPlatformManager().sendAppdefEvent(
                         this.getOverlord(), servers[i].getEntityId(),
                         AppdefEvent.ACTION_UPDATE);
                    
                    ServerValue server = getServerManager()
                        .getServerById(this.getOverlord(), servers[i].getId());
                    
                    ServiceLightValue[] services = server.getServiceValues();
                    for (int j = 0; j < services.length; j++) {
                        getPlatformManager().sendAppdefEvent(
                            this.getOverlord(), services[j].getEntityId(),
                            AppdefEvent.ACTION_UPDATE);
                    }
                }
            } catch (Exception e) {
                // Shouldn't happen, not fatal if by chance it does.  The
                // agent schedule will not be immediately updated.
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
                                              LatherValue lArgs)
        throws LatherRemoteException
    {
        UpdateAgent_args args = (UpdateAgent_args)lArgs;
        AgentValue agentVal;
        String errRes;

        try {
            this.checkUserCanManageAgent(ctx, args.getUser(), args.getPword(), 
                                         "update");
        } catch(PermissionException exc){
            return new UpdateAgent_result("Permission denied");
        }

        this.validateAgent(ctx, args.getAgentToken(), false);
        try {
            agentVal = this.getAgentManager().getAgent(args.getAgentToken());
        
            if((errRes = this.testAgentConn(args.getAgentIP(), 
                                            args.getAgentPort(), 
                                            agentVal.getAuthToken())) != null)
            {
                return new UpdateAgent_result(errRes);
            }
        
            agentVal.setAddress(args.getAgentIP());
            agentVal.setPort(args.getAgentPort());
            this.getAgentManager().updateAgent(args.getAgentToken(), agentVal);
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
                                              LatherValue lArg)
    {
        UserIsValid_args arg = (UserIsValid_args)lArg;

        try {
            this.getAuthManager().getSessionId(arg.getUser(), arg.getPword());
        } catch(Exception exc){
            this.log.warn("An invalid user(" + arg.getUser() + 
                          ") connected from " + ctx.getCallerIP());
            return new UserIsValid_result(false);
        }
        return new UserIsValid_result(true);
    }

    /**
     * Command to process measurements received from the agent.
     */
    private MeasurementSendReport_result 
        cmdMeasurementSendReport(LatherValue lArg)
        throws LatherRemoteException
    {
        MeasurementSendReport_args args;
        MeasurementSendReport_result res =
            new MeasurementSendReport_result();

        args = (MeasurementSendReport_args)lArg;
        this.getReportProcessor().handleMeasurementReport(args.getReport());

        res.setTime(System.currentTimeMillis());
        return res;
    }

    /**
     * Called by agents to report platforms, servers, and services
     * detected via autoinventory scans.
     */
    private NullLatherValue cmdAiSendReport(LatherValue lArg)
        throws LatherRemoteException
    {
        AiSendReport_args args = (AiSendReport_args)lArg;
        AutoinventoryManagerLocal aiManagerLocal;
        ScanStateCore core;

        core = args.getCore();
        //log.info("AgentCallbackBoss.aiSendReport: received: " + core);

        aiManagerLocal = this.getAutoInventoryManager();
        try {
            aiManagerLocal.reportAIData(args.getAgentToken(), core);
        } catch(AutoinventoryException exc){
            this.log.error("Error in AiSendReport: " + exc.getMessage(), exc);
            throw new LatherRemoteException(exc.getMessage());
        }

        return NullLatherValue.INSTANCE;
    }

    /**
     * Called by agents to report resources detected via runtime autoinventory 
     * scans, using the monitoring interfaces to a server.
     */
    private NullLatherValue cmdAiSendRuntimeReport(LatherValue lArg)
        throws LatherRemoteException
    {
        AiSendRuntimeReport_args arg = (AiSendRuntimeReport_args)lArg;
        AutoinventoryManagerLocal aiManagerLocal;

        aiManagerLocal = this.getAutoInventoryManager();
        try {
            aiManagerLocal.reportAIRuntimeReport(arg.getAgentToken(), 
                                                 arg.getReport());
        } catch(ValidationException exc){

        } catch(PermissionException exc){

        } catch(ApplicationException exc){
            this.log.error("RUNTIME REPORT ERROR: " + exc.getMessage());
        } catch(AutoinventoryException exc){

        }
        return NullLatherValue.INSTANCE;
    }

    /**
     * Get config information about all the entities which an agent
     * is servicing.
     */
    private MeasurementGetConfigs_result cmdMeasurementGetConfigs(LatherValue lArgs)
        throws LatherRemoteException
    {
        MeasurementGetConfigs_result res;
        MeasurementGetConfigs_args args;
        MeasurementConfigList cList;
        MiniResourceTree tree;
        AuthzSubjectValue overlord;
        ArrayList ents;

        args = (MeasurementGetConfigs_args)lArgs;
        overlord = this.getOverlord();
        try {
            tree = this.getAgentManager().
                getEntitiesForAgent(overlord,
                                    args.getAgentToken());
        } catch(PermissionException exc){
            exc.printStackTrace();
            throw new SystemException("Overlord unable to get resource " +
                                      "tree list");
        } catch(AgentNotFoundException exc){
            throw new SystemException("Validated an agent which could " +
                                      "not be found");
        }
        ents = new ArrayList();
        for(Iterator p=tree.getPlatformIterator(); p.hasNext(); ){
            MiniPlatformNode pNode = (MiniPlatformNode)p.next();

            this.addMeasurementConfig(ents, pNode.getPlatform());

            try {
                PageList services = this.getServiceManager().
                    getPlatformServices(overlord, pNode.getId(),
                                        PageControl.PAGE_ALL);
                for (int i = 0; i < services.size(); i++) {
                    ServiceValue val = (ServiceValue)services.get(i);

                    AppdefEntityID id =
                        new AppdefEntityID(AppdefEntityConstants.
                                           APPDEF_TYPE_SERVICE, val.getId());
                                           
                    this.addMeasurementConfig(ents, id,
                                              val.getServiceType().getName());
                }
            } catch (Exception e) {
                //Shouldn't happen
                log.error("Encountered exception looking up platform " +
                          "services: " + e.getMessage(), e);
            }

            for(Iterator s=pNode.getServerIterator(); s.hasNext(); ){
                MiniServerNode sNode = (MiniServerNode)s.next();

                this.addMeasurementConfig(ents, sNode.getServer());

                for(Iterator v=sNode.getServiceIterator(); v.hasNext(); ){
                    MiniServiceNode vNode = (MiniServiceNode)v.next();

                    this.addMeasurementConfig(ents, vNode.getService());
                }
            }
        }

        cList = new MeasurementConfigList();
        cList.setEntities((MeasurementConfigEntity[])
                          ents.toArray(new MeasurementConfigEntity[0]));

        res = new MeasurementGetConfigs_result();
        res.setConfigs(cList);
        return res;
    }

    private void addMeasurementConfig(List ents, MiniResourceValue resource){
        addMeasurementConfig(ents, resource.getEntityId(), resource.typeName);
    }

    private void addMeasurementConfig(List ents, AppdefEntityID id, 
                                      String typeName) {
        MeasurementConfigEntity ent = new MeasurementConfigEntity();
        ConfigResponse response;
        byte[] config;

        try {
            response = this.getConfigManager().
                getMergedConfigResponse(this.getOverlord(),
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
    private NullLatherValue cmdTrackLogMessage(LatherValue lArg)
        throws LatherRemoteException {

        TrackSend_args args = (TrackSend_args) lArg;
        TrackEventReport report = args.getEvents();

        TrackEvent[] events = report.getEvents();

        for (int i = 0; i < events.length; i++) {
            // Create a ResourceLogEvent to send
            this.log.debug("TrackEvent: " + events[i]);
            ResourceLogEvent rle = new ResourceLogEvent(events[i]);
            this.sendTopicMessage(EventConstants.EVENTS_TOPIC, rle);
        }

        return new NullLatherValue();
    }

    /**
     * Called by agents to report config changes
     */
    private NullLatherValue cmdTrackConfigChange(LatherValue lArg)
        throws LatherRemoteException {

        TrackSend_args args = (TrackSend_args) lArg;
        TrackEventReport report = args.getEvents();

        TrackEvent[] events = report.getEvents();

        for (int i = 0; i < events.length; i++) {
            // Create a ConfigChangedEvent to send
            this.log.debug("TrackEvent: " + events[i]);
            ConfigChangedEvent cce = new ConfigChangedEvent(events[i]);
            this.sendTopicMessage(EventConstants.EVENTS_TOPIC, cce);
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
        this.log.debug("Request for " + method + "() from " +
                       ctx.getCallerIP());
        
        if(this.secureCommands.contains(method)){
            if(!(arg instanceof SecureAgentLatherValue)){
                this.log.warn("Authenticated call made from " +
                              ctx.getCallerIP() + " which did not subclass " +
                              "the correct authentication class");
                throw new LatherRemoteException("Unauthorized agent denied");
            }

            this.validateAgent(ctx,
                               ((SecureAgentLatherValue)arg).getAgentToken());
        }

        if(method.equals(CommandInfo.CMD_PING)){
            return this.cmdPing(arg);
        } else if(method.equals(CommandInfo.CMD_USERISVALID)){
            return this.cmdUserIsValid(ctx, arg);
        } else if(method.equals(CommandInfo.CMD_MEASUREMENT_SEND_REPORT)){
            return this.cmdMeasurementSendReport(arg);
        } else if(method.equals(CommandInfo.CMD_MEASUREMENT_GET_CONFIGS)){
            return this.cmdMeasurementGetConfigs(arg);
        } else if(method.equals(CommandInfo.CMD_REGISTER_AGENT)){
            return this.cmdRegisterAgent(ctx, arg);
        } else if(method.equals(CommandInfo.CMD_UPDATE_AGENT)){
            return this.cmdUpdateAgent(ctx, arg);
        } else if(method.equals(CommandInfo.CMD_AI_SEND_REPORT)){
            return this.cmdAiSendReport(arg);
        } else if(method.equals(CommandInfo.CMD_AI_SEND_RUNTIME_REPORT)){
            return this.cmdAiSendRuntimeReport(arg);
        } else if(method.equals(CommandInfo.CMD_TRACK_SEND_LOG)){
            return this.cmdTrackLogMessage(arg);
        } else if(method.equals(CommandInfo.CMD_TRACK_SEND_CONFIG_CHANGE)){
            return this.cmdTrackConfigChange(arg);
        } else {
            this.log.warn(ctx.getCallerIP() + " attempted to invoke '" + 
                          method + "' which could not be found");
            throw new LatherRemoteException("Unknown method, '" + method + 
                                            "'");
        }
    }

    public void destroy() {
    }
}
