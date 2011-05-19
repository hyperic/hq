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

package org.hyperic.hq.operation.rabbit.admin;

import org.hyperic.hq.operation.rabbit.admin.erlang.ControlAction;
import org.hyperic.hq.operation.rabbit.admin.erlang.ErlangConverter;

/**
 * Encapsulates a Rabbit Erlang control action with the appropriate ErlangConverter to use.
 * @author Helena Edelson
 */
public class RabbitControlAction implements ControlAction {

    private ErlangConverter converter;

    private String module;

    private String function;

    private Class key;

    public RabbitControlAction() {
    }

    protected RabbitControlAction(Class key, String module, String function) {
        this(key, module, function, null);
    }

    protected RabbitControlAction(Class key, String module, String function, ErlangConverter converter) {
        this.module = module;
        this.function = function;
        this.converter = converter;
        this.key = key != null ? key : this.getClass();
    } 

    public Class getKey() {
        return key;
    }

    public String getModule() {
        return module;
    }

    public String getFunction() {
        return function;
    }

    public ErlangConverter getConverter() {
        return converter;
    }
}
