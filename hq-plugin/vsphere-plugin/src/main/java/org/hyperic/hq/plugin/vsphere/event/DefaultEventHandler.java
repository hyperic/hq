/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], Hyperic, Inc.
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
package org.hyperic.hq.plugin.vsphere.event;

import com.vmware.vim25.*;
import com.vmware.vim25.mo.EventHistoryCollector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * DefaultEventHandler
 * ToDo - behavior. Right now just discovery of what we have
 *
 * @author Helena Edelson
 */
public class DefaultEventHandler implements EventHandler {

    private static final Log logger = LogFactory.getLog(DefaultEventHandler.class.getName());

    /**
     * @param events
     */
    public void handleEvents(Event[] events) {
        if (events != null && events.length > 0) {
            for (Event e : events) {
                handleEvent(e);
            }
        }
    }

    /**
     * ToDo determine which we want.
     *
     * @param ehc
     * @param pageSize
     */
    public void handleEvents(EventHistoryCollector ehc, int pageSize) {
        if (ehc != null) {
            try {
                logger.debug("Events In the latestPage: ");
                handleEvents(ehc.getLatestPage());

                logger.debug("Events In the nextPage: ");
                handleEvents(ehc.readNextEvents(pageSize));

                logger.debug("Events In the previousPage: ");
                ehc.resetCollector();
                handleEvents(ehc.readPreviousEvents(pageSize));
            }
            catch (Exception e) {
                logger.error("", e);
            }
        }
    }

    /**
     * @param event
     */
    public void handleEvent(Event event) {
        if (event != null) {
            String typeName = event.getClass().getName();
            int lastDot = typeName.lastIndexOf('.');
            if (lastDot != -1) {
                typeName = typeName.substring(lastDot + 1);
            }

            doHandleEvent(event, typeName); 
        }
    }

    /**
     * Just printing event properties for now
     * @param event
     * @param typeName
     */
    private void doHandleEvent(Event event, String typeName) {
        StringBuilder builder = new StringBuilder("Event[").append("type=").append(typeName).append(" eventId=").append(event.getKey())
                .append(" chainId=").append(event.getChainId()).append(" user=" + event.getUserName())
                .append(" time=").append(event.getCreatedTime().getTime()).append(" message=").append(event.getFullFormattedMessage());

        if (event.getVm() != null) {
            builder.append(" vm=").append(event.getVm().getVm().get_value());
        }
        if (event.getDatacenter() != null) {
            builder.append(" datacenter=").append(event.getDatacenter().getDatacenter());
        }
        if (event.getComputeResource() != null) {
            builder.append(" computeResource=").append(event.getComputeResource().getComputeResource());
        }
        if (event.getHost() != null) {
            builder.append(" host=").append(event.getHost().getHost());
        }

        builder.append("]");

        logger.debug(builder.toString());

    }

}
