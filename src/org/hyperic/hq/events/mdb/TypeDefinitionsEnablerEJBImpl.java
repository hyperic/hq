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

package org.hyperic.hq.events.mdb;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEvent;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.bizapp.server.session.BizappSessionEJB;
import org.hyperic.hq.bizapp.shared.EventsBossLocal;
import org.hyperic.hq.bizapp.shared.EventsBossUtil;
import org.hyperic.hq.common.SystemException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** The DefaultMetricsEnabler Message-Drive Bean is intended to pick up
 * AppdefEvents and enable measurements whenever appdef entities are created
 *
 * @ejb:bean name="TypeDefinitionsEnabler"
 *      jndi-name="ejb/events/TypeDefinitionsEnabler"
 *      local-jndi-name="LocalTypeDefinitionsEnabler"
 *      transaction-type="Container"
 *      acknowledge-mode="Auto-acknowledge"
 *      destination-type="javax.jms.Topic"
 *
 * @jboss:destination-jndi-name name="topic/eventsTopic"
 */
public class TypeDefinitionsEnablerEJBImpl
    extends BizappSessionEJB implements MessageDrivenBean, MessageListener {

    private final Log log = LogFactory
            .getLog("org.hyperic.hq.bizapp.server.mdb.TypeDefinitionsEnablerEJBImpl");

    private MessageDrivenContext ctx = null;

    private static EventsBossLocal eboss = null;
    
    private EventsBossLocal getEBoss() {
        if (eboss == null) {
            try {
                eboss = EventsBossUtil.getLocalHome().create();
            } catch (CreateException e) {
                throw new SystemException(e);
            } catch (NamingException e) {
                throw new SystemException(e);
            }
        }
        return eboss;
    }

    public void onMessage(Message inMessage) {
        if (!(inMessage instanceof ObjectMessage)) { return; }

        try {
            ObjectMessage om = (ObjectMessage) inMessage;
            Object obj = om.getObject();

            if (!(obj instanceof AppdefEvent)) return;

            AppdefEvent ae = (AppdefEvent) obj;

            if (ae.getAction() == AppdefEvent.ACTION_METRIC_ENABLED) {
                AppdefEntityID id = ae.getResource();
                AuthzSubjectValue subject = ae.getSubject();
                
                getEBoss().inheritResourceTypeAlertDefinition(subject, id);
            }
        } catch (Exception e) {
            log.error("Failed to handle TypeDefinitionsEnabler message: "+e, e);
        } 
    }

    /** @ejb:create-method */
    public void ejbCreate() {}

    public void ejbPostCreate() {}

    public void ejbActivate() {}

    public void ejbPassivate() {}

    /** @ejb:remove-method */
    public void ejbRemove() {
        this.ctx = null;
    }

    public void setMessageDrivenContext(MessageDrivenContext ctx)
        throws EJBException {
        this.ctx = ctx;
    }
}
