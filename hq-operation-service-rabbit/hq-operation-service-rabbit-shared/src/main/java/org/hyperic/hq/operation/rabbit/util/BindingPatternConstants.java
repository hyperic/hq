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
public class BindingPatternConstants {

    /**
     * Use to bind a queue to the appropriate exchange
     */
    public static final String BINDING_NAME_TO_AGENT = "hq-agents.#";

    /**
     * Use to bind a queue to the appropriate exchange
     */
    public static final String BINDING_NAME_TO_SERVER = "hq-servers.#";

    /**
     * Use to bind a queue to the appropriate exchange
     */
    public static final String BINDING_PATTERN_TO_AGENT_OPERATIONS = "hq-agents.*.operations.#";

    /**
     * Use to bind a queue to the appropriate exchange
     */
    public static final String BINDING_PATTERN__TO_SERVER_OPERATIONS = "hq-servers.*.operations.#";

    /**
     *
     */
    public static final String OPERATION_PREFIX = ".operations.";

    /**
     * Metrics report - high load
     */
    public static final String BINDING_PATTERN_METRICS_REPORT = "*.*.operations.metrics.reports.main";

    /**
     * Metrics availability report - high load
     */
    public static final String BINDING_PATTERN_METRICS_AVAILABILITY_REPORT = "*.*.operations.metrics.reports.availability";

    /**
     * General Metrics (schedule,unschedule,config, excludes reports.*)
     */
    public static final String BINDING_PATTERN_METRICS_ACTIONS = "*.*.operations.metrics.actions.*"; 

    /**
     * Runtime Scans - high load
     */
    public static final String BINDING_PATTERN_SCANS_RUNTIME = "*.*.operations.scans.runtime";

    /**
     * Default Scans - high load
     */
    public static final String BINDING_PATTERN_SCANS_DEFAULT = "*.*.operations.scans.default";

    /**
     * Scan auto-discovery
     */
    public static final String BINDING_PATTERN_SCANS_AUTO_DISCOVERY= "*.*.operations.scans.autodiscovery.*";
 
    /** Auth */
    public static final String BINDING_PATTERN_AUTHENTICATION = "*.*.operations.authentication.#";

    /**
     * Config
     */
    public static final String BINDING_PATTERN_CONFIG_REGISTRATION = "*.*.operations.config.*";

    /**
     * Events
     */
    public static final String BINDING_PATTERN_EVENTS_ALL = "*.*.operations.events.#";

    /**
     * Control Actions
     */
    public static final String BINDING_PATTERN_CONTROL_ACTIONS_ALL = "*.*.operations.controlActions.*";

    /**
     * Plugins
     */
    public static final String BINDING_PATTERN_PLUGINS_ALL = "*.*.operations.plugins.#";
}
