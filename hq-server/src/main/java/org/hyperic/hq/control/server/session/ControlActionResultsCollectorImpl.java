/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.control.server.session;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.control.ControlActionResult;
import org.hyperic.hq.control.GroupControlActionResult;
import org.hyperic.hq.control.shared.ControlActionTimeoutException;
import org.hyperic.hq.control.shared.ControlConstants;
import org.hyperic.hq.control.shared.ControlScheduleManager;
import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link ControlActionResultsCollector}
 * 
 * Periodically polls the ControlHistory DB table waiting for control action
 * results from the agent to be processed. Most of this logic was extracted from
 * the quartz jobs for running control actions.
 * @author jhickey
 * 
 */
@Component
public class ControlActionResultsCollectorImpl implements ControlActionResultsCollector {

    // The time to wait in between checking if a control action has
    // finished.
    private static final int JOB_WAIT_INTERVAL = 500;

    private ControlScheduleManager controlScheduleManager;

    private ConfigManager configManager;

    @Autowired
    public ControlActionResultsCollectorImpl(ControlScheduleManager controlScheduleManager,
                                             ConfigManager configManager) {
        this.controlScheduleManager = controlScheduleManager;
        this.configManager = configManager;
    }

    public ControlActionResult waitForResult(Integer jobId, int timeout)
        throws ControlActionTimeoutException, ApplicationException {
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() < (start + timeout)) {
            try {
                ControlHistory history = controlScheduleManager.getJobHistoryValue(jobId);
                String status = history.getStatus();
                if (status.equals(ControlConstants.STATUS_COMPLETED) ||
                    status.equals(ControlConstants.STATUS_FAILED)) {
                    return new ControlActionResult(new AppdefEntityID(history.getEntityType()
                        .intValue(), history.getEntityId()), status, history.getMessage());
                }
            } catch (ApplicationException e) {
                // This may happen if we start checking status before tx
                // commits. Ignore and re-try
            }

            // wait some more
            try {
                Thread.sleep(JOB_WAIT_INTERVAL);
            } catch (InterruptedException e) {
            }
        }

        throw new ControlActionTimeoutException("Timeout waiting for job id " + jobId);
    }

    public GroupControlActionResult waitForGroupResults(AppdefEntityID groupId, List<Integer> ids,
                                                        int timeout)
        throws ControlActionTimeoutException, ApplicationException {
        long start = System.currentTimeMillis();
        List<Integer> idsToProcess = new ArrayList<Integer>(ids);
        Set<ControlActionResult> results = new HashSet<ControlActionResult>();
        String groupStatus = ControlConstants.STATUS_COMPLETED;
        String groupMessage = null;
        while (System.currentTimeMillis() < (start + timeout)) {
            for (Iterator<Integer> iterator = idsToProcess.iterator(); iterator.hasNext();) {
                Integer jobId = iterator.next();
                try {
                    ControlHistory history = controlScheduleManager.getJobHistoryValue(jobId);
                    String status = history.getStatus();

                    if (status.equals(ControlConstants.STATUS_COMPLETED) ||
                        status.equals(ControlConstants.STATUS_FAILED)) {
                        iterator.remove();
                        results.add(new ControlActionResult(new AppdefEntityID(history
                            .getEntityType().intValue(), history.getEntityId()), status, history
                            .getMessage()));
                    }
                    if (status.equals(ControlConstants.STATUS_FAILED)) {
                        // One failure marks the whole group as failed
                        groupStatus = ControlConstants.STATUS_FAILED;
                        groupMessage = history.getMessage();
                    }
                } catch (ApplicationException e) {
                    // This may happen if we start checking status before tx
                    // commits. Ignore and re-try
                }
            }
            if (idsToProcess.isEmpty()) {
                return new GroupControlActionResult(results, groupId, groupStatus, groupMessage);
            }
            // wait some more
            try {
                Thread.sleep(JOB_WAIT_INTERVAL);
            } catch (InterruptedException e) {
            }
        }

        throw new ControlActionTimeoutException("Timeout waiting for all jobs to complete");
    }

    @Transactional(readOnly=true)
    public int getTimeout(AuthzSubject subject, AppdefEntityID id, int defaultTimeout) {
        int timeout;
        try {
            ConfigResponse config = getConfigResponse(subject, id);
            String strTimeout = config.getValue(ControlPlugin.PROP_TIMEOUT);
            if (strTimeout == null)
                return defaultTimeout;
            timeout = Integer.parseInt(strTimeout);
        } catch (Exception e) {
            return defaultTimeout;
        }

        return timeout * 1000;
    }

    /**
     * Get the control config response
     */

    private ConfigResponse getConfigResponse(AuthzSubject subject, AppdefEntityID id)
        throws PluginException {
        ConfigResponseDB config;
        try {
            config = configManager.getConfigResponse(id);
        } catch (Exception e) {
            throw new PluginException(e);
        }

        if (config == null || config.getControlResponse() == null) {
            throw new PluginException("Control not " + "configured for " + id);
        }

        byte[] controlResponse = config.getControlResponse();
        ConfigResponse configResponse;
        try {
            configResponse = ConfigResponse.decode(controlResponse);
        } catch (Exception e) {
            throw new PluginException("Unable to decode configuration");
        }

        return configResponse;
    }

}
