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

package org.hyperic.hq.product;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.util.StringUtil;
import org.hyperic.util.config.BooleanConfigOption;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EnumerationConfigOption;
import org.hyperic.util.config.StringConfigOption;

public class LogTrackPlugin extends GenericPlugin {

    private static HashMap logLevelCache = new HashMap();

    private static Log log =
        LogFactory.getLog(LogTrackPlugin.class.getName());

    static final boolean debugLogging =
        "true".equals(System.getProperty("log_track.debug"));
    
    /* Log levels */
    public static final int LOGLEVEL_ERROR   = 3;
    public static final int LOGLEVEL_WARN    = 4;
    public static final int LOGLEVEL_INFO    = 6;
    public static final int LOGLEVEL_DEBUG   = 7;

    public static final String LOGLEVEL_ERROR_LABEL  = "Error";
    public static final String LOGLEVEL_WARN_LABEL   = "Warn";
    public static final String LOGLEVEL_INFO_LABEL   = "Info";
    public static final String LOGLEVEL_DEBUG_LABEL  = "Debug";

    private static final int LOG_LEVELS[] = {
        LOGLEVEL_ERROR,
        LOGLEVEL_WARN,
        LOGLEVEL_INFO,
        LOGLEVEL_DEBUG
    };

    private static final Integer[] LOG_LEVELS_INTEGER;
    
    static final String PROP_LEVEL = 
        ProductPlugin.TYPE_LOG_TRACK + ".level";

    static final String PROP_ENABLE =
        ProductPlugin.TYPE_LOG_TRACK + ".enable";

    /**
     * @deprecated
     * @see PROP_INCLUDE
     */
    private static final String PROP_PATTERN =
        ProductPlugin.TYPE_LOG_TRACK + ".pattern";
    
    static final String PROP_INCLUDE =
        ProductPlugin.TYPE_LOG_TRACK + ".include";

    static final String PROP_EXCLUDE =
        ProductPlugin.TYPE_LOG_TRACK + ".exclude";
 
    private static final String[] ENABLE_PROPS =
        createTypeLabels(PROP_ENABLE);

    /**
     * @deprecated
     * @see INCLUDE_PROPS
     */
    private static final String[] PATTERN_PROPS =
        createTypeLabels(PROP_PATTERN);
    
    private static final String[] INCLUDE_PROPS =
        createTypeLabels(PROP_INCLUDE);

    private static final String[] EXCLUDE_PROPS =
        createTypeLabels(PROP_EXCLUDE);

    private static final String[] LEVEL_PROPS =
        createTypeLabels(PROP_LEVEL);

    static {
        Integer[] levels = new Integer[LOG_LEVELS.length];
        for (int i=0; i<levels.length; i++) {
            levels[i] = new Integer(LOG_LEVELS[i]);
        }
        LOG_LEVELS_INTEGER = levels;
    }

    private LogTrackPluginManager manager;

    private int logLevel;
    private StringMatcher matcher = null;
    private Map logLevelMap = null;
    private LogMessageFolder folder;

    public static int[] getLogLevels() {
        return LOG_LEVELS;
    }

    static String[] LOGLEVEL_LABELS = {
        LOGLEVEL_ERROR_LABEL,
        LOGLEVEL_WARN_LABEL,
        LOGLEVEL_INFO_LABEL,
        LOGLEVEL_DEBUG_LABEL
    };

    public static String getLogLevelLabel(int level) {
        switch (level) {
        case LOGLEVEL_ERROR:
            return LOGLEVEL_ERROR_LABEL;
        case LOGLEVEL_WARN:
            return LOGLEVEL_WARN_LABEL;
        case LOGLEVEL_INFO:
            return LOGLEVEL_INFO_LABEL;
        case LOGLEVEL_DEBUG:
            return LOGLEVEL_DEBUG_LABEL;
        default:
            throw new IllegalArgumentException("Invalid log level: "+level);
        }
    }

    protected int getLogLevel(String label) {
        for (int x=0;x<LOGLEVEL_LABELS.length;x++) {
            if (LOGLEVEL_LABELS[x].equals(label)) {
                return LOG_LEVELS[x];
            }
        }
        throw new IllegalArgumentException("Invalid log level label: " + label);
    }

    public String[] getLogLevelAliases() {
        return LOGLEVEL_LABELS;
    }

    protected Map getLogLevelMap() {
        return this.logLevelMap;
    }

    private static Map createLogLevelMap(String[] mapping) {
        Map map = new HashMap();
        int len = LOGLEVEL_LABELS.length;

        if (mapping.length != len) {
            String msg = "mapping.length != " + len; 
            throw new IllegalArgumentException(msg);
        }

        for (int i=0; i<len; i++) {
            Integer level = LOG_LEVELS_INTEGER[i];
            StringTokenizer tok = new StringTokenizer(mapping[i], ",");
            while (tok.hasMoreTokens()) {
                map.put(tok.nextToken(), level);
            }
        }

        return map;
    }
    
