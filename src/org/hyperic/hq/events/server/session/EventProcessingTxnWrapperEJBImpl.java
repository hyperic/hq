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

package org.hyperic.hq.events.server.session;


import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.shared.EventProcessingTxnWrapperLocal;
import org.hyperic.hq.events.shared.EventProcessingTxnWrapperUtil;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.TriggerInterface;

/**
 * Wraps trigger event processing in a new container managed transaction.
 * 
 * @ejb:bean name="EventProcessingTxnWrapper"
 *      jndi-name="ejb/event/EventProcessingTxnWrapper"
 *      local-jndi-name="LocalEventProcessingTxnWrapper"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:transaction type="REQUIRED"
 *
 */
public class EventProcessingTxnWrapperEJBImpl implements SessionBean {

    private Log log = LogFactory.getLog(EventProcessingTxnWrapperEJBImpl.class);
    
    /**
     * Invoke trigger event processing within a new container managed transaction.
     * 
     * @param trigger The trigger.
     * @param event The event to process.
     * @throws SystemException if event processing fails. This exception will 
     *                         cause the transaction to be rolled back.
     * @ejb:interface-method
     */
    public void processEvent(TriggerInterface trigger, AbstractEvent event) 
        throws SystemException {
        try {
            trigger.processEvent(event);
        } catch (Exception e) {
            // Throwing this runtime exception rolls back the transaction.
            throw new SystemException(e);
        }
    }
    
    public static EventProcessingTxnWrapperLocal getOne() {
        try {
            return EventProcessingTxnWrapperUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
    
    public void ejbCreate() {}
    public void ejbPostCreate() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbRemove() {}
    public void setSessionContext(SessionContext ctx) {}
}
