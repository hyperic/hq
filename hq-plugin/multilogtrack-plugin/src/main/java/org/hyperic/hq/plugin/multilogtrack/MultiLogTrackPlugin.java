/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2012], VMWare, Inc.
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

package org.hyperic.hq.plugin.multilogtrack;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.LogFileTailPlugin;
import org.hyperic.hq.product.LogMessageFolder;
import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.sigar.FileInfo;
import org.hyperic.sigar.FileTail;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.util.config.BooleanConfigOption;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

public class MultiLogTrackPlugin extends LogFileTailPlugin {
    
    private static final Log log = LogFactory.getLog(MultiLogTrackPlugin.class);
    private Pattern includePattern;
    private Pattern excludePattern;
    private Pattern includePattern_2;
    private Pattern excludePattern_2;
    private AtomicReference<FileTail> watcher = new AtomicReference<FileTail>();
    private Sigar sigar;
    private final Long matchSleepTime;
    protected static final String FILE_SCAN_INTERVAL = "300000";
    private static final HashSet<String> firstTimeConfig = new HashSet<String>();
    /** picks up new files as they appear */
    private static AtomicReference<Collection<MultiDirWatcher>> fileWatcherThreads =
        new AtomicReference<Collection<MultiDirWatcher>>();
    static final String INCLUDE_PATTERN = "includepattern";
    static final String EXCLUDE_PATTERN = "excludepattern";
    static final String SECONDARY_PATTEN_SUFFIX = "_2";
    static final String INCLUDE_PATTERN_2 = "includepattern" + SECONDARY_PATTEN_SUFFIX;
    static final String EXCLUDE_PATTERN_2 = "excludepattern" + SECONDARY_PATTEN_SUFFIX;
    
    public MultiLogTrackPlugin() {
        this.matchSleepTime = getSleepTime();
    }

    public TrackEvent processLine(FileInfo info, String line, String basedir, String logfilepattern, int offset) {
        final TrackEvent event = processLine(
            INCLUDE_PATTERN, includePattern, includePattern, excludePattern, info, line, basedir, logfilepattern, offset);
        final TrackEvent event2 = processLine(
            INCLUDE_PATTERN_2, includePattern, includePattern_2, excludePattern_2, info, line, basedir, logfilepattern, offset);
        // don't want to return a null event if one was evaluated.  Need to make sure we return the correct one
        if (event != null) {
            return event;
        } else if (event2 != null) {
            return event2;
        } else {
            return null;
        }
    }
    
    private TrackEvent processLine(String property, Pattern primaryIncludePattern, Pattern includePattern,
                                   Pattern excludePattern, FileInfo info, String line, String basedir,
                                   String logfilepattern, int offset) {
        if (null == includePattern || !includePattern.matcher(line).find()) {
            return null;
        }
        if (null != excludePattern && excludePattern.matcher(line).find()) {
            return null;
        }
        MultiLogTrackMeasurementPlugin.incrementNumLines(property, basedir, logfilepattern,
                                                         primaryIncludePattern.toString(), offset);
        return newTrackEvent(System.currentTimeMillis(), LogTrackPlugin.LOGLEVEL_ANY, info.getName(), line);
    }

    public void configure(ConfigResponse config) throws PluginException {
        this.config = config;
        setFileWatcherThread(config);
        final Reference<Pattern> includePatternRef = new Reference<Pattern>();
        configure(config, INCLUDE_PATTERN, includePatternRef);
        includePattern = includePatternRef.get();
        final Reference<Pattern> excludePatternRef = new Reference<Pattern>();
        configure(config, EXCLUDE_PATTERN, excludePatternRef);
        excludePattern = excludePatternRef.get();
        final Reference<Pattern> includePatternRef_2 = new Reference<Pattern>();
        configure(config, INCLUDE_PATTERN_2, includePatternRef_2);
        includePattern_2 = includePatternRef_2.get();
        final Reference<Pattern> excludePatternRef_2 = new Reference<Pattern>();
        configure(config, EXCLUDE_PATTERN_2, excludePatternRef_2);
        excludePattern_2 = excludePatternRef_2.get();
    }
    
