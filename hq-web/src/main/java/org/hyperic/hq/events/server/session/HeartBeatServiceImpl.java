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

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.util.MessagePublisher;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.HeartBeatEvent;
import org.hyperic.hq.events.shared.HeartBeatService;
import org.hyperic.hq.zevents.ZeventManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The service that dispatches heart beats.
 */
@Service
//@Transactional
public class HeartBeatServiceImpl implements HeartBeatService {
    private final Log log = LogFactory.getLog(HeartBeatServiceImpl.class);
    private String topicName = EventConstants.EVENTS_TOPIC;
    private MessagePublisher messagePublisher;
    

    @Autowired
    public HeartBeatServiceImpl(MessagePublisher messagePublisher) {
        this.messagePublisher = messagePublisher;
    }

    /**
     * Dispatch a heart beat.
     * 
     * @param beatTime The heart beat time.
     */
    //@Transactional
    public void dispatchHeartBeat(Date beatTime) {
        log.debug("Heart Beat Service started dispatching a heart beat: " + beatTime + ", timestamp=" +
                  beatTime.getTime());

        HeartBeatEvent event = new HeartBeatEvent(beatTime);

        try {
         
            // Send the heart beat event
            messagePublisher.publishMessage(topicName, event);
        } catch (Exception e) {
            // Do not send out heart beat if services are not up
        }

        try {
            ZeventManager.getInstance().enqueueEvent(event.toZevent());
        } catch (InterruptedException e) {
            // Do not send out heart beat if thread is interrupted
        }

        log.debug("Heart Beat Service finished dispatching a heart beat: " + beatTime);
    }
}
