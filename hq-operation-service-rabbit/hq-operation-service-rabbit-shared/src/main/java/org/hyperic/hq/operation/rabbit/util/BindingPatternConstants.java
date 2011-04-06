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