    public void configure(ConfigResponse config, String configProp, Reference<Pattern> patternRef) throws PluginException {
        String pattern = config.getValue(configProp, "");
        pattern = pattern.trim().startsWith("*") ? "." + pattern.trim() : pattern.trim();
        if (pattern != null && pattern.trim().length() != 0) {
            try {
                patternRef.set(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
            } catch (Exception e) {
                throw new PluginException("error compiling pattern " + configProp + "=" + pattern + ": " + e, e);
            }
        } else {
            patternRef.set(null);
        }
    }

    private void setFileWatcherThread(final ConfigResponse c) {
        final String basedir = MultiLogTrackServerDetector.getBasedir(c);
        final String logfilepattern = MultiLogTrackServerDetector.getLogfilepattern(c);
        final Thread thread = new MultiDirWatcher(c);
        Collection<MultiDirWatcher> threads = fileWatcherThreads.get();
        if (threads == null) {
            threads = new ArrayList<MultiDirWatcher>();
            fileWatcherThreads.set(threads);
        }
        boolean startThread = true;
        for (MultiDirWatcher t : threads) {
            if (t.basedir.equals(basedir) && t.logfilepattern.equals(logfilepattern)) {
                startThread = false;
                break;
            }
        }
        if (startThread) {
            thread.setDaemon(true);
            thread.start();
        }
    }
    
    private class MultiDirWatcher extends Thread {
        private final ConfigResponse cfg;
        private String basedir;
        private String logfilepattern;
        private MultiDirWatcher(ConfigResponse c) {
            this.cfg = c;
            this.basedir = MultiLogTrackServerDetector.getBasedir(c);
        	this.logfilepattern = MultiLogTrackServerDetector.getLogfilepattern(c);
            setName("MultiDirWatcher-" + basedir + "-" + logfilepattern);
        }
        public void run() {
            if (log.isDebugEnabled()) {
                log.debug("starting multi log track watcher thread for basedir=" + basedir +
                          ", logfilepattern=" + logfilepattern);
            }
            while (true) {
                try {
                    setFiles(cfg);
                    long sleepTime = Long.parseLong(FILE_SCAN_INTERVAL);
                    try {
                        sleepTime = Long.parseLong(cfg.getValue("fileScanInterval", FILE_SCAN_INTERVAL));
                        if (sleepTime < 60000) {
                            log.warn("file scan interval set to " + sleepTime +
                                     ", will not scan more than once a minute.  Setting interval to one minute");
                            sleepTime = 60000;
                        }
                    } catch (NumberFormatException e) {
                        log.debug(e,e);
                    }
                    Thread.sleep(sleepTime);
                } catch (Throwable t) {
                    log.error(t,t);
                }
            }
        }
    }

    private void setFiles(ConfigResponse cfg) throws PluginException {
        try {
            final Set<String> oldFiles = new HashSet<String>();
            final String basedir = MultiLogTrackServerDetector.getBasedir(cfg);
            final String includePatternBuf = cfg.getValue(INCLUDE_PATTERN);
            final String logfilepattern = MultiLogTrackServerDetector.getLogfilepattern(cfg);
            final String key = logfilepattern + "|" + basedir + "|" + includePatternBuf;
            if (firstTimeConfig.contains(key)) {
                MultiLogTrackServerDetector.getBasedirAndSetFilesFromCache(cfg, oldFiles, false);
            } else {
                firstTimeConfig.add(key);
            }
            final List<String> files =
                MultiLogTrackServerDetector.getFilesCached(logfilepattern, basedir, includePatternBuf, true);
            for (final String logfile : files) {
                boolean exists = oldFiles.remove(logfile);
                if (!exists) {
                    final String fs = MultiLogTrackServerDetector.getFileSeparator(basedir);
                    configureLogfile(basedir + fs + logfile, cfg);
                }
            }
            final FileTail fileWatcher = getFileWatcher(cfg);
            for (final String nonExistentLogFile : oldFiles) {
                fileWatcher.remove(nonExistentLogFile);
            }
        } catch (SigarException e) {
            throw new PluginException(e.getMessage(), e);
        }
    }

    private void configureLogfile(String logfile, ConfigResponse cfg) throws SigarException {
        FileTail fileWatcher = getFileWatcher(cfg);
        if (!new File(logfile).exists()) {
            log.warn("logfile=" + logfile + " does not exist");
            if (log.isDebugEnabled()) log.debug("logfile=" + logfile + " does not exist", new Throwable());
            return;
        }
        fileWatcher.add(logfile);
        if (log.isDebugEnabled()) {
            log.debug("Adding Multi Log Track file watchers for file=" + logfile);
        }
        try {
            Field field = LogTrackPlugin.class.getDeclaredField("folder");
            field.setAccessible(true);
            final LogMessageFolder folder = new LogMessageFolder(this);
            folder.setRepeatMax(Long.MAX_VALUE);
            field.set(this, folder);
        } catch (Exception e) {
            log.error(e,e);
        }
    }

    private FileTail getFileWatcher(final ConfigResponse cfg) {
        if (this.watcher.get() == null) {
            if (sigar == null) {
                sigar = new Sigar();
            }
            this.watcher.set(new MultiFileTail(sigar, cfg));
            getManager().addFileWatcher(this.watcher.get());
            if (log.isDebugEnabled()) {
                log.debug("init file tail basedir=" + MultiLogTrackServerDetector.getBasedir(cfg) + 
                          ", pattern=" + MultiLogTrackServerDetector.getLogfilepattern(cfg) +
                          ", watcher=" + watcher +
                          ", this=" + this +
                          ", cfg=" + cfg +
                          ", sigar=" + sigar);
            }
        }
        return this.watcher.get();
    }

    private class MultiFileTail extends FileTail {
        private final ConfigResponse cfg;
        private MultiFileTail(Sigar sigar, ConfigResponse cfg) {
            super(sigar);
            this.cfg = cfg;
        }
        public void tail(FileInfo info, Reader reader) {
            FileInfo previous = info.getPreviousInfo();
            if (info.getInode() == 0 || previous.getInode() == 0) {
                long prevSize = previous.getSize();
                if (log.isDebugEnabled()) {
                    final String msg = "Inode=0, will attempt to seek to previous Inode size=" + prevSize;
                    log.debug((info.getInode() != 0 ? "previous " : "") + msg);
                }
                if (info.getSize() == prevSize) {
                    log.debug("log watcher event occured but there are no lines to read since info.getSize() == previous.getSize()");
                    return;
                }
                try {
                    reader.skip(prevSize);
                } catch (IOException e) {
                    log.error("Could not skip to the previous offset on Inode=0: " + e, e);
                    return;
                }
            }
            BufferedReader buffer = new BufferedReader(reader);
            String basedir = MultiLogTrackServerDetector.getBasedir(cfg);
            String logfilepattern = MultiLogTrackServerDetector.getLogfilepattern(cfg);
            try {
                String line;
                final boolean debug = log.isDebugEnabled();
                boolean first = true;
                int i=0;
                while ((line = buffer.readLine()) != null) {
                    if (!first && matchSleepTime != null && matchSleepTime > 0) {
                        sleep(matchSleepTime);
                    }
                    first = false;
                    if (debug) {
                        log.debug("processing file=" + info.getName() + ", line=" + line +
                                  ", basedir=" + MultiLogTrackServerDetector.getBasedir(cfg) + 
                                  ", pattern=" + MultiLogTrackServerDetector.getLogfilepattern(cfg) +
                                  ", includepattern=" + includePattern +
                                  ", this=" + toString());
                    }
                    TrackEvent event = processLine(info, line, basedir, logfilepattern, i++);
                    if (event != null && reportEvents()) {
                        getManager().reportEvent(event);
                    }
                }
            } catch (IOException e) {
                log.error(info.getName() + ": " + e, e);
            }
        }
        private void sleep(Long sleepTime) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                log.debug(e,e);
            }
        }
        private boolean reportEvents() {
            String value = cfg.getValue(MultiLogTrackServerDetector.ENABLE_ONLY_METRICS);
            if (value == null || value.equalsIgnoreCase("false")) {
                return true;
            }
            return false;
        }
        public int hashCode() {
            return cfg.hashCode();
        }
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o instanceof MultiFileTail) {
                MultiFileTail t = (MultiFileTail) o;
                return cfg.equals(t.cfg);
            }
            return false;
        }
    }

    private Long getSleepTime() {
        // TODO should handle this via the AgentConfig, but for now doing it via System.getProperty()
        String sleepTimeBuf = System.getProperty("multilogtrack.sleep");
        if (sleepTimeBuf == null) {
            return null;
        }
        try {
            return Long.parseLong(sleepTimeBuf);
        } catch (NumberFormatException e) {
            log.warn("multilogtrack.sleep not a number: " + sleepTimeBuf + ", plugin will not sleep btwn log matches");
        }
        return null;
    }

    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config) {
        ConfigSchema schema = new ConfigSchema();
        ConfigOption enableOption = getEnableOption(info, config);
        if (enableOption != null) {
            schema.addOption(enableOption);
            enableOption.setDefault("true");
            enableOption.setOptional(false);
        }
        final String enableProp = MultiLogTrackServerDetector.ENABLE_LOG_SERVICES_PROP;
        ConfigOption option = new BooleanConfigOption(enableProp, "Enable Service Creation", false);
        option.setOptional(true);
        schema.addOption(option);

        final String metricsProp = MultiLogTrackServerDetector.ENABLE_ONLY_METRICS;
        option = new BooleanConfigOption(metricsProp, "Only Enable Log Metrics, Do Not Send Logs To The HQ Server", false);
        option.setOptional(true);
        schema.addOption(option);

        final String overrideChecks = MultiLogTrackServerDetector.OVERRIDE_CHECKS;
        option = new BooleanConfigOption(overrideChecks, "Override file checks.  Validate if files do not exist", false);
        option.setOptional(true);
        schema.addOption(option);

        return schema;
    }
    
    public class Reference<T> {
        private T value;
        public Reference() {}
        public Reference(T initialValue) {
            this.value = initialValue;
        }
        public T get() {
            return value;
        }
        public void set(T value) {
            this.value = value;
        }
        public int hashCode() {
            return value.hashCode();
        }
        public boolean equals(Object rhs) {
            if (this == rhs) {
                return true;
            }
            return rhs.equals(value);
        }
        public String toString() {
            return value.toString();
        }
    }

}