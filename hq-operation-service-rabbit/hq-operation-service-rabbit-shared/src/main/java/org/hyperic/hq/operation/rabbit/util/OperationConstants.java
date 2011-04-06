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

package org.hyperic.hq.operation.rabbit.util;

/**
 * Temporary
 * @author Helena Edelson
 */
public class OperationConstants {

    public static final String OPERATION_NAME_METRICS_REPORT = "hq-agent.add.template";

    public static final String OPERATION_NAME_DELETE_REVISION = "tc-server.revision.delete";

    public static final String OPERATION_NAME_AGENT_START = "start";

    public static final String OPERATION_NAME_AGENT_RESTART = "restart";

    public static final String OPERATION_NAME_AGENT_DIE = "die";

    public static final String OPERATION_NAME_AGENT_UPGRADE = "upgrade";

    public static final String OPERATION_NAME_GET_AGENT_BUNDLE = "getCurrentAgentBundle";

    public static final String OPERATION_NAME_SEND_FILE = "agentSendFileData";

    public static final String OPERATION_NAME_REMOVE_FILE = "agentRemoveFile";

    /**
     * TODO implement annotations to register operations automatically on startup
     * @Operation(OPERATION_NAME_UPDATE_SOMETHING)
     * Object someOperation(final Object request) {
     */
    public static final String[] SERVER_OPERATIONS = {
            "metrics.report.response",
            "metrics.availability.response",
            "metrics.schedule.request",
            "metrics.unschedule.request",
            "metrics.config.request",
            "scans.runtime.response",
            "scans.default.response",
            "scans.autodiscovery.start.request",
            "scans.autodiscovery.stop.request",
            "scans.autodiscovery.config.request",
            "ping.response",
            "user.authentication.response",
            "config.authentication.response",
            "config.registration.response",
            "config.upgrade.request",
            "config.bundle.response",
            "config.restart.request",
            "config.update.response",
            "events.track.log.response",
            "events.track.config.response",
            "controlActions.results.response",
            "controlActions.config.request",
            "controlActions.execute.request",
            "plugin.metadata.response",
            "plugin.liveData.response",
            "plugin.control.add.request",
            "plugin.track.add.request",
            "plugin.track.remove.request"
    };

    /**
     * TODO implement annotations to register operations automatically on startup
     */
    public static final String[] AGENT_OPERATIONS = {
            "metrics.report.request",
            "metrics.availability.request",
            "metrics.schedule.response",
            "metrics.unschedule.response",
            "metrics.config.response",
            "scans.runtime.request",
            "scans.default.request",
            "scans.autodiscovery.start.response",
            "scans.autodiscovery.stop.response",
            "scans.autodiscovery.config.response",
            "ping.request",
            "user.authentication.request",
            "config.authentication.request",
            "config.registration.request",
            "config.upgrade.response",
            "config.bundle.request",
            "config.restart.response",
            "config.update.request",
            "events.track.log.request",
            "events.track.config.request",
            "controlActions.results.request",
            "controlActions.config.response",
            "controlActions.execute.response",
            "plugin.metadata.request",
            "plugin.liveData.request",
            "plugin.control.add.response",
            "plugin.track.add.response",
            "plugin.track.remove.response"
    };

}
