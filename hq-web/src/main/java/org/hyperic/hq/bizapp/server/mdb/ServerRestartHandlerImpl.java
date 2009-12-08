/* Copyright 2009 SpringSource Inc. All Rights Reserved. */

package org.hyperic.hq.bizapp.server.mdb;

import javax.annotation.PostConstruct;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.ConfigManagerImpl;
import org.hyperic.hq.appdef.server.session.ServerManagerImpl;
import org.hyperic.hq.autoinventory.server.session.AutoinventoryManagerImpl;
import org.hyperic.hq.control.ControlEvent;
import org.hyperic.hq.control.server.session.ServerRestartHandler;
import org.hyperic.hq.control.shared.ControlConstants;
import org.hyperic.hq.measurement.server.session.TrackerManagerImpl;

/**
 * Uses transaction manager ("Required") and is bound to topic/eventsTopic 
 */
public class ServerRestartHandlerImpl implements MessageListener {

    private ServerRestartHandler serverRestartHandler;

    private Log log = LogFactory.getLog(ServerRestartHandlerImpl.class);

    @PostConstruct
    public void init() {
        serverRestartHandler = new ServerRestartHandler(ServerManagerImpl.getOne(),
                                                        ConfigManagerImpl.getOne(),
                                                        AutoinventoryManagerImpl.getOne(),
                                                        TrackerManagerImpl.getOne());
    }

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


}
