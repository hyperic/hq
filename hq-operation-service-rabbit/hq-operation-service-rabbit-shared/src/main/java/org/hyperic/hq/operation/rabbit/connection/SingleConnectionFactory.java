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

package org.hyperic.hq.operation.rabbit.connection;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.PossibleAuthenticationFailureException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.ConnectException;

/**
 * @author Helena Edelson
 */
@Component
public final class SingleConnectionFactory extends AbstractRabbitConnectionFactory {

    private final Log logger = LogFactory.getLog(this.getClass());

    public SingleConnectionFactory() {
        super();
    }
    
    public SingleConnectionFactory(String username, String password) {
        super(username, password);
    }

    @Override
    protected Connection createConnection(Address[] addrs) throws IOException {
        return super.doNewConnection(addrs);
    }

    /**
     * Essentially a ping to see if a broker can be reached.
     * @return ConnectionStatus with throwable reason if not
     * and a boolean active - true if we could connect.
     */
    public ConnectionStatus activeOnStartup(Address[] addrs) {
        Throwable reason = null;
        boolean active = false;

        try {
            Connection c = newConnection(addrs);
            active = c != null && c.isOpen();
        } catch (Throwable e) {
            reason = e;
            translate(e, addrs);
        }
        return new ConnectionStatus(reason, active);
    }

    private void translate(Throwable error, Address[] addrs) {
        if (error instanceof PossibleAuthenticationFailureException) {
            logger.error("can not connect with credentials: " + getUsername() + ":" + getPassword());
        } else if (error instanceof ConnectException) {
            logger.error("can not connect with at least one of the following: " + StringUtils.arrayToCommaDelimitedString(addrs));
        } else {
            logger.error(error.getMessage());
        }
    }
}
