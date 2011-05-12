/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2009-2010], VMware, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 */

package org.hyperic.hq.operation.rabbit.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.operation.RegisterAgentResponse;
import org.hyperic.hq.operation.rabbit.util.ServerConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

/**
 * Handles thrown exceptions in an OperationEndpoint
 * to send that error contact back to the caller.
 * @author Helena Edelson
 */
@Component
public class RabbitErrorHandler implements ErrorHandler {

    private final Log logger = LogFactory.getLog(RabbitErrorHandler.class);

    private final RabbitTemplate rabbitTemplate;
 
    @Autowired
    public RabbitErrorHandler(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Temporary, just started. TODO:
     * 1. It has to know or get the appropriate error to exchange mapping (create this)
     * 2. It also has to get the routing key for this.
     * 3. Much of this already exists, just needs  an error context addition.
     * @param t the Throwable cause
     */
    public void handleError(Throwable t) { 
        final String context = t.getCause().toString();

        if (context.contains("BadCredentialsException")) {
            rabbitTemplate.publish(ServerConstants.EXCHANGE_TO_AGENT, "response.register",
                    new RegisterAgentResponse("Permission denied"), null);
        }
    }
}
