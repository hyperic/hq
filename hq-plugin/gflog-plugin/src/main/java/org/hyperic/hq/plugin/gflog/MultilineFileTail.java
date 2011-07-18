package org.hyperic.hq.plugin.gflog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.FileInfo;
import org.hyperic.sigar.FileTail;
import org.hyperic.sigar.Sigar;

public abstract class MultilineFileTail extends FileTail {

    
    private static Log log =
        LogFactory.getLog(MultilineFileTail.class);

    private Hashtable<FileInfo, BufInfo> bufs = new Hashtable<FileInfo, BufInfo>();
    
    private static final String[] LOG_LEVELS = {
        "error",
        "severe",
        "warning",
        "config",
        "info",
        "fine"
    };
       
    public MultilineFileTail(Sigar sigar) {
        super(sigar);
    }

    @Override
    public void tail(FileInfo info, Reader reader) {

        BufInfo bufInfo = bufs.get(info);
        
        String line;
        BufferedReader buffer =
            new BufferedReader(reader);

        try {
            while ((line = buffer.readLine()) != null) {
                if(isSectionStart(line)) {
                    log.debug("section start");
                    if(bufInfo == null) {
                        log.debug("creating new bufinfo");
                        bufInfo = new BufInfo();
                        bufInfo.level = parseLogLevel(line);
                        bufInfo.buf.append(line);
                        bufInfo.buf.append('\n');                    
                        bufInfo.last = System.currentTimeMillis();
                        bufs.put(info, bufInfo);
                    } else {
                        log.debug("sending previous buffer");
                        message(info, bufInfo.buf.toString(), bufInfo.level);       
                        bufs.remove(info);
                        
                        bufInfo = new BufInfo();
                        bufInfo.level = parseLogLevel(line);
                        bufInfo.buf.append(line);
                        bufInfo.buf.append('\n');                    
                        bufInfo.last = System.currentTimeMillis();
                        bufs.put(info, bufInfo);
                    }
                } else {
                    log.debug("continue previous section");
                    if(bufInfo != null) {
                        bufInfo.buf.append(line);
                        bufInfo.buf.append('\n');    
                        bufInfo.last = System.currentTimeMillis();                        
                    } else {
                        log.debug("line: " + line + " doesn't below to previous section... skipping");                        
                    }
                }
            }
        } catch (IOException e) {
            log.error(info.getName() + ": " + e.getMessage());
        }
    }
    
    public abstract void message(FileInfo info, String message, String level);

    private boolean isSectionStart(String line) {
        String[] fields = line.split(" ");
        
        if(fields.length < 2)
            return false;
        
        String parsedLevel = parseLogLevel(fields[0]);
        if(parsedLevel == null)
            return false;
           
        return true;
    }
    
    private String parseLogLevel(String levelString) {
        String[] fields = levelString.split(" ");
        
        if(fields.length == 0 || fields[0].length() < 2)
            return null;
        
        String level = fields[0].substring(1);

        if(log.isDebugEnabled())
            log.debug("parseLogLevel:"+levelString+"/"+level);
        
        if(ArrayUtils.contains(LOG_LEVELS, level)) {
            return level;
        } else {
            return null;
        }        
    }
    
    @Override
    public void check() {
        
        // before we read new log lines, check if existing
        // buffers are old enough to make a decision that
        // no more lines are coming to that log message section.
        // we flush those old buffers as events.
        
        Set<Entry<FileInfo, BufInfo>> entries = bufs.entrySet();
        Iterator<Entry<FileInfo, BufInfo>> iter = entries.iterator();
        while (iter.hasNext()) {
            Entry<FileInfo, BufInfo> entry = (Entry<FileInfo, BufInfo>) iter.next();
            if(entry.getValue().last < System.currentTimeMillis()-5000) {
                message(entry.getKey(), entry.getValue().buf.toString(), entry.getValue().level);
                iter.remove();
            }
        }
        
        super.check();
    }

    /**
     * Helper class to store buffer for log message, last write time
     * and log level.
     */
    private class BufInfo {
        StringBuilder buf;
        long last;
        String level;
        BufInfo() {
            buf = new StringBuilder();
            last = 0;
            level = "info";
        }
    }

}