    public static boolean isEnabled(ConfigResponse config,
                                    int type) {
        String option = config.getValue(ENABLE_PROPS[type]);
        if (option == null) {
            return false;
        }
        return option.equals("true");
    }

    public static void setEnabled(ConfigResponse config,
                                  int type, int level) {
        if (level == -1) {
            return;
        }
        config.setValue(ENABLE_PROPS[type], "true");
        config.setValue(LEVEL_PROPS[type],
                        getLogLevelLabel(level));
    }
    
    protected boolean supportsLogLevels() {
        return true;
    }

    protected boolean shouldDebugLog() {
        return true;
    }
    
    protected boolean shouldLog(int level) {
        if (supportsLogLevels()) {
            return level <= getLogLevel();
        }
        else {
            if (debugLogging) {
                debugLog("Log levels not supported");
            }
            return true;
        }
    }

    protected int getLogLevel() {
        return this.logLevel;
    }
    
    protected void setLogLevel(int level) {
        this.logLevel = level;
    }

    protected boolean messageMatches(TrackEvent event) {
        String message = event.getMessage();

        if (this.matcher == null) {
            if (debugLogging) {
                debugLog(message, "No pattern match configured");
            }
            return !this.folder.shouldFold(event, ".*");
        }

        message = stripNewLines(message);

        boolean matches =
            this.matcher.matches(message);

        if (debugLogging) {
            String debugMsg;
            if (matches) {
                debugMsg = "Matches";
            }
            else {
                debugMsg = "Does not match";
            }
            debugLog(message, debugMsg +
                     " '" + this.matcher + "'");
        }

        if (matches) {
            List lastMatches = this.matcher.getLastMatches();
            return !this.folder.shouldFold(event, lastMatches);
        }
        else {
            return false;
        }
    }

    protected TrackEvent newTrackEvent(long time,
                                       String level,
                                       String source,
                                       String message)
    {
        Integer intLevel = (Integer)getLogLevelMap().get(level);
        if (intLevel == null) {
            if (debugLogging) {
                debugLog(message,
                         "no level mapped to '" + level + "'");
            }
            return null;
        }
        return newTrackEvent(time, intLevel.intValue(),
                             source, message);
    }

    void debugLog(String debugMsg) {
        debugLog(null, debugMsg);
    }

    void debugLog(String message, String debugMsg) {
        if (!debugLogging) {
            return;
        }
        if (!shouldDebugLog()) {
            return;
        }
        if (message != null) {
            message = " for message='" + message + "',";            
        }
        else {
            message = "";
        }
        log.debug(debugMsg + message +
                  " plugin=" + getName() +
                  " [" + getTypeInfo().getName() + "]");                
    }

    protected String stripNewLines(String message) {
        message = StringUtil.replace(message, "\r", "");
        message = StringUtil.replace(message, "\n", "");
        return message;
    }
    
    protected TrackEvent newTrackEvent(long time,
                                       int level,
                                       String source,
                                       String message)
    {
        if (!shouldLog(level)) {
            if (debugLogging) {
                debugLog(message,
                        "Ignoring, level " +
                        getLogLevelLabel(level) +
                        " > " +
                        getLogLevelLabel(getLogLevel())); 

            }
            return null;
        }
        else if (debugLogging) {
            debugLog(message,
                    "Accepting, level " +
                    getLogLevelLabel(level) +
                    " <= " +
                    getLogLevelLabel(getLogLevel()));
        }

        TrackEvent event =
            new TrackEvent(getName(),
                           time,
                           level,
                           source,
                           message);
        
        if (!messageMatches(event)) {
            return null;
        }

        if (debugLogging) {
            debugLog(message, "Reporting Event");
        }

        return event;
    }

    public void reportEvent(long time,
                            int level,
                            String source,
                            String message)
    {
        TrackEvent event =
            newTrackEvent(time, level, source, message);
        if (event != null) {
            getManager().reportEvent(event);
        }
    }
    
