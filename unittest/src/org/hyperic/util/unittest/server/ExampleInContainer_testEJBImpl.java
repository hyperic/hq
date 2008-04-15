/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.util.unittest.server;

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import junit.framework.Assert;

import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.util.unittest.server.ExampleInContainer_testLocal;
import org.hyperic.util.unittest.server.ExampleInContainer_testUtil;

/**
 * The session bean implementing the in-container unit tests for the 
 * ExampleInContainer_test. Note that the transaction type should 
 * always be set to "NOTSUPPORTED" since we don't want the unit testing 
 * framework modifying the transactional behavior of the target component 
 * we are testing.
 * 
* @ejb:bean name="ExampleInContainer_test"
*      jndi-name="ejb/authz/ExampleInContainer_test"
*      local-jndi-name="LocalExampleInContainer_test"
*      view-type="local"
*      type="Stateless"
* 
* @ejb:util generate="physical"
* @ejb:transaction type="NOTSUPPORTED"
*/
public class ExampleInContainer_testEJBImpl implements SessionBean {
    
    /**
     * The {@link #getOne()} factory method is necessary for in-container unit tests.
     * 
     * @return The local interface.
     */
    public static ExampleInContainer_testLocal getOne() {
        try {
            return ExampleInContainer_testUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
    
    /**
     * Session bean method providing the environment within which the target 
     * component (AlertDefinitionManager) may be tested.
     * 
     * @ejb:interface-method
     */
    public void testQueryAlertDefinitionManager() throws Exception {        
        AlertDefinitionManagerLocal adMan = AlertDefinitionManagerEJBImpl.getOne();
        
        Integer id = adMan.getIdFromTrigger(new Integer(-1));
        
        Assert.assertNull("shouldn't have found alert def id", id);
    }

    public void ejbCreate() throws CreateException {}
    public void ejbActivate() throws EJBException, RemoteException {}
    public void ejbPassivate() throws EJBException, RemoteException {}
    public void ejbRemove() throws EJBException, RemoteException {}
    public void setSessionContext(SessionContext arg0) {}

}
