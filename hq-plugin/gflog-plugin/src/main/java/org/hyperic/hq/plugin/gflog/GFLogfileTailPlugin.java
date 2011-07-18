package org.hyperic.hq.plugin.gflog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.LogFileTrackPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.sigar.FileInfo;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;

public class GFLogfileTailPlugin extends LogFileTrackPlugin {

    private static Log log =
        LogFactory.getLog(GFLogfileTailPlugin.class);
    
    public final static String OPT_FORCE_ERROR = "force.error";
    public final static String OPT_FORCE_WARN = "force.warn";
    public final static String OPT_FORCE_INFO = "force.info";

    private static final String[] LOG_LEVELS = {
        "error,severe",     //Error
        "warning",          //Warning
        "info,config",             //Info
        "fine,finer,finest"       //Debug
    };
    
    private Pattern forceErrorPattern = null;
    private Pattern forceWarnPattern = null;
    private Pattern forceInfoPattern = null;

    private static Sigar sigar = null;

    private MultilineFileTail watcher = null;

    public String[] getLogLevelAliases() {
        return LOG_LEVELS;
    }    
    
    protected ConfigOption getFilesOption(TypeInfo info, ConfigResponse config) {
        return null;
    }

    static void cleanup() {
        if (sigar != null) {
            sigar.close();
        }
    }

    private MultilineFileTail getFileWatcher() {
        if (this.watcher == null) {
            log.debug("init file tail");

            if (sigar == null) {
                sigar = new Sigar();
            }

            this.watcher = new MultilineFileTail(sigar) {
                public void message(FileInfo info, String message, String level) {
                    TrackEvent event =  newTrackEvent(System.currentTimeMillis(),
                            forceLevel(message, level),
                            info.getName(),
                            message);
                    if (event != null) {
                        getManager().reportEvent(event);
                    }
                };
            };
            getManager().addFileWatcher(this.watcher);
        }
        return this.watcher;
    }

    public void configure(ConfigResponse config)
    throws PluginException {

        super.configure(config);
        
        String eRegex = config.getValue(OPT_FORCE_ERROR, null);
        this.forceErrorPattern = (StringUtils.isBlank(eRegex) ? null : Pattern.compile(eRegex));
        String wRegex = config.getValue(OPT_FORCE_WARN, null);
        this.forceWarnPattern = (StringUtils.isBlank(wRegex) ? null : Pattern.compile(wRegex));
        String iRegex = config.getValue(OPT_FORCE_INFO, null);
        this.forceInfoPattern = (StringUtils.isBlank(iRegex) ? null : Pattern.compile(iRegex));
        
        String[] files = new String[]{config.getValue("path")};

        try {
            getFileWatcher().add(files);
        } catch (SigarException e) {
            throw new PluginException(e.getMessage(), e);
        }
    }

    public void shutdown()
    throws PluginException {

        if (this.watcher != null) {
            getManager().removeFileWatcher(this.watcher);
            this.watcher = null;
        }

        super.shutdown();
    }
    
    /**
     * This function checks if we need to force a change of
     * log level. Sometimes we only want to track error level
     * but actual error message is tagged with info for example.
     * 
     * @param message Message to check against force regex
     * @param level Original log level
     * @return New log level if matched against the rules. Returns
     *         original if match failed. 
     */
    private String forceLevel(String message, String level) {
        // we match in this order: error,warn,info

        if(forceErrorPattern != null) {
            Matcher m = forceErrorPattern.matcher(message);
            if(m.find())
                return "error";
        }

        if(forceWarnPattern != null) {
            Matcher m = forceWarnPattern.matcher(message);
            if(m.find())
                return "warning";
        }

        if(forceInfoPattern != null) {
            Matcher m = forceInfoPattern.matcher(message);
            if(m.find())
                return "info";
        }

        return level;
    }

}
