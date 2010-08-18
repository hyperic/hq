/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
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
 * 
 */
public class DirWatcher 
    implements Runnable
{
    private final Log _log = LogFactory.getLog(DirWatcher.class);

    private final File               _dir;
    private final DirWatcherCallback _cback;
    
    private final List _lastList;

    public DirWatcher(File dir, DirWatcherCallback callback, List lastList) {
        _dir      = dir;
        _cback    = callback;
        _lastList = new ArrayList();
        _lastList.addAll(lastList);
    }

    public DirWatcher(File dir, DirWatcherCallback callback) {
        this(dir, callback, new ArrayList());
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
