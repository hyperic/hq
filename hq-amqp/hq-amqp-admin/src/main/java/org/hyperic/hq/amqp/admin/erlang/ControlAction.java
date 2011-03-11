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

package org.hyperic.hq.amqp.admin.erlang;

import org.hyperic.hq.amqp.admin.erlang.ErlangConverter;

/**
 * Encapsulates an Erlang control action with the appropriate ErlangConverter to use.
 * @author Helena Edelson
 */
public interface ControlAction {

    /**
     * Returns the key.
     * @return key as class type to allow the caller to know the implementation class
     * versus the changing function names in a Broker's internal API per version.
     */
    Class getKey();

    /**
     * Returns the Rabbit control module name.
     * @return the module name
     */
    String getModule();

    /**
     * Returns the Rabbit control function name.
     * @return the function name
     */
    String getFunction();

    /**
     * Returns the Spring Erlang converter to use
     * for the particular module:function pair.
     * @return org.hyperic.hq.amqp.admin.RpcConverter
     */
    ErlangConverter getConverter();
    
}
