package org.hyperic.hq.application.server.session;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

/**
 * @ejb:bean name="TestManager"
 *      jndi-name="ejb/appdef/TestManager"
 *      local-jndi-name="LocalTestManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 * @ejb:transaction type="REQUIRED"
 */
public class TestManagerEJBImpl 
    implements SessionBean 
{
    /**
     * @ejb:interface-method
     */
    public void throwException() throws Exception {
        throw new Exception();
    }
    
    /**
     * @ejb:interface-method
     */
    public void noop() {
        return;
    }
    
    public void setSessionContext(SessionContext ctx) {}
    public void ejbCreate() {}
    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
}
