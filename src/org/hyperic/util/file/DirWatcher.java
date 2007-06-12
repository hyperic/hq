package org.hyperic.util.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Watches a directory for changes, issuing callbacks when things change  
 * 
 * XXX:  JBoss has similar functionality that we should re-use
 */
public class DirWatcher 
    implements Runnable
{
    private final Log _log = LogFactory.getLog(DirWatcher.class);

    private final File               _dir;
    private final DirWatcherCallback _cback;
    
    private final List _lastList = new ArrayList();
    
    public DirWatcher(File dir, DirWatcherCallback callback) {
        _dir      = dir;
        _cback    = callback;
    }
    
    public void run() { 
        _log.info("Watching: " + _dir);
        while (true) {
            try {
                List curList = Arrays.asList(_dir.listFiles());

                for (Iterator i=curList.iterator(); i.hasNext(); ) {
                    File f = (File)i.next();
                    
                    if (!_lastList.contains(f)) {
                        _cback.fileAdded(f);
                    }
                }
                
                for (Iterator i=_lastList.iterator(); i.hasNext(); ) {
                    File f = (File)i.next();
                    
                    if (!curList.contains(f)) {
                        _cback.fileRemoved(f);
                    }
                }
                
                _lastList.clear();
                _lastList.addAll(curList);
            } catch(Throwable e) { 
                // Catch everything, including Errors, since things like
                // IncompatableClassChange errors can sometimes occur
                _log.warn("Error while processing directory listing", e);
            }
            try {
                Thread.sleep(1000);
            } catch(InterruptedException e) {
                _log.info("Dying");
                return;
            }
        }
    }

    public interface DirWatcherCallback {
        void fileAdded(File f);
        void fileRemoved(File f);
    }
}