    public void configure(ConfigResponse config) throws PluginException {
        super.configure(config);

        if (supportsLogLevels()) {
            int type = getTypeInfo().getType();
            String level = config.getValue(LEVEL_PROPS[type]);
            if (level == null) {
                setLogLevel(LOGLEVEL_ERROR);
            }
            else {
                setLogLevel(getLogLevel(level));
            }
            if (debugLogging) {
                debugLog("Configured log level=" +
                         getLogLevelLabel(getLogLevel()));
            }

            String[] logLevels = getLogLevelAliases();
            this.logLevelMap = (Map)logLevelCache.get(logLevels);
            if (this.logLevelMap == null) {
                log.debug("Creating log level map for: " +
                          getTypeInfo().getName());
                this.logLevelMap = createLogLevelMap(logLevels);
                logLevelCache.put(logLevels, this.logLevelMap);
            }
        }

        int type = getTypeInfo().getType();
        String includes = config.getValue(INCLUDE_PROPS[type]);
        if ("".equals(includes)) {
            includes = null;
        }
        if (includes == null) {
            //XXX for back-compat
            includes = config.getValue(PATTERN_PROPS[type]);
            if ("".equals(includes)) {
                includes = null;
            }
        }

        String excludes = config.getValue(EXCLUDE_PROPS[type]);
        if ("".equals(excludes)) {
            excludes = null;
        }

        if ((includes != null) || (excludes != null)) {
            this.matcher = new StringMatcher();

            try {
                this.matcher.setIncludes(includes);
            } catch (Exception e) {
                String msg =
                    "Invalid " + INCLUDE_PROPS[type] + ": " + e;
                throw new PluginException(msg);
            }

            try {
                this.matcher.setExcludes(excludes);
            } catch (Exception e) {
                String msg =
                    "Invalid " + EXCLUDE_PROPS[type] + ": " + e;
                throw new PluginException(msg);
            }
        }

        if (debugLogging) {
            if (this.matcher == null) {
                debugLog("No pattern match configured");
            }
            else {
                debugLog("Pattern match configured to '" +
                         this.matcher + "'");
            }
        }

        this.folder = new LogMessageFolder(this);

        if (this.matcher == null) {
            if (this.folder.getRepeatMax() ==
                LogMessageFolder.DEFAULT_REPEAT_MAX)
            {
                this.folder.setRepeatMax(100);
            }
        }
    }

    protected boolean supportsPatternMatching() {
        return true;
    }

    protected ConfigOption getIncludeOption(TypeInfo info, ConfigResponse config) {
        if (supportsPatternMatching()) {
            String defaultValue =
                config.getValue(PATTERN_PROPS[info.getType()]); //XXX back-compat

            if (defaultValue == null) {
                defaultValue = getTypeProperty("DEFAULT_LOG_INCLUDE");
            }
            if (defaultValue == null) {
                defaultValue = "";
            }
            ConfigOption option =
                new StringConfigOption(INCLUDE_PROPS[info.getType()],
                                       "Log Pattern Match", defaultValue);
            option.setOptional(true);
            return option;
        }
        else {
            return null;
        }
    }

    protected ConfigOption getExcludeOption(TypeInfo info, ConfigResponse config) {
        if (supportsPatternMatching()) {
            String defaultValue = getTypeProperty("DEFAULT_LOG_EXCLUDE");
            if (defaultValue == null) {
                defaultValue = "";
            }
            ConfigOption option =
                new StringConfigOption(EXCLUDE_PROPS[info.getType()],
                                       "Log Pattern Exclude", defaultValue);
            option.setOptional(true);
            return option;
        }
        else {
            return null;
        }
    }

    protected ConfigOption getEnableOption(TypeInfo info, ConfigResponse config) {
        String defaultValue = getTypeProperty("DEFAULT_LOG_TRACK_ENABLE");
        boolean enable = "true".equals(defaultValue);

        ConfigOption option =
            new BooleanConfigOption(ENABLE_PROPS[info.getType()],
                                    "Enable Log Tracking",
                                    enable);
        option.setOptional(true);
        return option;
    }

    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config) {
        int type = info.getType();
        ConfigSchema schema = new ConfigSchema();

        ConfigOption enableOption = getEnableOption(info, config);
        if (enableOption != null) {
            schema.addOption(enableOption);
        }

        if (supportsLogLevels()) {
            String defaultLevel =
                getTypeProperty("DEFAULT_LOG_LEVEL");
            if (defaultLevel == null) {
                defaultLevel = LOGLEVEL_ERROR_LABEL;
            }
            EnumerationConfigOption option =
                new EnumerationConfigOption(LEVEL_PROPS[type],
                                            "Track event log level",
                                            defaultLevel,
                                            LOGLEVEL_LABELS);
            option.setOptional(true);
            schema.addOption(option);
        }

        ConfigOption includeOption = getIncludeOption(info, config);
        if (includeOption != null) {
            schema.addOption(includeOption);
        }

        ConfigOption excludesOption = getExcludeOption(info, config);
        if (excludesOption != null) {
            schema.addOption(excludesOption);
        }

        return schema;
    }

    public void init(PluginManager manager)
        throws PluginException {
        
        super.init(manager);
        
        this.manager = (LogTrackPluginManager)manager;
    }

    public LogTrackPluginManager getManager() {
        return this.manager;
    }
}
