/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
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
 *
 */

package org.hyperic.hq.events.server.session;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.ActionExecutionInfo;
import org.hyperic.hq.events.ActionInterface;
import org.hyperic.hq.events.AlertInterface;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;
/**
 * Mock implementation of {@link ActionInterface}.  We can't use EasyMock for this because Actions.execute() will
 * later try to instantiate via reflection
 * @author jhickey
 *
 */
public class MockAction implements ActionInterface {

    /**
     *
     */
    public MockAction() {

    }
    public String execute(AlertInterface alert, ActionExecutionInfo info) throws ActionExecuteException {
        return "return!";
    }

    public void setParentActionConfig(AppdefEntityID aeid, ConfigResponse config) throws InvalidActionDataException
    {

    }

    public ConfigResponse getConfigResponse() throws InvalidOptionException, InvalidOptionValueException {
       return new ConfigResponse();
    }

    public ConfigSchema getConfigSchema() {
        return null;
    }

    public String getImplementor() {
        return getClass().getName();
    }

    public void init(ConfigResponse config) throws InvalidActionDataException {

    }

    public void setImplementor(String implementor) {

    }

}