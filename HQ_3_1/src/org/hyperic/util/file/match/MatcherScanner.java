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

package org.hyperic.util.file.match;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.Map;

import org.apache.commons.logging.Log;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.selectors.FileSelector;
import org.apache.tools.ant.util.FileUtils;

class MatcherScanner extends DirectoryScanner {

    private int maxDepth;
    private Log log;
    private MatcherInterruptCallback interruptCB = null;
    private MatcherProgressCallback progressCB = null;
    private Map matches;
    private List errors;
    private boolean myFollowSymlinks = false;
    private static final FileUtils myFileUtils = FileUtils.newFileUtils();
    private int pauseCounter = 0;
    
    public MatcherScanner () {
        super();
        errors = new ArrayList();
    }

    public void initMatches ( Map matches ) {
        this.matches = matches;
    }

    public List getErrors () { return errors; }
    public void addError ( Exception e ) { errors.add(e); }

    public void setMaxDepth ( int max ) { maxDepth = max; }
    
    public Log getLog () { return this.log; }
    public void setLog ( Log log ) { this.log = log; }
    
    public void setMatcherInterruptCB (MatcherInterruptCallback icb) {
        interruptCB = icb;
    }
    public void setMatcherProgressCB (MatcherProgressCallback pcb) {
        progressCB = pcb;
    }

    protected void addMatch ( Object key, String fullpath ) {
        // System.err.println("MS--->addMatch(" + key + ", " + fullpath + ")");
        List existingList = (List) matches.get(key);
        if ( existingList == null ) {
            existingList = new ArrayList();
            matches.put(key, existingList);
        }
        existingList.add(fullpath);
    }

    public Map getMatches () { return matches; }
    
    public void setSelectors(FileSelector[] selectors) {
        super.setSelectors(selectors);
        for ( int i=0; i<selectors.length; i++ ) {
            if ( selectors[i] instanceof MatchSelector ) {
                ((MatchSelector) selectors[i]).setMatcherScanner(this);
            }
        }
    }

    public void setFollowSymlinks(boolean followSymlinks) {
        this.myFollowSymlinks = followSymlinks;
        super.setFollowSymlinks(followSymlinks);
    }

    public void doScan() throws MatcherInterruptedException {
        checkInterrupted();
        for ( int i=0; i<selectors.length; i++ ) {
            if ( selectors[i] instanceof MasterMatchSelector ) {
                ((MasterMatchSelector) selectors[i]).setScanner(this);
            }
        }
        super.scan();
    }
    
    private void checkInterrupted () throws MatcherInterruptedException {
        if ( interruptCB != null ) {
            if ( interruptCB.getIsInterrupted() ) {
                throw new MatcherInterruptedException();
            }
        } else {
            // Just check for thread interruption
            if ( Thread.currentThread().isInterrupted() ) {
                throw new MatcherInterruptedException();
            }
        }
    }
    
    /** Override default implementation to track dir depth */
    protected void scandir(File dir, String vpath, boolean fast) {
        pauseCounter = 0;
        scandir(dir, vpath, 0);
    }

    private void pause () {
        if ( ++pauseCounter % 25 == 0 ) {
            try { Thread.sleep(50); } catch (InterruptedException ie) {
                throw new MatcherInterruptedException();
            }
        }
    }

    protected void scandir(File dir, String vpath, int depth) {

        // This is just here to ensure that our scanning doesn't spin-up
        // the CPU.  We sleep briefly every few directories to give other
        // processes a chance to run.
        pause();

        if ( depth > maxDepth && maxDepth != -1 ) {
            if ( log != null ) {
                //log.info("scandir: Skipping dir " + dir.getAbsolutePath() 
                //         + " and all subdirs because they are below our "
                //         + "maxDepth of " + maxDepth);
            }
            return;

        } else {
            checkInterrupted();
            if ( isExcluded(dir.getAbsolutePath()) ) {
                //log.info("scandir: Skipping: " + dir.getAbsolutePath() 
                //         + " because it is explicitly excluded from scan.");
                return;
            }
        }

        if ( !dir.canRead() ) {
            errors.add(new MatcherException("Don't have permissions to read "
                                            + "directory: "
                                            + dir.getAbsolutePath()));
            return;
        }
        String[] newfiles = dir.list();

        if (newfiles == null) {
            /*
             * two reasons are mentioned in the API docs for File.list
             * (1) dir is not a directory. This is impossible as
             *     we wouldn't get here in this case.
             * (2) an IO error occurred (why doesn't it throw an exception
             *     then???)
             */
            errors.add(new MatcherException("IO error scanning directory "
                                            + dir.getAbsolutePath()));
            return;
        }

        // log.debug("scandir: Scanning: " + dir.getAbsolutePath());

        // Tell someone how our scan is progressing...
        if ( progressCB != null ) progressCB.notifyScanDir(dir);

        if (!myFollowSymlinks) {
            Vector noLinks = new Vector();
            for (int i = 0; i < newfiles.length; i++) {
                checkInterrupted();
                try {
                    if (myFileUtils.isSymbolicLink(dir, newfiles[i])) {
                        String name = vpath + newfiles[i];
                        File   file = new File(dir, newfiles[i]);
                        if (file.isDirectory()) {
                            dirsExcluded.addElement(name);
                        } else {
                            filesExcluded.addElement(name);
                        }
                    } else {
                        noLinks.addElement(newfiles[i]);
                    }
                } catch (IOException ioe) {
                    String msg = "IOException caught while checking "
                        + "for links, couldn't get canonical path!";
                    // will be caught and redirected to Ant's logging system
                    errors.add(new MatcherException(msg, ioe));
                }
            }
            newfiles = new String[noLinks.size()];
            noLinks.copyInto(newfiles);
        }

        for (int i = 0; i < newfiles.length; i++) {
            checkInterrupted();
            pause();
            String name = vpath + newfiles[i];
            File   file = new File(dir, newfiles[i]);
            if (file.isDirectory()) {
                if (isIncluded(name)) {
                    if (!isExcluded(name)) {
                        // log.info("scandir: checking isSelected(" + name + "," + file.getAbsolutePath() + ")");
                        if (isSelected(name,file)) {
                            continue;
                        } else {
                            if (/*fast &&*/ couldHoldIncluded(name)) {
                                scandir(file, name + File.separator, depth+1);
                            }
                        }

                    } else {
                        if (/*fast &&*/ couldHoldIncluded(name)) {
                            scandir(file, name + File.separator, depth+1);
                        }
                    }
                } else {
                    if (/*fast &&*/ couldHoldIncluded(name)) {
                        scandir(file, name + File.separator, depth+1);
                    }
                }
                // if (!fast) {
                //     scandir(file, name + File.separator, fast);
                // }
            } else if (file.isFile()) {
                if (isIncluded(name)) {
                    if (!isExcluded(name)) {
                        if (isSelected(name,file)) {
                            continue;
                        } else {
                            // everythingIncluded = false;
                            // filesDeselected.addElement(name);
                        }
                    } else {
                        // everythingIncluded = false;
                        // filesExcluded.addElement(name);
                    }
                } else {
                    // everythingIncluded = false;
                    // filesNotIncluded.addElement(name);
                }
            }
        }
    }
}
