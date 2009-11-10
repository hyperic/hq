/* Copyright 2009 SpringSource Inc. All Rights Reserved. */

package org.hyperic.hq.bizapp.server.mdb;

import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.ConfigManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ServerManagerEJBImpl;
import org.hyperic.hq.autoinventory.server.session.AutoinventoryManagerImpl;
import org.hyperic.hq.control.ControlEvent;
import org.hyperic.hq.control.shared.ControlConstants;
import org.hyperic.hq.control.server.session.ServerRestartHandler;
import org.hyperic.hq.measurement.server.session.TrackerManagerImpl;

/**
 * 
 * 
 * @ejb:bean name="ServerRestartHandler"
 *           jndi-name="ejb/bizapp/ServerRestartHandler"
 *           local-jndi-name="ServerRestartHandler" transaction-type="Container"
 *           acknowledge-mode="Auto-acknowledge"
 *           destination-type="javax.jms.Topic"
 * 
 * @ejb:transaction type="REQUIRED"
 * 
 * @jboss:destination-jndi-name name="topic/eventsTopic"
 */
public class ServerRestartHandlerEJBImpl implements MessageDrivenBean, MessageListener {

    private ServerRestartHandler serverRestartHandler;

    private Log log = LogFactory.getLog(ServerRestartHandlerEJBImpl.class);

    public void onMessage(Message message) {
        if (message instanceof ObjectMessage) {
            try {
                final Object messageObject = ((ObjectMessage) message).getObject();
                if (messageObject instanceof ControlEvent) {
                    ControlEvent event = (ControlEvent) messageObject;
                    if ((event.getAction().equals("restart") || event.getAction().equals("start")) &&
                        event.getStatus().equals(ControlConstants.STATUS_COMPLETED) && event.getResource().isServer())
                    {
                        try {
                            serverRestartHandler.serverRestarted(event.getResource());
                        } catch (Exception e) {
                            log.error("Error processing possible server restart event", e);
                        }
                    }
                }
            } catch (JMSException e) {
                log.error("Error processing possible server restart event", e);
            }
        }
    }

    /**
     * @see javax.ejb.MessageDrivenBean#ejbCreate()
     * @ejb:create-method
     */
    public void ejbCreate() {
        serverRestartHandler = new ServerRestartHandler(ServerManagerEJBImpl.getOne(),
                                                        ConfigManagerEJBImpl.getOne(),
                                                        AutoinventoryManagerImpl.getOne(),
                                                        TrackerManagerImpl.getOne());
    }

    /**
     * @see javax.ejb.MessageDrivenBean#ejbPostCreate()
     */
    public void ejbPostCreate() {
    }

    /**
     * @see javax.ejb.MessageDrivenBean#ejbActivate()
     */
    public void ejbActivate() {
    }

    /**
     * @see javax.ejb.MessageDrivenBean#ejbPassivate()
     */
    public void ejbPassivate() {
    }

    /**
     * @see javax.ejb.MessageDrivenBean#ejbRemove()
     * @ejb:remove-method
     */
    public void ejbRemove() {
    }

    public void setMessageDrivenContext(MessageDrivenContext ctx) {
    }
}
