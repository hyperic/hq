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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.control.ControlEvent;
import org.hyperic.hq.events.AlertFiredEvent;
import org.hyperic.hq.events.shared.EventLogManagerLocal;
import org.hyperic.hq.events.shared.EventLogManagerUtil;
import org.hyperic.hq.measurement.shared.ConfigChangedEvent;
import org.hyperic.hq.measurement.shared.ResourceLogEvent;

/** 
 * The LoggingDispatcher Message-Drive Bean is intended to be used
 * to log Events
 *
 * @ejb:bean name="LoggingDispatcher"
 *      jndi-name="ejb/bizapp/LoggingDispatcher"
 *      local-jndi-name="LocalLoggingDispatcher"
 *      transaction-type="Container"
 *      acknowledge-mode="Auto-acknowledge"
 *      destination-type="javax.jms.Topic"
 *
 * @jboss:destination-jndi-name name="topic/eventsTopic"
 */

public class LoggingDispatcherEJBImpl 
    implements MessageDrivenBean, MessageListener {
    private final Log log =
        LogFactory.getLog(
            "org.hyperic.hq.bizapp.server.mdb.LoggingDispatcherEJBImpl");

    private MessageDrivenContext ctx = null;
    private EventLogManagerLocal elMan = null;

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

            if (elMan == null)
                elMan = EventLogManagerUtil.getLocalHome().create();

            // Do logging
            if (obj instanceof ControlEvent) {
                ControlEvent ce = (ControlEvent) obj;
                elMan.createLog(ce, ce.getSubject(), ce.getStatus());
            }
            else if (obj instanceof ResourceLogEvent) {
                ResourceLogEvent re = (ResourceLogEvent) obj;
                elMan.createLog(re, re.getSource(), re.getLevelString());
            } else if (obj instanceof ConfigChangedEvent) {
                ConfigChangedEvent ce = (ConfigChangedEvent) obj;
                elMan.createLog(ce, ce.getSource(), "INF");
            } else if (obj instanceof AlertFiredEvent) {
                AlertFiredEvent ae = (AlertFiredEvent) obj;
                elMan.createLog(ae, ae.getAlertDefName(), "ALR");
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
