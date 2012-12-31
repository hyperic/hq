/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.plugin.iis;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.bizapp.shared.lather.ControlSendCommandResult_args;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.Win32ControlPlugin;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.Win32Exception;

public class IisControlPlugin extends Win32ControlPlugin {

    private Log log = LogFactory.getLog(IisControlPlugin.class);

    @Override
    public void doAction(String action, final ControlSendCommandResult_args resultsMetadata) throws PluginException {
        log.debug("[doAction] action: " + action);
        try {
            if (action.equals("restart")) {
                log.debug("[doAction] svc.status: " + svc.getStatusString());
                if (svc.getStatus() == Service.SERVICE_RUNNING) {
                    svc.stop((long) getTimeoutMillis());
                    log.debug("[doAction] svc.status: " + svc.getStatusString());
                }

                if (svc.getStatus() == Service.SERVICE_STOPPED) {
                    svc.start((long) getTimeoutMillis());
                    setResult(RESULT_SUCCESS);
                } else {
                    throw new Win32Exception("Service is not stopped. (svc.status: " + svc.getStatusString() + ")");
                }

                log.debug("[doAction] svc.status: " + svc.getStatusString());
            } else {
                super.doAction(action);
            }
        } catch (Win32Exception e) {
            setResult(RESULT_FAILURE);
            throw new PluginException(action + " " + getServiceName() + " failed: " + e.getMessage());
        }
    }
}
