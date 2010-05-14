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

package org.hyperic.hq.transport;

import org.hyperic.hq.appdef.Agent;
import org.jboss.remoting.transporter.TransporterClient;
import org.springframework.stereotype.Component;

/**
 * The factory class for creating proxies to agent services. Note that proxy
 * invocations are not thread-safe and must be synchronized externally if
 * multiple threads are making invocations on the same proxy.
 * 
 * The unidirectional transport is not supported for a .ORG instance.
 */
@Component
public class AgentProxyFactoryImpl implements AgentProxyFactory {

    /**
     * @see org.hyperic.hq.transport.AgentProxyFactory#createSyncService(Agent,
     *      java.lang.Class)
     */
    public Object createSyncService(Agent agent, Class serviceInterface) throws Exception {

        if (agent.isUnidirectional()) {
            throw new UnsupportedOperationException(".ORG instance does not support the unidirectional transport.");
        } else {
            // TODO need to implement bidirectional
            throw new UnsupportedOperationException("bidirectional not supported yet");
        }
    }

    /**
     * @see org.hyperic.hq.transport.AgentProxyFactory#createAsyncService(Agent,
     *      java.lang.Class, boolean)
     */
    public Object createAsyncService(Agent agent, Class serviceInterface, boolean guaranteed) throws Exception {
        if (agent.isUnidirectional()) {
            throw new UnsupportedOperationException(".ORG instance does not support the unidirectional transport.");
        } else {
            // TODO need to implement bidirectional
            throw new UnsupportedOperationException("bidirectional not supported yet");
        }
    }

    /**
     * @see org.hyperic.hq.transport.AgentProxyFactory#destroyService(java.lang.Object)
     */
    public void destroyService(Object proxy) {
        if (proxy != null) {
            TransporterClient.destroyTransporterClient(proxy);
        }
    }

}
