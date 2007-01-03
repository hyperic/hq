package org.hyperic.hq.galerts.server.session;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.galerts.server.session.GtriggerTypeInfo;
import org.hyperic.hq.galerts.server.session.GtriggerType;

/**
 * @ejb:bean name="GtriggerManager"
 *      jndi-name="ejb/galerts/GtriggerManager"
 *      local-jndi-name="LocalGtriggerManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 * @ejb:transaction type="REQUIRED"
 */
public class GtriggerManagerEJBImpl 
    implements SessionBean 
{
    private final Log _log = LogFactory.getLog(GtriggerManagerEJBImpl.class);

    private GtriggerTypeInfoDAO _ttypeDAO;
    
    public GtriggerManagerEJBImpl() {
        _ttypeDAO = new GtriggerTypeInfoDAO(DAOFactory.getDAOFactory()); 
    }

    /**
     * @ejb:interface-method
     */
    public GtriggerTypeInfo findTriggerType(GtriggerType type) {
        return _ttypeDAO.find(type);
    }
    
    /**
     * Register a trigger type.  
     * 
     * @param triggerType Trigger type to register
     * @return the persisted metadata about the trigger type
     * @ejb:interface-method
     */
    public GtriggerTypeInfo registerTriggerType(GtriggerType triggerType) {
        GtriggerTypeInfo res;
     
        res = _ttypeDAO.find(triggerType);
        if (res != null) {
            _log.warn("Attempted to register GtriggerType class [" + 
                      triggerType.getClass() + "] but it was already " + 
                      "registered");
            return res;
        }
        res = new GtriggerTypeInfo(triggerType.getClass());
        _ttypeDAO.save(res);
        return res;
    }
    
    /**
     * Unregister a trigger type.  This method will fail if any alert
     * definitions are using triggers of this type.
     * 
     * @param triggerType Trigger type to unregister
     * @ejb:interface-method
     */
    public void unregisterTriggerType(GtriggerType triggerType) {
        GtriggerTypeInfo info = _ttypeDAO.find(triggerType);
        
        if (info == null) {
            _log.warn("Tried to unregister a trigger type which was not " + 
                      "registered");
            return;
        }
            
        _ttypeDAO.remove(info);
    }
    
    public void ejbCreate() {}
    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void setSessionContext(SessionContext ctx) {}
}
