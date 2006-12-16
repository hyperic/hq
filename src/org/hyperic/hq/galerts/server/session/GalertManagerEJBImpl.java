package org.hyperic.hq.galerts.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.server.session.Crispo;
import org.hyperic.hq.common.shared.CrispoManagerLocal;
import org.hyperic.hq.common.shared.CrispoManagerUtil;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.events.server.session.Escalation;
import org.hyperic.hq.events.server.session.EscalationMediator;
import org.hyperic.hq.galerts.processor.GalertProcessor;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyInfo;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyType;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyTypeInfo;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GalertDefPartition;
import org.hyperic.hq.galerts.server.session.ExecutionReason;
import org.hyperic.hq.galerts.server.session.GalertLog;
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

    private ExecutionStrategyTypeInfoDAO _stratTypeDAO;
    private GalertDefDAO                 _defDAO;
    private GalertLogDAO                 _logDAO;
    private CrispoManagerLocal           _crispoMan;
    
    public GalertManagerEJBImpl() {
        DAOFactory f = DAOFactory.getDAOFactory();
        
        _stratTypeDAO = f.getExecutionStrategyTypeInfoDAO();
        _defDAO       = new GalertDefDAO(f);
        _logDAO       = f.getGalertLogDAO();
        
        try {
            _crispoMan = CrispoManagerUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }

    /**
     * Update basic properties of an alert definition
     * 
     * If any of the passed params are non-null, they will be updated with
     * the new value
     * 
     * @ejb:interface-method  
     */
    public void update(GalertDef def, String name, String desc, Boolean enabled)
    {
        if (name != null)
            def.setName(name);
        if (desc != null)
            def.setDescription(desc);
        if (enabled != null)
            def.setEnabled(enabled.booleanValue());
        
        // TODO:  Re-configure the def in the GalertProcessor, since alerts
        //        will need to be sent out with a different name or be
        //        disabled.
    }
    
    /**
     * Update the escalation of an alert def
     * 
     * @ejb:interface-method  
     */
    public void update(GalertDef def, Escalation escalation) {
        def.setEscalation(escalation);
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
        GalertDef def = _defDAO.findById(id);
        return def.getGalertDefValue();
    }
    
    /**
     * @ejb:interface-method  
     */
    public GalertLog createAlertLog(GalertDef def, ExecutionReason reason) { 
        GalertLog newLog = new GalertLog(def, reason, 
                                         System.currentTimeMillis());
        
        _logDAO.save(newLog);
        return newLog;
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
    public List findAlertLogs(ResourceGroup group) {
        return _logDAO.findAll(group);
    }

    /**
     * @ejb:interface-method  
     */
    public PageList findAlertLogsByTimeWindow(ResourceGroup group, long begin,
                                              PageControl pc) {
        return _logDAO.findByTimeWindow(group, begin, pc);
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
        Iterator j=configs.iterator();
        for (Iterator i=triggerInfos.iterator(); i.hasNext(); ) {
            GtriggerTypeInfo typeInfo = (GtriggerTypeInfo)i.next();
            ConfigResponse config = (ConfigResponse)j.next();
            Crispo crispo = _crispoMan.create(config);
            GtriggerInfo t = strat.addTrigger(typeInfo, crispo, def.getGroup(), 
                                              partition); 
 
            _defDAO.save(t);
        }
        GalertProcessor.getInstance().alertDefUpdated(def);
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
        GalertProcessor.getInstance().alertDefUpdated(def);
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
     * Delete an alert definition.  The caller must unload it from the
     * {@link GalertProcessor}
     * 
     * @ejb:interface-method
     */  
    public void deleteAlertDef(GalertDef def) {
        List nukeCrispos = new ArrayList();
        Integer defId = def.getId();
        
        for (Iterator i=def.getStrategies().iterator(); i.hasNext(); ) {
            ExecutionStrategyInfo strat = (ExecutionStrategyInfo)i.next();
            
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
     * start escalation inside JTA context for Galerts
     * @ejb:interface-method
     */
    public void startEscalation(Integer id, ExecutionReason reason)
    {
        try {
            GalertDef def = _defDAO.findById(id);
            EscalationMediator.getInstance().startGEscalation(def, reason);
        } catch (ActionExecuteException e) {
            _log.error("Unable to execute actions", e);
        } catch (PermissionException e) {
            _log.error("No permission to begin escalation", e);
        }
    }

    /**
     * @ejb:interface-method  
     */
    public void startup() {
        _log.warn("Galert manager starting up!");
        GalertProcessor.getInstance().startupInitialize(_defDAO.findAll());
    }
    
    public void ejbCreate() {}
    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void setSessionContext(SessionContext ctx) {}
}
