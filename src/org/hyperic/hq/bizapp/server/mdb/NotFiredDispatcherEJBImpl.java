/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.bizapp.server.mdb;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.naming.NamingException;

import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.events.shared.TriggerTrackerLocal;
import org.hyperic.hq.events.shared.TriggerTrackerUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** The NotFiredDispatcher Message-Drive Bean is intended to be used
 * to remove filtering for triggers that have fired
 * <p>
 *
 * </p>
 * @ejb:bean name="NotFiredDispatcher"
 *      jndi-name="ejb/bizapp/NotFiredDispatcher"
 *      local-jndi-name="LocalNotFiredDispatcher"
 *      transaction-type="Container"
 *      acknowledge-mode="Auto-acknowledge"
 *      destination-type="javax.jms.Topic"
 *
 * @jboss:destination-jndi-name name="topic/eventsTopic"
 *
 */

public class NotFiredDispatcherEJBImpl 
    implements MessageDrivenBean, MessageListener {
    private final Log log =
        LogFactory.getLog(
            "org.hyperic.hq.bizapp.server.mdb.NotFiredDispatcher");

    private MessageDrivenContext ctx = null;

    private TriggerTrackerLocal tracker = null;

    ///////////////////////////////////////
    // operations

    /**
     * The onMessage method
     */
    public void onMessage(Message inMessage) {
        if (!(inMessage instanceof ObjectMessage)) {
            return;
        }

        try {
            ObjectMessage om = (ObjectMessage) inMessage;
            Object obj = om.getObject();

            if (tracker == null)
                tracker = TriggerTrackerUtil.getLocalHome().create();

            if (obj instanceof TriggerNotFiredEvent) {
                // Remove tracking
                TriggerNotFiredEvent tnfe = (TriggerNotFiredEvent) obj;
                tracker.remove(tnfe.getInstanceId());
            }
        } catch (JMSException e) {
            log.error("Cannot open message object", e);
            e.printStackTrace();
        } catch (NamingException e) {
            // Change to warning so that it won't show up in startup log
            log.warn("Cannot lookup EventLogManager", e);
        } catch (CreateException e) {
            log.error("Cannot create EventLogManager", e);
        }
    }

    ///////////////////////////////////////
    // EJB operations

    /**
     * @see javax.ejb.MessageDrivenBean#ejbCreate()
     * @ejb:create-method
     */
    public void ejbCreate() {}

    /**
     * @see javax.ejb.MessageDrivenBean#ejbPostCreate()
     */
    public void ejbPostCreate() {}

    /**
     * @see javax.ejb.MessageDrivenBean#ejbActivate()
     */
    public void ejbActivate() {}

    /**
     * @see javax.ejb.MessageDrivenBean#ejbPassivate()
     */
    public void ejbPassivate() {}

    /**
     * @see javax.ejb.MessageDrivenBean#ejbRemove()
     * @ejb:remove-method
     */
    public void ejbRemove() {
        this.ctx = null;
    }

    public void setMessageDrivenContext(MessageDrivenContext ctx)
        throws EJBException {
        this.ctx = ctx;
    }

}
