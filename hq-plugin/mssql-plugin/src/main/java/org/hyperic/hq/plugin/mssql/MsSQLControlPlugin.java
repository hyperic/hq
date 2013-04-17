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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private static final String DEFAULT_SQLSERVER_SERVICE_NAME = "MSSQLSERVER";
    private static final String DEFAULT_SQLAGENT_SERVICE_NAME = "SQLSERVERAGENT";
    private static final Log log = LogFactory.getLog(MsSQLControlPlugin.class);

    @Override
    public void doAction(String action) throws PluginException {
        String sqlAgentServiceName = DEFAULT_SQLAGENT_SERVICE_NAME;
        String sqlServerServiceName = getServiceName();
        if (!sqlServerServiceName.equals(DEFAULT_SQLSERVER_SERVICE_NAME)) {
            sqlAgentServiceName = sqlServerServiceName.replaceFirst("MSSQL", "SQLAgent");
        }
        Service sqlAgent = null;
        try {
            sqlAgent = new Service(sqlAgentServiceName);
        }catch(Win32Exception e) {
            log.debug("Could not find SqlAgent service "+sqlAgentServiceName +": " + e, e);

        }
       
        try {
            if (action.equals("start")) {
                //starting the SQL agent service will also start the server
                if (null != sqlAgent) {
                    log.debug("About to start SqlAgent service "+sqlAgentServiceName);
                    sqlAgent.start();
                }
                else {
                    log.debug("About to start SqlServer service "+sqlServerServiceName);
                    svc.start();
                }
                setResult(RESULT_SUCCESS);
                return;
            }
            if (action.equals("stop")) {
                if (null != sqlAgent && isServiceRunning(sqlAgent)) {  
                    log.debug("About to stop SqlAgent service "+sqlAgentServiceName);
                    sqlAgent.stop((long)getTimeoutMillis());
                }
                if (isRunning()){
                    log.debug("About to stop SqlServer service "+sqlServerServiceName);
                    svc.stop();
                }
                setResult(RESULT_SUCCESS);
                return;
            }
            if (action.equals("restart")) {
                if (null != sqlAgent && isServiceRunning(sqlAgent)) {              
                    sqlAgent.stop((long)getTimeoutMillis());
                }
                if (isRunning()){
                    svc.stop((long)getTimeoutMillis());
                }
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

    private boolean isServiceRunning(Service service) {
        int status = service.getStatus();
        if (status == Service.SERVICE_START_PENDING || 
            status == Service.SERVICE_RUNNING || 
            status == Service.SERVICE_STOP_PENDING) {
           return true;
        }
        return false;

    }
}
