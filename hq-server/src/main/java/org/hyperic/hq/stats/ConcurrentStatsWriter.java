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
package org.hyperic.hq.stats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.util.stats.StatCollector;
import org.hyperic.util.stats.StatUnreachableException;
import org.hyperic.util.stats.StatsObject;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
public class ConcurrentStatsWriter implements ApplicationListener<ContextRefreshedEvent>, ApplicationContextAware {
	private static final Log log = LogFactory.getLog(ConcurrentStatsWriter.class.getName());
	private static final String BASE_FILENAME = "hqstats";
	
	private final TaskScheduler taskScheduler;
	private ApplicationContext applicationContext;
	private ConcurrentStatsCollector concurrentStatsCollector;
	private String currFilename;
    private FileWriter file;
    private String baseDir;
    
    /** Write period is in seconds **/
    public static final int WRITE_PERIOD = 15;
    
    @Autowired
    public ConcurrentStatsWriter(ConcurrentStatsCollector concurrentStatsCollector, TaskScheduler taskScheduler) {
    	this.concurrentStatsCollector = concurrentStatsCollector;
    	this.taskScheduler = taskScheduler;
    }
    
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if(event.getApplicationContext() != this.applicationContext) {
            return;
        }
        setupBasedir();
        setFileInfo();
        printHeader();
        taskScheduler.scheduleWithFixedDelay(
                new StatsWriter(), new Date(System.currentTimeMillis() + (WRITE_PERIOD * 1000)), WRITE_PERIOD*1000);
        concurrentStatsCollector.setStarted(true);
        log.info("ConcurrentStatsCollector has started");
    }
	
    private void setFileInfo() {
        currFilename = getFilename(false);
        cleanupFilename(currFilename);
        File aFile = new File(currFilename);
        try {
            if (aFile.exists()) {
                String mvFilename = getFilename(true);
                aFile.renameTo(new File(mvFilename));
                gzipFile(mvFilename);
            }
            if (file != null) {
                file.close();
            }
            file = new FileWriter(currFilename, true);
        } catch (IOException e) {
            log.error(e, e);
        }
    }

    private void setupBasedir() {
    	final char fs = File.separatorChar;
    	try {
            // Start 2 levels up from where the webapp is deployed (engine home)
            String restartStorageDir =
                Bootstrap.getResource("/").getFile().getParentFile().getParentFile().getAbsolutePath();
            String logDir = "logs";
            File logDirectory = new File(restartStorageDir + fs + logDir);
            if (!(logDirectory.exists())) {
                logDirectory.mkdir();
            }
            String logSuffix = logDir + fs + "hqstats" + fs;
            baseDir = restartStorageDir + fs + logSuffix;
            log.info("using hqstats baseDir " + baseDir);
            final File dir = new File(baseDir);
            if (!dir.exists()) {
                dir.mkdir();
            }
        } catch (IOException e) {
            log.error("Error setting up stats directory for logging.  Stats will not be logged.  Cause: " +
                      e.getMessage(), e);
        }
    }

    private final void cleanupFilename(String filename) {
        final File file = new File(filename);
        final File path = file.getParentFile();
        final String name = file.getName();
        final FilenameFilter filter = new FilenameFilter() {
            final String _filename = name;

            public boolean accept(File dir, String name) {
                if (dir.equals(path) && name.startsWith(_filename)) {
                    return true;
                }
                return false;
            }
        };
        final File[] files = path.listFiles(filter);
        final long oneWeekAgo = System.currentTimeMillis() - (7 * MeasurementConstants.DAY);
        
        for (int i = 0; i < files.length; i++) {
            if (files[i].lastModified() < oneWeekAgo) {
                files[i].delete();
            }
        }
    }

    private final void printHeader() {
        final StringBuilder buf = new StringBuilder("timestamp,");
        final String countAppend = "_COUNT";
        
        for (Map.Entry<String, StatCollector> entry : concurrentStatsCollector.getStatKeys().entrySet()) {
            final String key = (String) entry.getKey();
            final StatCollector value = (StatCollector) entry.getValue();
            
            buf.append(key).append(',');
            
            // Only print the COUNT column if the StatCollector object doesn't
            // exist.
            // This means that the stat counts come in asynchronously rather
            // than begin calculated every interval.
            if (value == null) {
                buf.append(key).append(countAppend).append(',');
            }
        }
        
        try {
            file.append(buf.append("\n").toString());
            file.flush();
        } catch (IOException e) {
            log.warn(e.getMessage(), e);
        }
    }

    private final String getFilename(boolean withTimestamp) {
        final Calendar cal = Calendar.getInstance();
        final int month = 1 + cal.get(Calendar.MONTH);
        final String monthStr = (month < 10) ? "0" + month : String.valueOf(month);
        final int day = cal.get(Calendar.DAY_OF_MONTH);
        final String dayStr = (day < 10) ? "0" + day : String.valueOf(day);
        String rtn = BASE_FILENAME + "-" + monthStr + "-" + dayStr;
        
        if (withTimestamp) {
            final int hour = cal.get(Calendar.HOUR_OF_DAY);
            final String hourStr = (hour < 10) ? "0" + hour : String.valueOf(hour);
            final int min = cal.get(Calendar.MINUTE);
            final String minStr = (min < 10) ? "0" + min : String.valueOf(min);
            final int sec = cal.get(Calendar.SECOND);
            final String secStr = (sec < 10) ? "0" + sec : String.valueOf(sec);
        
            rtn = rtn + "-" + hourStr + "." + minStr + "." + secStr;
        }
        
        return baseDir + rtn + ".csv";
    }

    private void gzipFile(final String filename) {
        new Thread() {
            public void run() {
                FileOutputStream gfile = null;
                GZIPOutputStream gstream = null;
                PrintStream pstream = null;
                boolean succeed = false;
                
                try {
                    gfile = new FileOutputStream(filename + ".gz");
                    gstream = new GZIPOutputStream(gfile);
                    pstream = new PrintStream(gstream);
                
                    BufferedReader reader = new BufferedReader(new FileReader(filename));
                    String tmp;
                    
                    while (null != (tmp = reader.readLine())) {
                        pstream.append(tmp).append("\n");
                    }
                    
                    gstream.finish();
                    succeed = true;
                } catch (IOException e) {
                    log.warn(e.getMessage(), e);
                } finally {
                    close(gfile);
                    close(gstream);
                    close(pstream);

                    if (succeed) {
                        new File(filename).delete();
                    } else {
                        new File(filename + ".gz").delete();
                    }
                }
            }

            private void close(OutputStream s) {
                if (s != null) {
                    try {
                        s.close();
                    } catch (IOException e) {
                        log.warn(e.getMessage(), e);
                    }
                }
            }
        }.start();
    }

    private class StatsWriter implements Runnable {
        public synchronized void run() {
            try {
                Map<String, List<Number>> stats = getStatsByKey();
                StringBuilder buf = getCSVBuf(stats);
                final FileWriter fw = getFileWriter();
                fw.append(buf.append("\n").toString());
                fw.flush();
            } catch (Throwable e) {
                log.warn(e.getMessage(), e);
            }
        }

        private FileWriter getFileWriter() throws IOException {
            final String filename = getFilename(false);
            
            if (!currFilename.equals(filename)) {
                file.close();
                gzipFile(currFilename);
                
                file = new FileWriter(filename, true);
                
                printHeader();
                
                currFilename = filename;
                cleanupFilename(currFilename);
            }
            
            return file;
        }

        private final StringBuilder getCSVBuf(Map<String, List<Number>> stats) {
            final StringBuilder rtn = new StringBuilder();
            
            rtn.append(System.currentTimeMillis()).append(',');
            
            for (Map.Entry<String, StatCollector> entry : concurrentStatsCollector.getStatKeys().entrySet()) {
                String key = (String) entry.getKey();
                StatCollector stat = (StatCollector) entry.getValue();
            
                if (stat != null) {
                    try {
                        long value = stat.getVal();
                        rtn.append(value).append(',');
                    } catch (StatUnreachableException e) {
                        if (log.isDebugEnabled()) {
                            log.debug(e.getMessage(), e);
                        }
                        rtn.append(',');
                        continue;
                    }
                } else {
                    List<Number> list = stats.get(key);
                    long total = 0l;
                    
                    if (list != null) {
                        for (Number val : list) {
                            total += val.longValue();
                        }
                    
                        rtn.append(total).append(',').append(list.size()).append(",");
                    } else {
                        rtn.append(',').append(',');
                    }
                }
            }
            
            return rtn;
        }

        private Map<String, List<Number>> getStatsByKey() {
            final Map<String, List<Number>> rtn = new HashMap<String, List<Number>>();
            final StatsObject marker = concurrentStatsCollector.generateMarker();
            List<Number> tmp;
            Object obj;
            
            while (marker != (obj = concurrentStatsCollector.pollQueue())) {
                final StatsObject stat = (StatsObject) obj;
                final String id = stat.getId();
                final long val = stat.getVal();
                
                if (null == (tmp = rtn.get(id))) {
                    tmp = new ArrayList<Number>();
                    rtn.put(id, tmp);
                }
                
                tmp.add(new Long(val));
            }
            
            return rtn;
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
       this.applicationContext = applicationContext;
    }
    
    
}