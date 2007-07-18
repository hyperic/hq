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

package org.hyperic.hq.galerts.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.GroupChangeCallback;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.server.session.Crispo;
import org.hyperic.hq.common.server.session.CrispoManagerEJBImpl;
import org.hyperic.hq.common.shared.CrispoManagerLocal;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationManagerEJBImpl;
import org.hyperic.hq.escalation.shared.EscalationManagerLocal;
import org.hyperic.hq.events.AlertAuxLog;
import org.hyperic.hq.events.AlertAuxLogProvider;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.galerts.processor.GalertProcessor;
import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyInfo;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyType;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyTypeInfo;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GalertDefPartition;
import org.hyperic.hq.galerts.server.session.ExecutionReason;
import org.hyperic.hq.galerts.server.session.GalertLog;
import org.hyperic.hq.galerts.shared.GalertManagerLocal;
import org.hyperic.hq.galerts.shared.GalertManagerUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;


/**
 * @ejb:bean name="GalertManager"
 *      jndi-name="ejb/galerts/GalertManager"
 *      local-jndi-name="LocalGalertManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 * @ejb:transaction type="REQUIRED"
 */
public class GalertManagerEJBImpl 
    implements SessionBean 
{
    private final Log _log = LogFactory.getLog(GalertManagerEJBImpl.class);

    private EscalationManagerLocal       _escMan;
    private ExecutionStrategyTypeInfoDAO _stratTypeDAO;
    private GalertDefDAO                 _defDAO;
    private GalertAuxLogDAO              _auxLogDAO;
    private GalertLogDAO                 _logDAO;
    private CrispoManagerLocal           _crispoMan;
    private GalertActionLogDAO           _actionLogDAO;
    
    public GalertManagerEJBImpl() {
        DAOFactory f = DAOFactory.getDAOFactory();

        _escMan       = EscalationManagerEJBImpl.getOne();      
        _stratTypeDAO = new ExecutionStrategyTypeInfoDAO(f); 
        _defDAO       = new GalertDefDAO(f);
        _logDAO       = new GalertLogDAO(f);
        _auxLogDAO    = new GalertAuxLogDAO(f);
        _crispoMan    = CrispoManagerEJBImpl.getOne();
        _actionLogDAO = new GalertActionLogDAO(f);
    }

    /**
     * Update basic properties of an alert definition
     * 
     * If any of the passed params are non-null, they will be updated with
     * the new value
     * 
     * @ejb:interface-method  
     */
    public void update(GalertDef def, String name, String desc, 
                       AlertSeverity severity, Boolean enabled)
    {
        boolean seriousUpdate = false;
        boolean updateName = false;
        
        if (def.isDeleted()) {
            throw new IllegalArgumentException("Unable to update a def " + 
                                               "which has already been " + 
                                               "deleted");
        }
        
        if (name != null) {
            def.setName(name);
            updateName = true;
        }
        
        if (desc != null) {
            def.setDescription(desc);
        }

        if (severity != null) {
            def.setSeverity(severity);
        }

        if (enabled != null) {
            def.setEnabled(enabled.booleanValue());
            seriousUpdate = true;
        }
        
        if (seriousUpdate) {
            GalertProcessor.getInstance().loadReloadOrUnload(def);
        } else if (updateName) {
            GalertProcessor.getInstance().alertDefUpdated(def, name);
        }
    }
    
    /**
     * Update the escalation of an alert def
     * @ejb:interface-method  
     */
    public void update(GalertDef def, Escalation escalation) {
        def.setEscalation(escalation);
        
        // End any escalation we were previously doing.
        EscalationManagerEJBImpl.getOne().endEscalation(def);
        GalertProcessor.getInstance().loadReloadOrUnload(def);
    }
    
    /**
     * Enable/disable an alert def
     * @ejb:interface-method
     */
    public void enable(GalertDef def, boolean enable) {
        update(def, null, null, null, Boolean.valueOf(enable));
    }
    
    /**
     * Find all alert definitions for the specified group
     * @ejb:interface-method  
     */
    public PageList findAlertDefs(ResourceGroup g, PageControl pc) {
        Pager pager = Pager.getDefaultPager();
        return pager.seek(_defDAO.findAll(g), pc);
    }

    /**
     * @ejb:interface-method  
     */
    public Collection findAllStrategyTypes() {
        return _stratTypeDAO.findAll();
    }
    
    /**
     * @ejb:interface-method  
     */
    public ExecutionStrategyTypeInfo findStrategyType(Integer id) {
        return _stratTypeDAO.findById(id);
    }

    /**
     * @ejb:interface-method  
     */
    public ExecutionStrategyTypeInfo findStrategyType(ExecutionStrategyType t) {
        return _stratTypeDAO.find(t);
    }

    /**
     * @ejb:interface-method  
     */
    public GalertDef findById(Integer id) {
        return _defDAO.findById(id);
    }
    
    /**
     * @ejb:interface-method  
     */
    public GalertAuxLog findAuxLogById(Integer id) {
        return _auxLogDAO.findById(id);
    }
    
    /**
     * Save the alert log and associated auxillary log information to the
     * DB.  
     * 
     * DevNote:  Since the GalertAuxLog table needs to be written first 
     * (for foreign-key from the auxType tables), we first traverse all the 
     * logs and save them.  Then, we perform the same traversal and save the 
     * specific logs.
     * 
     * @ejb:interface-method  
     */
    public GalertLog createAlertLog(GalertDef def, ExecutionReason reason) { 
        Map gAuxLogToAuxLog = new HashMap(); // Stores real logs to auxType logs
        GalertLog newLog = new GalertLog(def, reason, 
                                         System.currentTimeMillis());
        addAuxLogChildren(newLog, null, reason.getAuxLogs(), gAuxLogToAuxLog);
        _logDAO.save(newLog);
        
        for (Iterator i=gAuxLogToAuxLog.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry ent = (Map.Entry)i.next();
            GalertAuxLog gAuxLog = (GalertAuxLog)ent.getKey();
            AlertAuxLog auxLog = (AlertAuxLog)ent.getValue();
            AlertAuxLogProvider provider = auxLog.getProvider();
            
            if (provider != null)
                provider.save(gAuxLog.getId().intValue(), auxLog);
        }
        return newLog;
    }
    
    private void addAuxLogChildren(GalertLog alert, GalertAuxLog parent, 
                                   List auxLogs, Map gAuxLogToAuxLog) 
    {
        for (Iterator i=auxLogs.iterator(); i.hasNext(); ) {
            AlertAuxLog auxLog = (AlertAuxLog)i.next();
            GalertAuxLog newLog;
            
            newLog = alert.addAuxLog(auxLog, parent);
            gAuxLogToAuxLog.put(newLog, auxLog);
            addAuxLogChildren(alert, newLog, auxLog.getChildren(),
                              gAuxLogToAuxLog);
        }
    }
    
    /**
     * @ejb:interface-method  
     */
    public void createActionLog(GalertLog alert, String detail, Action action,
                                AuthzSubject subject) 
    {
        GalertActionLog log = alert.createActionLog(detail, action, subject);
        
        _actionLogDAO.save(log);
    }
    
    /**
     * @ejb:interface-method  
     */
    public List findAlertLogs(GalertDef def) {
        return _logDAO.findAll(def.getGroup());
    }

    /**
     * @ejb:interface-method  
     */
    public GalertLog findLastFixedByDef(GalertDef def) {
        try {
            return _logDAO.findLastByDefinition(def, true);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Simply sets the 'fixed' flag on an alert
     * @ejb:interface-method  
     */
    public void fixAlert(GalertLog alert) {
        alert.setFixed(true);
    }
    
    /**
     * @ejb:interface-method  
     */
    public Escalatable findEscalatableAlert(Integer id) {
        return GalertEscalatableCreator.createEscalatable(_logDAO.findById(id));
    }

    /**
     * @ejb:interface-method  
     */
    public GalertLog findAlertLog(Integer id) {
        return _logDAO.findById(id);
    }

    /**
     * @ejb:interface-method  
     */
    public List findAlertLogs(ResourceGroup group) {
        return _logDAO.findAll(group);
    }

    /**
     * @ejb:interface-method  
     */
    public PageList findAlertLogsByTimeWindow(ResourceGroup group, long begin,
                                              long end, PageControl pc) 
    {
        return _logDAO.findByTimeWindow(group, begin, end, pc);
    }

    /**
     * @see findAlerts
     * @return a list of {@link Escalatable}s
     * @ejb:interface-method
     */
    public List findEscalatables(AuthzSubjectValue subj, int count, 
                                 int priority, long timeRange, long endTime,
                                 List includes)
        throws PermissionException
    {
        List alerts = findAlerts(subj, count, priority, timeRange, endTime,
                                 includes);
        List res = new ArrayList(alerts.size());
        
        for (Iterator i=alerts.iterator(); i.hasNext(); ) {
            GalertLog alert = (GalertLog)i.next();
            
            res.add(GalertEscalatableCreator.createEscalatable(alert));
        }
        return res;
    }
    
    /**
     * Find group alerts based on a set of criteria
     *
     * @param subj      Subject doing the finding
     * @param count     Max # of alerts to return
     * @param priority  A value from {@link EventConstants}
     * @param timeRange the amount of milliseconds prior to current that the
     *                  alerts will be contained in.  e.g. the beginning of the  
     *                  time range will be (current - timeRante)
     * @param includes  A list of entity IDs to include in the result
     * 
     * @return a list of {@link GalertLog}s
     * @ejb:interface-method
     */
    public List findAlerts(AuthzSubjectValue subj, int count, int priority,
                           long timeRange, long endTime, List includes) 
        throws PermissionException 
    {
        List alerts;
            
        if (priority == EventConstants.PRIORITY_ALL) {
            alerts = _logDAO.findByCreateTime(endTime- timeRange, endTime, 
                                              count);
        } else {
            PageInfo pInfo = PageInfo.create(0, count, GalertLogSortField.DATE,
                                             false);
            alerts = _logDAO.findByCreateTimeAndPriority(endTime - timeRange,
                                                         endTime, priority, 
                                                         pInfo);
        }
            
        List result = new ArrayList();
        for (Iterator i=alerts.iterator(); i.hasNext(); ) {
            GalertLog l = (GalertLog)i.next();
            GalertDef def = l.getAlertDef();
            
            // Filter by appdef entity
            AppdefEntityID aeid = def.getAppdefID();
            if (includes != null && !includes.contains(aeid))
                continue;

            result.add(l);
        }
            
        return result;
    }
    
    /**
     * @ejb:interface-method
     */
    public List findAlerts(AuthzSubjectValue subj, AlertSeverity severity,
                           long timeRange, long endTime, PageInfo pInfo)
    {
        return _logDAO.findByCreateTimeAndPriority(endTime - timeRange, endTime, 
                                                   severity.getCode(), pInfo);
    }

    /**
     * @ejb:interface-method  
     */
    public void deleteAlertLog(GalertLog log) {
        _logDAO.remove(log);
    }
    
    /**
     * @ejb:interface-method  
     */
    public void deleteAlertLogs(ResourceGroup group) {
        _logDAO.removeAll(group);
    }

    /**
     * Register an execution strategy.
     * @ejb:interface-method
     */
    public ExecutionStrategyTypeInfo 
        registerExecutionStrategy(ExecutionStrategyType stratType)
    {
        ExecutionStrategyTypeInfo info = _stratTypeDAO.find(stratType);
        
        if (info != null) {
            _log.warn("Execution strategy type [" + 
                      stratType.getClass().getName() + "] already registered"); 
            return info;
        }

        info = new ExecutionStrategyTypeInfo(stratType);
        _stratTypeDAO.save(info);
        return info;
    }
    
    /**
     * Unregister an execution strategy.  This will fail if any alert  
     * definitions are currently using the strategy
     * @ejb:interface-method
     */
    public void unregisterExecutionStrategy(ExecutionStrategyType sType) {
        ExecutionStrategyTypeInfo info = _stratTypeDAO.find(sType);
        
        if (info == null) {
            _log.warn("Execution strategy [" + sType.getClass().getName() + 
                      "] already unregistered");
            return;
        }

        if (_defDAO.countByStrategy(info) != 0) {
            throw new IllegalArgumentException("Unable to unregister [ " + 
                                               sType.getClass().getName() +
                                               "] alert defs are using it");
        }
        
        _stratTypeDAO.remove(info);
    }

    /**
     * Configure triggers for a given partition.
     * 
     * @param triggerInfos A list of {@link GtriggerTypeInfo}s
     * @param configs      A list of {@link ConfigResponse}s, one for each
     *                     trigger info
     *                   
     * @ejb:interface-method
     */
    public void configureTriggers(GalertDef def, GalertDefPartition partition,
                                  List triggerInfos, List configs) 
    {
        ExecutionStrategyInfo strat;
        
        if (triggerInfos.size() != configs.size()) {
            throw new IllegalArgumentException("Must be a config for each " + 
                                               "trigger");
        }
        
        strat = def.getStrategy(partition);

        // Delete the old triggers if there were any
        List crispos = new ArrayList();
        for (Iterator i=strat.getTriggers().iterator(); i.hasNext(); ) {
            GtriggerInfo t = (GtriggerInfo)i.next();
            
            crispos.add(t.getConfigCrispo());
            _defDAO.remove(t);
        }
        
        for (Iterator i=crispos.iterator(); i.hasNext(); ) {
            _crispoMan.deleteCrispo((Crispo)i.next());
        }
        strat.clearTriggers();
        
        // Now add the new triggers
        Iterator j = configs.iterator();
        for (Iterator i=triggerInfos.iterator(); i.hasNext(); ) {
            GtriggerTypeInfo typeInfo = (GtriggerTypeInfo)i.next();
            ConfigResponse config = (ConfigResponse)j.next();
            Crispo crispo = _crispoMan.create(config);
            GtriggerInfo t = strat.addTrigger(typeInfo, crispo, def.getGroup(), 
                                              partition); 
 
            _defDAO.save(t);
        }
        GalertProcessor.getInstance().loadReloadOrUnload(def);
    }
    
    /**
     * @ejb:interface-method  
     */
    public ExecutionStrategyInfo 
        addPartition(GalertDef def, GalertDefPartition partition, 
                     ExecutionStrategyTypeInfo stratType, 
                     ConfigResponse stratConfig)
    {
        Crispo stratCrispo = _crispoMan.create(stratConfig);
        ExecutionStrategyInfo res = def.addPartition(partition, stratType, 
                                                     stratCrispo); 
        
        _stratTypeDAO.save(res);
        GalertProcessor.getInstance().loadReloadOrUnload(def);
        return res;
    }
    
    
    /**
     * @ejb:interface-method  
     */
    public GalertDef createAlertDef(AuthzSubjectValue subject, String name,
                                    String description, AlertSeverity severity,
                                    boolean enabled, ResourceGroup group)  
    {
        GalertDef def;
        
        def = new GalertDef(name, description, severity, enabled, group);

        _defDAO.save(def);
        GalertProcessor.getInstance().validateAlertDef(def);
        return def;
    }

    /**
     * Reload an alert definition.  Probably should only be called internally
     * here.
     * 
     * @ejb:interface-method
     */  
    public void reloadAlertDef(GalertDef def) {
        GalertProcessor.getInstance().loadReloadOrUnload(def);
    }
    
    /**
     * Mark an alert definition as deleted.  This will remove it from all
     * dialogues, but will leave all the data (specific alerts) in place.  
     *
     * @ejb:interface-method
     */
    public void markDefDeleted(GalertDef def) {
        update(def, null, null, null, Boolean.FALSE);
        def.setEscalation(null);
        def.setDeleted(true);
    }
    
    /**
     * Delete an alert definition along with all logs which are tied to it.
     * 
     * @ejb:interface-method
     */  
    public void nukeAlertDef(GalertDef def) {
        List nukeCrispos = new ArrayList();
        Integer defId = def.getId();
        
        for (Iterator i=_auxLogDAO.findAll(def).iterator(); i.hasNext(); ) {
            GalertAuxLog auxLog = (GalertAuxLog)i.next();
            
            auxLog.getProvider().delete(auxLog.getId().intValue());
        }
        _auxLogDAO.removeAll(def);
        
        // Kill the logs
        _logDAO.removeAll(def);
        
        for (Iterator i=def.getStrategies().iterator(); i.hasNext(); ) {
            ExecutionStrategyInfo strat = (ExecutionStrategyInfo)i.next();
            
            // Reconfigure the def to have 0 triggers (i.e. nuke the instances)
            configureTriggers(def, strat.getPartition(), Collections.EMPTY_LIST, 
                              Collections.EMPTY_LIST);
            nukeCrispos.add(strat.getConfigCrispo());
        }
        
        _defDAO.remove(def);
        
        for (Iterator i=nukeCrispos.iterator(); i.hasNext(); ) {
            Crispo c = (Crispo)i.next();

            _crispoMan.deleteCrispo(c);
        }
        GalertProcessor.getInstance().alertDefDeleted(defId);
    }
    
    /**
     * Start an escalation for a group alert definition.
     *
     * @ejb:interface-method
     */
    public void startEscalation(GalertDef def, ExecutionReason reason) {
        _escMan.startEscalation(def, new GalertEscalatableCreator(def, reason));
    }

    /**
     * Start an escalation for a group alert definition.
     *
     * @ejb:interface-method
     */
    public void processGroupDeletion(ResourceGroup g) { 
        Collection defs = _defDAO.findAbsolutelyAllGalertDefs(g);
        
        for (Iterator i=defs.iterator(); i.hasNext(); ) {
            GalertDef def = (GalertDef)i.next();
            
            _log.debug("Cascade deleting GalertDef[" + def.getName() + "]"); 
            nukeAlertDef(def);
        }
    }
    
    /**
     * @ejb:interface-method
     */
    public void startup() {
        _log.info("Galert manager starting up!");
        
        HQApp.getInstance().registerCallbackListener(GroupChangeCallback.class, 
                                                     new GroupChangeCallback()
        {
            public void postGroupCreate(ResourceGroup g) {
            }

            /**
             * Delete the GalertDefs that depend on the deleted group
             */
            public void preGroupDelete(ResourceGroup g) {
                GalertManagerEJBImpl.getOne().processGroupDeletion(g);
            }

            /**
             * When the group system changes the members, we reload the
             * in-memory alert definition. 
             * 
             * This may be undesirable if the frequency of the changes to
             * the alert definition is high, since the in-memory state is
             * reset every time this operation is performed.
             */
            public void groupMembersChanged(ResourceGroup g) {
                Collection defs = findAlertDefs(g, PageControl.PAGE_ALL);  
                
                for (Iterator i=defs.iterator(); i.hasNext(); ) {
                    GalertDef def = (GalertDef)i.next();
                    
                    reloadAlertDef(def);
                }
                _log.debug("Group members changed for group [" + g + "]");
            }
        });
        
        GalertProcessor.getInstance().startupInitialize(_defDAO.findAll());
    }
    
    public static GalertManagerLocal getOne() {
        try {
            return GalertManagerUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }

    public void ejbCreate() {}
    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void setSessionContext(SessionContext ctx) {}
}
