package org.hyperic.hq.common.server.session;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.hyperic.hq.TransactionContext;

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
     * execute custom action in JTA transaction context.
     * @ejb:interface-method
     * @ejb:transaction type="RequiresNew"
     */
    public TransactionContext execute(TransactionContext context)
    {
        return context.run(context);
    }

    /**
     * execute custom action in JTA transaction context.
     * @ejb:interface-method
     */
    public TransactionContext executeReqNew(TransactionContext context)
    {
        return context.run(context);
    }

    public void setSessionContext(SessionContext sessionContext) {}
    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbCreate() { }
}
