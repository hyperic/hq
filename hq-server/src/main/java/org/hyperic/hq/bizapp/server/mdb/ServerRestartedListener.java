/* Copyright 2009 SpringSource Inc. All Rights Reserved. */

package org.hyperic.hq.bizapp.server.mdb;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.control.ControlEvent;
import org.hyperic.hq.control.server.session.ServerRestartHandler;
import org.hyperic.hq.control.shared.ControlConstants;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Bound to topic/eventsTopic
 */
public class ServerRestartedListener implements MessageListener {

    private ServerRestartHandler serverRestartHandler;

    private final Log log = LogFactory.getLog(ServerRestartedListener.class);

    @Autowired
    public ServerRestartedListener(ServerRestartHandler serverRestartHandler) {
        this.serverRestartHandler = serverRestartHandler;
    }



    public void onMessage(Message message) {
        if (message instanceof ObjectMessage) {
            try {
                final Object messageObject = ((ObjectMessage) message).getObject();
                if (messageObject instanceof ControlEvent) {
                    ControlEvent event = (ControlEvent) messageObject;
                    if ((event.getAction().equals("restart") || event.getAction().equals("start")) &&
                        event.getStatus().equals(ControlConstants.STATUS_COMPLETED) && event.getResource().isServer()) {
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

}
