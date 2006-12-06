package org.hyperic.hq.bizapp.server.session;

import java.util.Iterator;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManagerUtil;
import org.hyperic.hq.bizapp.shared.uibeans.AlertDefinitionBean;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.events.server.session.Escalation;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyInfo;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyType;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyTypeInfo;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GalertDefPartition;
import org.hyperic.hq.galerts.server.session.GtriggerType;
import org.hyperic.hq.galerts.server.session.GtriggerTypeInfo;
import org.hyperic.hq.galerts.shared.GalertManagerLocal;
import org.hyperic.hq.galerts.shared.GalertManagerUtil;
import org.hyperic.hq.galerts.shared.GtriggerManagerLocal;
import org.hyperic.hq.galerts.shared.GtriggerManagerUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;


/** 
* The BizApp's interface to the Events Subsystem
*
* @ejb:bean name="GalertBoss"
*      jndi-name="ejb/bizapp/GalertBoss"
*      local-jndi-name="LocalGalertBoss"
*      view-type="both"
*      type="Stateless"
* 
* @ejb:transaction type="REQUIRED"
*/
public class GalertBossEJBImpl 
   implements SessionBean 
{
    private final Log _log = LogFactory.getLog(GalertBossEJBImpl.class); 

    private SessionManager          _sessMan;
    private GalertManagerLocal      _galertMan;
    private GtriggerManagerLocal    _triggerMan;

    public GalertBossEJBImpl() {
        _sessMan = SessionManager.getInstance();
        try {
            _galertMan  = GalertManagerUtil.getLocalHome().create();
            _triggerMan = GtriggerManagerUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
    
    /**
     * @ejb:interface-method
     */
    public ExecutionStrategyTypeInfo 
        registerExecutionStrategy(int sessionId, 
                                  ExecutionStrategyType stratType)
        throws PermissionException, SessionException
    {
        _sessMan.authenticate(sessionId);
        return _galertMan.registerExecutionStrategy(stratType);
                          
    }

    /**
     * @ejb:interface-method
     */
    public ExecutionStrategyTypeInfo 
        findStrategyType(int sessionId, ExecutionStrategyType type)
        throws PermissionException, SessionException
    {
        _sessMan.authenticate(sessionId);
        return _galertMan.findStrategyType(type);
                          
    }
    
    /**
     * @ejb:interface-method
     */
    public GtriggerTypeInfo findTriggerType(int sessionId, GtriggerType type) 
        throws SessionException
    {
        _sessMan.authenticate(sessionId);
        return _triggerMan.findTriggerType(type);
    }
        
    /**
     * @ejb:interface-method
     */
    public GtriggerTypeInfo registerTriggerType(int sessionId, 
                                                GtriggerType type) 
        throws SessionException
    {
        _sessMan.authenticate(sessionId);
        return _triggerMan.registerTriggerType(type);
    }
        

    /**
     * @ejb:interface-method
     */
    public ExecutionStrategyInfo 
        addPartition(int sessionId, GalertDef def, GalertDefPartition partition, 
                     ExecutionStrategyTypeInfo stratType, 
                     ConfigResponse stratConfig)
        throws SessionException
    {
        _sessMan.authenticate(sessionId);
        return _galertMan.addPartition(def, partition, stratType, stratConfig);
    }
    

    /**
     * @ejb:interface-method
     */
    public GalertDef createAlertDef(int sessionId, String name,
                                    String description, AlertSeverity severity,
                                    boolean enabled, ResourceGroup group) 
        throws SessionException
    {
        AuthzSubjectValue subject = _sessMan.getSubject(sessionId);

        return _galertMan.createAlertDef(subject, name, description,
                                         severity, enabled, group);
    }

    /**
     * @ejb:interface-method
     */
    public void configureTriggers(int sessionId, GalertDef def, 
                                  GalertDefPartition partition,
                                  List triggerInfos, List configs)
        throws SessionException
    {
        _sessMan.authenticate(sessionId);
        _galertMan.configureTriggers(def, partition, triggerInfos, configs);
    }

    /**
     * Find all the group alert definitions for a given appdef group.
     * 
     * @return a collection of {@link AlertDefinitionBean}s
     * @throws PermissionException 
     * @ejb:interface-method
     */
    public PageList findDefinitions(int sessionId, Integer gid, PageControl pc)
        throws SessionException, PermissionException
    {
        PageList res = new PageList();
        
        AuthzSubjectValue subj = _sessMan.getSubject(sessionId);
        
        // Find the ResourceGroup
        ResourceGroup g;
        try {
            g = ResourceGroupManagerUtil.getLocalHome().create()
                .findResourceGroupById(subj, gid);
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
        
        PageList alertDefs = _galertMan.findAlertDefs(g, pc);
        for (Iterator i= alertDefs.iterator(); i.hasNext(); ) {
            GalertDef def = (GalertDef)i.next();
            
            res.add(new AlertDefinitionBean(def.getId(), 0, def.getName(),
                                            def.getDescription(),
                                            def.isEnabled(), null));
        }
        res.setTotalSize(alertDefs.getTotalSize());
        return res;
    }

    /**
     * @ejb:interface-method
     */
    public void deleteAlertDef(int sessionId, GalertDef def) 
        throws SessionException
    {
        _sessMan.authenticate(sessionId);
        _galertMan.deleteAlertDef(def);
    }    

    /**
     * @ejb:interface-method
     */
    public GalertDef findDefinition(int sessionId, Integer id)
        throws SessionException
    {
        _sessMan.authenticate(sessionId);
        return _galertMan.findById(id);
    }    

    /**
     * @ejb:interface-method  
     */
    public void update(int sessionId, GalertDef def, String name, String desc, 
                       Boolean enabled)
        throws SessionException
    {
        _sessMan.authenticate(sessionId);
        _galertMan.update(def, name, desc, enabled);
    }
    
    /**
     * @ejb:interface-method  
     */
    public void update(int sessionId, GalertDef def, Escalation escalation) 
        throws SessionException
    {
        _sessMan.authenticate(sessionId);
        _galertMan.update(def, escalation);
    }

    /**
    * @ejb:create-method
    */
   public void ejbCreate() {}
   public void ejbRemove() {}
   public void ejbActivate() {}
   public void ejbPassivate() {}
   public void setSessionContext(SessionContext ctx) {}
}
