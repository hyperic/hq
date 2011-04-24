/* Copyright 2009 SpringSource Inc. All Rights Reserved. */

package org.hyperic.hq.bizapp.server.mdb;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefConverter;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.control.ControlEvent;
import org.hyperic.hq.control.server.session.ServerRestartHandler;
import org.hyperic.hq.control.shared.ControlConstants;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Bound to topic/eventsTopic
 */
public class ServerRestartedListener implements MessageListener {

    private ServerRestartHandler serverRestartHandler;

    private ResourceManager resourceManager;
    
    private AppdefConverter appdefConverter;

    private final Log log = LogFactory.getLog(ServerRestartedListener.class);

    @Autowired
    public ServerRestartedListener(ServerRestartHandler serverRestartHandler,
                                   ResourceManager resourceManager, AppdefConverter appdefConverter) {
        this.serverRestartHandler = serverRestartHandler;
        this.resourceManager = resourceManager;
        this.appdefConverter = appdefConverter;
    }

    public void onMessage(Message message) {
        if (message instanceof ObjectMessage) {
            try {
                final Object messageObject = ((ObjectMessage) message).getObject();
                if (messageObject instanceof ControlEvent) {
                    ControlEvent event = (ControlEvent) messageObject;
                    if ((event.getAction().equals("restart") || event.getAction().equals("start")) &&
                        event.getStatus().equals(ControlConstants.STATUS_COMPLETED)) {
                        final AppdefEntityID resource = appdefConverter
                            .newAppdefEntityId(resourceManager.findResourceById(event.getResource()));
                        if (AppdefEntityConstants.APPDEF_TYPE_SERVER == resource.getType()) {
                            try {
                                serverRestartHandler.serverRestarted(resource);
                            } catch (Exception e) {
                                log.error("Error processing possible server restart event", e);
                            }
                        }
                    }
                }
            } catch (JMSException e) {
                log.error("Error processing possible server restart event", e);
            }
        }
    }

}
