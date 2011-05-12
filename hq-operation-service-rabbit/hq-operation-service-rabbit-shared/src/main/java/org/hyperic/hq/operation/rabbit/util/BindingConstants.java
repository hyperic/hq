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
 * @author Helena Edelson
 */
public class BindingConstants {

    /**
     * Use to bind a queue to the appropriate exchange
     */
    public static final String BINDING_NAME_TO_AGENT = "hq.agent.#";

    /**
     * Use to bind a queue to the appropriate exchange
     */
    public static final String BINDING_NAME_TO_SERVER = "hq.server.#";

    /**
     * Use to bind a queue to the appropriate exchange
     */
    public static final String BINDING_AGENT_OPERATIONS = "hq.agent.#";

    /**
     * Use to bind a queue to the appropriate exchange
     */
    public static final String BINDING_SERVER_OPERATIONS = "hq.server.#";

    /**
     *  deprecated. TODO remove
     */
    public static final String OPERATION_PREFIX = ".operations.";

    /**
     * Metrics report - high load
     */
    public static final String BINDING_METRICS_REPORT = "*.*.metrics.reports.main";

    /**
     * Metrics availability report - high load
     */
    public static final String BINDING_METRICS_AVAILABILITY_REPORT = "*.*.metrics.reports.availability";

    /**
     * General Metrics (schedule,unschedule,config, excludes reports.*)
     */
    public static final String BINDING_METRICS_ACTIONS = "*.*.metrics.actions.*";

    /**
     * Runtime Scans - high load
     */
    public static final String BINDING_SCANS_RUNTIME = "*.*.scans.runtime";

    /**
     * Default Scans - high load
     */
    public static final String BINDING_SCANS_DEFAULT = "*.*.scans.default";

    /**
     * Scan auto-discovery
     */
    public static final String BINDING_SCANS_AUTO_DISCOVERY= "*.*.scans.autodiscovery.*";
 
    /** Auth */
    public static final String BINDING_AUTHENTICATION = "*.*.authentication.#";

    public static final String BINDING_REGISTER_AGENT = "hq.agent.config.register.request";


    /**
     * Config
     */
    public static final String BINDING_CONFIG_REGISTRATION = "*.*.config.*";

    /**
     * Events
     */
    public static final String BINDING_EVENTS_ALL = "*.*.events.#";

    /**
     * Control Actions
     */
    public static final String BINDING_CONTROL_ACTIONS_ALL = "*.*.controlActions.*";

    /**
     * Plugins
     */
    public static final String BINDING_PLUGINS_ALL = "*.*.plugins.#";
}
