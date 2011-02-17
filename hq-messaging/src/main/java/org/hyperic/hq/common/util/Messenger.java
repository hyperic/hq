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

package org.hyperic.hq.common.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class Messenger implements MessagePublisher {
    private static Log _log = LogFactory.getLog(Messenger.class);

    private JmsTemplate eventsJmsTemplate;
    private ConcurrentStatsCollector concurrentStatsCollector;
    
    @Autowired
    public Messenger(JmsTemplate eventsJmsTemplate, ConcurrentStatsCollector concurrentStatsCollector) {
        this.eventsJmsTemplate = eventsJmsTemplate;
        this.concurrentStatsCollector = concurrentStatsCollector;
    }

    /**
     * Send a List to a Topic.
     */
    public void publishMessage(String name, List<?> msgList, int maxSize) {
        final boolean debug = _log.isDebugEnabled();
        for (int i = 0; i < msgList.size(); i += maxSize) {
            int end = Math.min(i + maxSize, msgList.size());
            ArrayList<?> msgObj = new ArrayList<Object>(msgList.subList(i, end));
            publishMessage(name, msgObj);
            if (debug) {
                _log.debug("Sent " + (end - i) + " batched events to " + name);
            }
        }
    }

    /**
     * Send message to a Topic.
     * TODO make use of future method timing aspect to tie to ConcurrentStatsCollector and remove from code
     */
    public void publishMessage(String name, Serializable sObj) {
        final long start = System.currentTimeMillis();
        eventsJmsTemplate.convertAndSend(name, sObj);
        final long end= System.currentTimeMillis();
        concurrentStatsCollector.addStat((end- start), ConcurrentStatsCollector.JMS_TOPIC_PUBLISH_TIME);
    }
}
