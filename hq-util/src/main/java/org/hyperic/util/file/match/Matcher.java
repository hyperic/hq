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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;

import org.apache.tools.ant.types.selectors.FileSelector;

import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

/**
 * Implements a powerful file/directory matcher on top of Ant's fileset
 * classes.
 */
public class Matcher {

    /** 
     * Construct an Matcher.
     */
    public Matcher () {}

    /**
     * Sets up the appropriate MatcherScanners to use for the search.
     * @param config The config to use to initialize
     * @return A List of MatcherScanners to use for the search.
     */
    private List initScanners (MatcherConfig config, 
                               FileSystem[] filesystems,
                               MatchResults results) {

        List dirs = config.getSearchDirs();
        String dir;
        int i, j;
        MatcherScanner scanner;
        List scanners = new ArrayList();

        List selectorList = config.getMatchSelectors();
        MatchSelector[] selectors;
        MasterMatchSelector masterSelector;
        FileSelector[] masterSelectorArray;

        List excludes = config.getExcludePatterns();
        Log log = config.getLog();
        String[] excludesArray;
        selectors = new MatchSelector[selectorList.size()];
        for ( i=0; i<selectors.length; i++ ) {
            selectors[i] = (MatchSelector) selectorList.get(i);
        }
        masterSelectorArray = new FileSelector[1];
        masterSelectorArray[0]
            = new MasterMatchSelector(selectors,
                                      config.getAllowMultipleMatches());
        
        excludesArray = new String[excludes.size()];
        for ( i=0; i<excludesArray.length; i++ ) {
            excludesArray[i] = excludes.get(i).toString();
        }

        for ( i=0; i<dirs.size(); i++ ) { 
            dir = (String) dirs.get(i);
            if ( shouldSkip(dir, log, results) ) continue;
            if ( config.getFSTypes() != MatcherConfig.FS_ALL ) {
                if ( !isFSTypeMatch(dir, 
                                    config.getFSTypes(),
                                    filesystems,
                                    log) ) continue;
            }

            scanner = new MatcherScanner();
            scanner.setLog(log);
            scanner.setBasedir(new File(dir));
            scanner.setSelectors(masterSelectorArray);
            scanner.setExcludes(excludesArray);
            scanner.setFollowSymlinks(config.getFollowSymlinks());
            scanner.setMatcherInterruptCB(config.getMatcherInterruptCB());
            scanner.setMatcherProgressCB(config.getMatcherProgressCB());
            scanner.setMaxDepth(config.getMaxDepth());
            scanners.add(scanner);
        }

        return scanners;
    }

    /**
     * Determine if the dir is on a filesystem that we should
     * be searching.
     * @param dir The name of the directory.
     * @param fstypes The filesystem types we're supposed to search.
     * @param filesystems The filesystems that sigar detected.
     * @param log The log to use for errors and warnings.
     * @return true if the directory should be included in the search, 
     * false otherwise.
     */
    private boolean isFSTypeMatch ( String dir, 
                                    int fstypes, 
                                    FileSystem[] filesystems,
                                    Log log ) {
        String longestMatch = "";
        int longestMatchIndex = -1;
        String absPath = (new File(dir)).getAbsolutePath();
        String fsMountPoint;

        if ( filesystems == null ) {
            if ( log != null ) {
                log.error("matcher: filesystems array was never initialized.");
            }
            throw new IllegalArgumentException("filesystems array was never "
                                               + "initialized.");
        }

        for ( int i=0; i<filesystems.length; i++ ) {
            fsMountPoint = filesystems[i].getDirName();
            if ( absPath.startsWith(fsMountPoint) &&
                 fsMountPoint.length() > longestMatch.length() ) {
                longestMatch = fsMountPoint;
                longestMatchIndex = i;
            }
        }
        if ( longestMatchIndex == -1 ) {
            if ( log != null ) {
                log.warn("Directory " + dir + " did not match "
                         + "any filesystems, it will not be searched.");
            }
            return false;
        }

        return isCandidateFS(fstypes, 
                             filesystems[longestMatchIndex], 
                             log);
    }

