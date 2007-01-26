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

import javax.ejb.CreateException;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.MeasurementUnscheduleException;
import org.hyperic.hq.measurement.server.session.ScheduleArgs;
import org.hyperic.hq.measurement.server.session.UnScheduleArgs;
import org.hyperic.hq.measurement.shared.MeasurementProcessorUtil;

/**
 * <p>The AgentSchedule class runs as a daemon that asynchronously schedules
 * metrics with the agent.</p>
 *
 *
 * @ejb:bean name="AgentSchedule"
 *      jndi-name="ejb/measurement/AgentSchedule"
 *      local-jndi-name="LocalAgentSchedule"
 *      transaction-type="Container"
 *      acknowledge-mode="Auto-acknowledge"
 *      destination-type="javax.jms.Queue"
 *
 * @jboss:destination-jndi-name name="queue/agentScheduleQueue"
 *
 */
public class AgentScheduleEJBImpl
    implements MessageDrivenBean, MessageListener 
{
    private final Log log =
        LogFactory.getLog(AgentScheduleEJBImpl.class.getName());

    public void onMessage(final Message inMessage) {

        if (!(inMessage instanceof ObjectMessage)) {
            return;
        }
        
        try {
            ObjectMessage om = (ObjectMessage)inMessage;
            Object o = om.getObject();

            if (o instanceof AppdefEntityID) {
                AgentScheduleSynchronizer.schedule((AppdefEntityID) o);
            }
            else if (o instanceof ScheduleArgs) {
                ScheduleArgs args = (ScheduleArgs) o;
                AgentScheduleSynchronizer.schedule(args.getEntId());
            }
            else if (o instanceof UnScheduleArgs) {
                UnScheduleArgs args = (UnScheduleArgs) o;
                log.info("Unscheduling metrics for: " + args);
                MeasurementProcessorUtil.getLocalHome().create()
                    .unschedule(args.getAgentEntityID(), args.getUnscheduleEntities());
            }
            else {
                log.error("Unknown message type: " + o);
            }
        } catch (JMSException e) {
            log.debug("Could not get message.", e);
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (MeasurementUnscheduleException e) {
            log.error("Failed to unschedule measurements", e);
        }
    }

    /**
     * @ejb:create-method
     */
    public void ejbCreate() {
        if (log.isDebugEnabled())
         log.debug ("AgentScheduleEJBImpl lifecycle event: creation");
    }

    public void ejbPostCreate() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}

    /**
     * @ejb:remove-method
     */
    public void ejbRemove() {}
    public void setMessageDrivenContext(MessageDrivenContext ctx) {}
}
