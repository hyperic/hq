package org.hyperic.hq.common.server.session;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.dao.DAOFactory;
import org.hibernate.NonUniqueObjectException;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.EJBException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Iterator;

/**
 * Session bean controls transaction
 * @ejb:bean name="TransactionManager"
 *      jndi-name="ejb/common/TransactionManager"
 *      local-jndi-name="LocalTransactionManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:transaction type="Required"
 */
public class TransactionManagerEJBImpl implements SessionBean
{
    /**
     * @ejb:interface-method
     */
    public void delete(PersistedObject p) {
        DAOFactory.getDAOFactory().getDAO(p.getClass()).removePersisted(p);
    }

    /**
     * @ejb:interface-method
     */
    public void save(PersistedObject p) {
        DAOFactory.getDAOFactory().getDAO(p.getClass()).savePersisted(p);
    }

    /**
     * save list of persistedobjects
     * @ejb:interface-method
     */
    public void save(List lp) {
        savePersistedList(lp);
    }

    /**
     * @ejb:interface-method
     * @ejb:transaction type="RequiresNew"
     */
    public void saveReqNew(PersistedObject p) {
        DAOFactory.getDAOFactory().getDAO(p.getClass()).savePersisted(p);
    }

    /**
     * save list of persistedobjects in new transaction
     * @ejb:interface-method
     * @ejb:transaction type="RequiresNew"
     */
    public void saveReqNew(List lp) {
        savePersistedList(lp);
    }

    /**
     * @ejb:interface-method
     */
    public PersistedObject findPersisted(PersistedObject p) {
        try {
            return DAOFactory.getDAOFactory().getDAO(p.getClass())
                    .findPersisted(p);
        } catch (NonUniqueObjectException e) {
            // ok
            return null;
        }
    }

    private void savePersistedList(List lp)
    {
        DAOFactory factory = DAOFactory.getDAOFactory();
        for (Iterator i = lp.iterator(); i.hasNext();) {
            PersistedObject p = (PersistedObject)i.next();
            factory.getDAO(p.getClass()).savePersisted(p);
        }
    }

    public void setSessionContext(SessionContext sessionContext) throws EJBException, RemoteException
    {
    }

    public void ejbRemove() throws EJBException, RemoteException
    {
    }

    public void ejbActivate() throws EJBException, RemoteException
    {
    }

    public void ejbPassivate() throws EJBException, RemoteException
    {
    }

    public void ejbCreate() { }
}
