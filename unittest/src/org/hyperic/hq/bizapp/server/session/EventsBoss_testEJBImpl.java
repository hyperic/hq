/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.bizapp.server.session;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.bizapp.shared.EventsBossLocal;
import org.hyperic.hq.bizapp.shared.EventsBoss_testLocal;
import org.hyperic.hq.bizapp.shared.EventsBoss_testUtil;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.ext.AbstractTrigger;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.events.server.session.AlertCondition;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.junit.Assert;

/**
 * @ejb:bean name="EventsBoss_test"
 *      jndi-name="ejb/bizapp/EventsBoss_test"
 *      local-jndi-name="LocalEventsBoss_test"
 *      view-type="local"
 *      type="Stateless"
 * 
 * @ejb:util generate="physical"
 * @ejb:transaction type="NotSupported"
 */
public class EventsBoss_testEJBImpl implements SessionBean {
    
    private transient Log _log = LogFactory.getLog(EventsBoss_testEJBImpl.class);
    
    /**
     * @ejb:interface-method
     */
    public void testUpdateAlertDefinition() throws Exception {
        final AlertDefinitionManagerLocal adMan = getADMan();
        final AlertDefinition def =
            adMan.findAlertDefinitionById(new Integer(10100));
        final EventsBossLocal eBoss = getEventsBoss();
        final AuthzSubjectManagerLocal authMan = getAuthMan();
        final AuthzSubject subj = authMan.getOverlordPojo();
        final int sessionID = SessionManager.getInstance().put(subj);
        try {
            eBoss.updateAlertDefinition(sessionID, def.getAlertDefinitionValue());
        } catch (Exception e) {
            _log.error(e, e);
        }
        final Collection children = def.getChildren();
        for (final Iterator it=children.iterator(); it.hasNext(); ) {
            final AlertDefinition child = (AlertDefinition)it.next();
            Assert.assertTrue(matches(def, child));
        }
    }
    
    private boolean matches(AlertDefinition def, AlertDefinition child)
        throws EncodingException
    {
        final Collection defActions = def.getActions();
        final Collection childActions = child.getActions();
        // need to check parentId, configResponse, class
        for (final Iterator xx=defActions.iterator(); xx.hasNext(); ) {
            final Action defAction = (Action)xx.next();
            boolean matches = false;
            for (final Iterator yy=childActions.iterator(); yy.hasNext(); ) {
                final Action childAction = (Action)yy.next();
                if (matches(defAction, childAction)) {
                    matches = true;
                    break;
                }
            }
            Assert.assertTrue(defAction.toString(), matches);
        }
        final Collection defTriggers = def.getTriggers();
        final Collection childTriggers = child.getTriggers();
        for (final Iterator xx=defTriggers.iterator(); xx.hasNext(); ) {
            final AbstractTrigger defTrigger = (AbstractTrigger)xx.next();
            boolean matches = false;
            for (final Iterator yy=childTriggers.iterator(); yy.hasNext(); ) {
                final AbstractTrigger childTrigger = (AbstractTrigger)yy.next();
                if (matches(defTrigger, childTrigger)) {
                    matches = true;
                    break;
                }
            }
            Assert.assertTrue(defTrigger.toString(), matches);
        }
        final Collection defConditions = def.getConditions();
        final Collection childConditions = child.getConditions();
        for (final Iterator xx=defConditions.iterator(); xx.hasNext(); ) {
            final AlertCondition defCond = (AlertCondition)xx.next();
            boolean matches = false;
            for (final Iterator yy=childConditions.iterator(); yy.hasNext(); ) {
                final AlertCondition childCond = (AlertCondition)yy.next();
                if (matches(defCond, childCond)) {
                    matches = true;
                    break;
                }
            }
            Assert.assertTrue(defCond.toString(), matches);
        }

        def.getConditions();
        return true;
    }

    private boolean matches(AlertCondition c1, AlertCondition c2) {
        boolean res = c1.getComparator().equals(c2.getComparator());
        res = res && c1.getThreshold() == c2.getThreshold();
        res = res && c1.getName() == c2.getName();
        return res;
    }

    private boolean matches(AbstractTrigger t1, AbstractTrigger t2) {
        boolean res = new ConfigResponse(t1.getConfigSchema()).equals(
                new ConfigResponse(t2.getConfigSchema()));
        res = res && t1.getFrequency() == t2.getFrequency();
        res = res && t1.getClass().equals(t2.getClass());
        return res;
    }

    private boolean matches(Action a1, Action a2) throws EncodingException {
        boolean res = a1.getClassName().equals(a2.getClassName());
        res = res && a1.getParent().getId().equals(a2.getParent().getId());
        res = res && ConfigResponse.decode(a1.getConfig()).equals(
            ConfigResponse.decode(a2.getConfig()));
        return res;
    }

    private final AuthzSubjectManagerLocal getAuthMan() {
        return AuthzSubjectManagerEJBImpl.getOne();
    }

    private final EventsBossLocal getEventsBoss() {
        return EventsBossEJBImpl.getOne();
    }
    
    private final AlertDefinitionManagerLocal getADMan() {
        return AlertDefinitionManagerEJBImpl.getOne();
    }

    public static EventsBoss_testLocal getOne() {
        try {
            return EventsBoss_testUtil.getLocalHome().create();
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