    /**
     * Determine if the given filesystem is OK to search, based on
     * the value of fstype which tells us what filesystem types we
     * are supposed to search.
     */
    public boolean isCandidateFS ( int fstype, FileSystem fs, Log log ) {

        switch ( fs.getType() ) {
        case FileSystem.TYPE_UNKNOWN:
            if ( log != null ) {
                log.warn("Encountered UNKNOWN filesystem (device="
                         + fs.getDevName() + "): " + fs.getDirName());
            }
            return false;

        case FileSystem.TYPE_NONE:
            if ( log != null ) {
                log.warn("Encountered NONE filesystem (device="
                         + fs.getDevName() + "): " + fs.getDirName());
            }
            return false;

        case FileSystem.TYPE_LOCAL_DISK:
            return (fstype == MatcherConfig.FS_ALL ||
                    fstype == MatcherConfig.FS_LOCAL);

        case FileSystem.TYPE_NETWORK:
            return (fstype == MatcherConfig.FS_ALL ||
                    fstype == MatcherConfig.FS_NETWORK);

        case FileSystem.TYPE_RAM_DISK:
        case FileSystem.TYPE_CDROM:
        case FileSystem.TYPE_SWAP:
            return false;

        default:
            if ( log != null ) {
                log.warn("Encountered filesystem with invalid type ("
                         + fs.getType() + ") (device=" 
                         + fs.getDevName() + "): " 
                         + fs.getDirName());
            }
            return false;
        }
    }

    private FileSystem[] loadFilesystems (MatcherConfig config)
        throws SigarException {

        Log log = config.getLog();
        FileSystem[] filesystems;

        if ( config.getFSTypes() != MatcherConfig.FS_ALL ) {
            Sigar sigar = new Sigar();
            try {
                filesystems = sigar.getFileSystemList();
                return filesystems;

            } catch ( SigarException se ) {
                if ( log != null ) {
                    log.error("matcher: error getting "
                              + "available filesystems:" + se, se);
                }
                throw se;
                
            } finally {
                sigar.close();
            }
        }
        return null;
    }

    /**
     * Get the matches for this search.
     * @return A Map representing the matches.  The keys in the Map are
     * the keys that each MatchSelector in the MatcherConfig was
     * initialized with in its constructor.  The values are Lists, where
     * each element in the List is a String representing the full path
     * of the matched path.
     * @exception MatcherInterruptedException If the search was interrupted
     * before it could be completed.  In this case, you can get the matches
     * so far by calling getMatchesSoFar on the MatcherInterruptedException
     * object.
     * @exception SigarException If an error occurs reading the available
     * filesystems - this can only happen if the config's getFSTypes returns
     * a value other than MatcherConfig.FS_ALL.
     */
    public synchronized MatchResults getMatches ( MatcherConfig config ) 
        throws MatcherInterruptedException, SigarException {

        int i, j;
        List scanners;
        MatcherScanner scanner;
        MatchResults results = new MatchResults();
        File f;
        Log log = config.getLog();
        FileSystem[] filesystems = null;

        filesystems = loadFilesystems(config);        
        scanners = initScanners(config, filesystems, results);

        for ( i=0; i<scanners.size(); i++ ) {

            scanner = (MatcherScanner) scanners.get(i);
            scanner.initMatches(results.matches);

            try {
                scanner.doScan();

            } catch ( MatcherInterruptedException mie ) {
                mie.setMatchesSoFar(scanner.getMatches());
                if ( log != null ) {
                    log.warn("matcher: search interrupted.");
                }
                throw mie;
                
            } catch ( Exception e ) {
                // huh?
                scanner.addError(new MatcherException("matcher: search error", e));
                if ( log != null ) {
                    log.error("matcher: search error: " + e, e);
                }
            }
            
            results.matches = scanner.getMatches();
            if (log != null) {
                log.debug("results.matches=" + results.matches);
            }
            results.errors.addAll(scanner.getErrors());
        }

        return results;
    }

    /**
     * Should we skip a directory completely?
     * @return true if dir is null, if dir is not a directory,
     * if we can't read dir, or if dir does not exist.
     */
    private boolean shouldSkip ( String dir, Log log, MatchResults results ) {
        File dirFile;
        String msg;

        // Should never happen
        if ( dir == null ) {
            msg = "matcher: ignoring null dir.";
            if ( log != null ) {
                log.info(msg);
            }
            results.errors.add(new MatcherException(msg));
            return true;
        }

        dirFile = new File(dir);
        if ( !dirFile.exists() ) {
            msg = "matcher: ignoring non-existent dir: " + 
                dirFile.getAbsolutePath();
            if ( log != null ) {
                log.info(msg);
            }
            results.errors.add(new MatcherException(msg));
            return true;
        }
        if ( !dirFile.canRead() ) {
            msg = "matcher: ignoring unreadable dir: " + 
                dirFile.getAbsolutePath();
            if ( log != null ) {
                log.info(msg);
            }
            results.errors.add(new MatcherException(msg));
            return true;
        }
        if ( !dirFile.isDirectory() ) {
            msg = "matcher: ignoring non-dir: " + 
                dirFile.getAbsolutePath();
            if ( log != null ) {
                log.info(msg);
            }
            results.errors.add(new MatcherException(msg));
            return true;
        }
        return false;
    }
}
