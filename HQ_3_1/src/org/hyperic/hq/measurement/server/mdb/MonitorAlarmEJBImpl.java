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

package org.hyperic.hq.measurement.server.mdb;

import javax.ejb.EJBException;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.hyperic.hq.measurement.ext.ScheduledMonitor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @ejb:bean name="MonitorAlarm"
 *      jndi-name="ejb/measurement/MonitorAlarm"
 *      local-jndi-name="LocalMonitorAlarm"
 *      transaction-type="Container"
 *      acknowledge-mode="Auto-acknowledge"
 *      destination-type="javax.jms.Queue"
 *
 * @jboss:destination-jndi-name name="queue/monitorAlarmQueue"
 *
 */
public class MonitorAlarmEJBImpl implements MessageDrivenBean, MessageListener {
    private final Log log = LogFactory.getLog("org.hyperic.hq.measurement.server.mdb.MonitorAlarmEJBImpl");

    private MessageDrivenContext ctx = null;

    public void onMessage(Message inMessage) {
        if (inMessage instanceof ObjectMessage) {
            ObjectMessage om = (ObjectMessage) inMessage;
            Object obj = null;
            try {
                obj = om.getObject();
                ScheduledMonitor monitor = (ScheduledMonitor) obj;
                monitor.scheduleMeasurements();
            } catch (ClassCastException e) {
                log.error("Object is not a ScheduledMonitor, it is " +
                          obj.getClass() + ":" + e);
            } catch (JMSException e) {
                log.error("JMSException", e);
                e.printStackTrace();
            }
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

    /**
     * @see javax.ejb.MessageDrivenBean#setSessionContext()
     */
    public void setMessageDrivenContext(MessageDrivenContext ctx)
        throws EJBException {
        this.ctx = ctx;
    }

}
