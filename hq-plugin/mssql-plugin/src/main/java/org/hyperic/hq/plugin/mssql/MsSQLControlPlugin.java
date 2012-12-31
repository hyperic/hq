/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2012], Hyperic, Inc.
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

package org.hyperic.hq.plugin.mssql;

import org.hyperic.hq.bizapp.shared.lather.ControlSendCommandResult_args;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.Win32ControlPlugin;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.Win32Exception;

/**
 * Some versions of MS-SQL have 2 services running,
 * the main server service and another agent service.
 * In order to stop the main server service we need to first stop
 * the agent service and that is the purpose of this class.
 *
 */
public class MsSQLControlPlugin extends Win32ControlPlugin{

    private static final String SQL_AGENT_SERVICE_NAME = "SQLSERVERAGENT";

    @Override
    public void doAction(String action, final ControlSendCommandResult_args resultsMetadata) throws PluginException {
        
        Service sqlAgent = null;
        try {
            sqlAgent = new Service(SQL_AGENT_SERVICE_NAME);
        }catch(Win32Exception e) {
        }
       
        try {
            if (action.equals("start")) {
                //starting the SQL agent service will also start the server
                if (null != sqlAgent) {
                    sqlAgent.start();
                }
                else {
                    svc.start();
                }
                setResult(RESULT_SUCCESS);
                return;
            }
            if (action.equals("stop")) {
                if (null != sqlAgent) {
                    sqlAgent.stop((long)getTimeoutMillis());
                }
                svc.stop();
                setResult(RESULT_SUCCESS);
                return;
            }
            if (action.equals("restart")) {
                if (null != sqlAgent) {
                    sqlAgent.stop((long)getTimeoutMillis());
                }
                svc.stop((long)getTimeoutMillis());
                //starting the SQL agent service will also start the server
                if (null != sqlAgent) {
                    sqlAgent.start();
                }
                else {
                    svc.start();
                }
                setResult(RESULT_SUCCESS);
                return;
            }
        } catch (Win32Exception e) {
            setResult(RESULT_FAILURE);
            throw new PluginException(action + " " + getServiceName() +
                    " failed: " + e.getMessage());
        }
        throw new PluginException("Action '" + action +
                "' not supported");
    }

}
