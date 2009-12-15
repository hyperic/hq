package org.hyperic.hq.escalation.server.session;

import java.rmi.RemoteException;
import java.util.Collection;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import junit.framework.Assert;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.escalation.shared.EscalationManagerLocal;
import org.hyperic.hq.escalation.shared.EscalationManagerTestLocal;
import org.hyperic.hq.escalation.shared.EscalationManagerTestUtil;
import org.hyperic.hq.events.server.session.AlertManagerEJBImpl;
import org.hyperic.hq.events.shared.AlertManagerLocal;
import org.hyperic.hq.measurement.MeasurementConstants;

/**
 * The session bean implementing the in-container unit tests for the 
 * AuthzSubjectManager.
 * 
 * @ejb:bean name="EscalationManagerTest"
 *      jndi-name="ejb/authz/EscalationManagerTest"
 *      local-jndi-name="LocalEscalationManagerTest"
 *      view-type="local"
 *      type="Stateless"
 * 
 * @ejb:util generate="physical"
 * @ejb:transaction type="Bean"
 */
public class EscalationManagerTestEJBImpl implements SessionBean {
    
    public static EscalationManagerTestLocal getOne() {
        try {
            return EscalationManagerTestUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }
    
    /**
     * @ejb:interface-method
     */
    public void testExecuteStateWithInvalidAlertId() throws Exception {
        EscalationManagerLocal eMan = EscalationManagerEJBImpl.getOne();
        AlertManagerLocal aMan = AlertManagerEJBImpl.getOne();
        EscalationState state = new EscalationState();
        state.setAcknowledgedBy(null);
        state.setNextAction(0);
        state.setNextActionTime(System.currentTimeMillis()+MeasurementConstants.DAY);
        int alertId = 10100;
        Escalation esc = eMan.findById(new Integer(10100));
        state.setEscalation(esc);
        state.setAlertId(alertId);
        state.setAlertDefinitionId(10100);
        state.setAlertTypeEnum(-559038737);
        getOne().runEscalation(state);
        aMan.deleteAlerts(new Integer[] {new Integer(alertId)});
        Assert.assertNull(
            "alert " + alertId + " should not exist", aMan.getAlertById(new Integer(alertId)));
        try {
            // should exit without any errors
            eMan.executeState(state.getId());
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage(), false);
            throw e;
        }
        Assert.assertNull(
            "escalationStateId " + state.getId() + " should not exist",
            getEscalationStateDAO().get(state.getId()));
    }
    
    /**
     * @ejb:transaction type="Required"
     * @ejb:interface-method
     */
    public void runEscalation(EscalationState state) {
        getEscalationStateDAO().save(state);
        EscalationRuntime.getInstance().scheduleEscalation(state);
    }
    
    private EscalationStateDAO getEscalationStateDAO() {
        return new EscalationStateDAO(DAOFactory.getDAOFactory());
    }

    public void ejbCreate() throws CreateException {}
    public void ejbActivate() throws EJBException, RemoteException {}
    public void ejbPassivate() throws EJBException, RemoteException {}
    public void ejbRemove() throws EJBException, RemoteException {}
    public void setSessionContext(SessionContext arg0) {}
}
