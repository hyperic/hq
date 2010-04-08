package org.hyperic.hq.authz.server.session;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import junit.framework.Assert;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerTestLocal;
import org.hyperic.hq.authz.shared.ResourceManagerTestUtil;
import org.hyperic.hq.common.SystemException;

/**
 * The session bean implementing the in-container unit tests for the 
 * AuthzSubjectManager.
 * 
 * @ejb:bean name="ResourceManagerTest"
 *      jndi-name="ejb/authz/ResourceManagerTest"
 *      local-jndi-name="LocalResourceManagerTest"
 *      view-type="local"
 *      type="Stateless"
 * 
 * @ejb:util generate="physical"
 * @ejb:transaction type="NotSupported"
 */
public class ResourceManagerTestEJBImpl implements SessionBean {
    /**
     * @ejb:interface-method
     */
    public void testFindViewableSvcResources() throws Exception {
        testFindViewableSvcResources("joe", 15);
        testFindViewableSvcResources("hqadmin", 1264);
        testFindViewableSvcResources("nipuna11", 901);
        testFindViewableSvcResources("nipuna2", 901);
        testFindViewableSvcResources("nipuna3", 901);
        testFindViewableSvcResources("guest", 15);
    }

    private void testFindViewableSvcResources(String user, int expectedResources) {
        AuthzSubjectManagerLocal authMan = AuthzSubjectManagerEJBImpl.getOne();
        AuthzSubject subj = authMan.findSubjectByName(user);
        // it would be nice to test the Manager method rather than the DAO, but since it is an 
        // ee feature, it makes that a big more difficult.  The Manager calls 
        // findViewableSvcRes_orderName() eventually anyway.
        Collection list = getResourceDAO().findViewableSvcRes_orderName(subj.getId(), Boolean.FALSE);
        // List list = getResourceMan().findViewableSvcResources(subj, null, PageControl.PAGE_ALL);
        Set toTest = new HashSet(list);
        Assert.assertEquals(
            "There are duplicate service resources returned from findViewableSvcResources()",
            list.size(), toTest.size());
        Assert.assertEquals(expectedResources, toTest.size());
    }
    
    private ResourceDAO getResourceDAO() {
        return new ResourceDAO(DAOFactory.getDAOFactory());
    }

    public static ResourceManagerTestLocal getOne() {
        try {
            return ResourceManagerTestUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public void ejbCreate() throws CreateException {}
    public void ejbActivate() throws EJBException, RemoteException {}
    public void ejbPassivate() throws EJBException, RemoteException {}
    public void ejbRemove() throws EJBException, RemoteException {}
    public void setSessionContext(SessionContext arg0) {}
}
