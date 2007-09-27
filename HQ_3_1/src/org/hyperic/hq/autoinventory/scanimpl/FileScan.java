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

package org.hyperic.hq.autoinventory.scanimpl;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.util.PluginLoader;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EnumerationConfigOption;
import org.hyperic.util.config.DirArrayConfigOption;
import org.hyperic.util.config.IntegerConfigOption;
import org.hyperic.util.config.BooleanConfigOption;

import org.hyperic.util.file.match.Matcher;
import org.hyperic.util.file.match.MatcherConfig;
import org.hyperic.util.file.match.MatchSelector;
import org.hyperic.util.file.match.MatchResults;
import org.hyperic.util.file.match.MatcherInterruptCallback;
import org.hyperic.util.file.match.MatcherProgressCallback;
import org.hyperic.util.file.match.MatcherInterruptedException;

import org.hyperic.sigar.SigarException;

import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.FileServerDetector;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.ServerSignature;

/**
 * This implementation of ScanMethod knows how to scan the filesystem.
 * It can be configured to scan the whole filesystem, or to only
 * scan certain directories while ignoring others.  It can also ignore
 * entire filesystems of a particular type, for example network mounted
 * filesystems or CD-ROM drives (ie iso9660 filesystems).
 */
public class FileScan 
    extends ScanMethodBase
    implements MatcherInterruptCallback, MatcherProgressCallback {

    private List _scanDirs;
    private List _excludeDirs;
    private String _fsTypes;
    private int _depth;
    private boolean _followSymlinks;
    private boolean _isWindows;
    
    private Log _log = LogFactory.getLog(FileScan.class.getName());
    private transient long _statusUpdateCounter = 0;

    public static final String FS_TYPE_ALL     = "All disks";
    public static final String FS_TYPE_LOCAL   = "Local disks";
    public static final String FS_TYPE_NETWORK = "Network-mounted disks";
    public static final String[] FS_TYPES
        = { FS_TYPE_ALL, FS_TYPE_LOCAL, FS_TYPE_NETWORK };
    
    public FileScan () {
        // we'll give the filesystem a reliability factor of 5
        _authorityLevel = 5;
        _isWindows = PlatformDetector.IS_WIN32;
    }
    public FileScan (boolean isWindows) {
        // we'll give the filesystem a reliability factor of 5
        _authorityLevel = 5;
        _isWindows = isWindows;
    }

    public String getName () { return "FileScan"; }
    public String getDisplayName () { return "File Scan"; }
    public String getDescription () { return "Scan the filesystem"; }

    protected ConfigOption[] getOptionsArray () { 

        ConfigOption[] opts = new ConfigOption[5];
        int i=0;
        opts[i++] = new DirArrayConfigOption("scanDirs", 
                                             "Directories to scan"
                                             + " (use | as a delimiter)",
                                             getDefaultScanDirs());
        opts[i++] = new DirArrayConfigOption("excludeDirs",
                                             "Directories to exclude from scan"
                                             + " (use | as a delimiter)",
                                             getDefaultExcludeDirs());
        opts[i++] = new EnumerationConfigOption("fsTypes",
                                                "Filesystem types to scan",
                                                FS_TYPES[0],
                                                FS_TYPES);
        opts[i++] = new IntegerConfigOption("depth",
                                            "How deep (in directory levels) to scan."
                                            + "  Use -1 to indicate unlimited depth",
                                            new Integer(6));
        opts[i++] = new BooleanConfigOption("followSymlinks",
                                            "Should symlinks be followed?",
                                            false);
        return opts;
    }

    public void scan(ConfigResponse platformConfig, ServerDetector[] serverDetectors) 
        throws AutoinventoryException {

        boolean hasFileScan = false;
        for (int i=0; i<serverDetectors.length; i++) {
            if (serverDetectors[i] instanceof FileServerDetector) {
                hasFileScan = true;
                break;
            }
        }
        if (!hasFileScan) {
            _log.debug("Skipping FileScan");
            _state.setScanStatus(this, "scan completed");
            return;
        }

        try {
            _state.setScanStatus(this, "scan started");

            // Get the list of directories to scan
            _scanDirs = StringUtil.explode(_config.getValue("scanDirs"),
                                           DirArrayConfigOption.DELIM_STR);
            
            // Get the list of directories to exclude
            _excludeDirs = StringUtil.explode(_config.getValue("excludeDirs"),
                                              DirArrayConfigOption.DELIM_STR);
            
            _fsTypes = _config.getValue("fsTypes");
            int fsTypeID = convertFSType(_fsTypes);
            _depth = Integer.parseInt(_config.getValue("depth"));
            _followSymlinks
                = Boolean.valueOf(_config.getValue("followSymlinks")).booleanValue();

            if ( _scanner.getIsInterrupted() ) {
                return;
            }

            Matcher m = new Matcher();
            MatcherConfig mconfig = new MatcherConfig();
            Iterator matchedServerTypes;
            int i;
            MatchResults matchResults;
            List stMatches;
            String matchPath;
            ServerDetector serverDetector;
            ServerDetector prevDetector;
            List detectedServers;
            MatchSelector matchSelector;
                    
            // This map helps us keep track of whether or not
            // a previous server detector has detected a server
            // in a given directory.
            Map pathsToDetectors = new HashMap();

            for ( i=0; i<_scanDirs.size(); i++ ) {
                mconfig.addSearchDir((String) _scanDirs.get(i));
            }
            mconfig.setExcludePatterns(_excludeDirs);
            mconfig.setFSTypes(fsTypeID);
            mconfig.setFollowSymlinks(_followSymlinks);
            mconfig.setLog(_log);
            mconfig.setMaxDepth(_depth);
            mconfig.setMatcherInterruptCB(this);
            mconfig.setMatcherProgressCB(this);
            mconfig.setAllowMultipleMatches(true);

            // For each server detector, add a matcherselector
            for ( i=0; i<serverDetectors.length; i++ ) {
                matchSelector = getMatchSelector(serverDetectors[i]);
                mconfig.addMatchSelector(matchSelector);
            }

            try {
                _statusUpdateCounter = 0;
                matchResults = m.getMatches(mconfig);
                _state.addScanExceptions(this, matchResults.getErrorArray());

            } catch (SigarException se) {
                throw new AutoinventoryException("Fatal Exception running "
                                                 + "FileScan: " + se, se);

            } catch (MatcherInterruptedException mie) {
                // Don't do anything, the scan is aborted
                _log.info("Scan interrupted.");
                
                // but if we wanted to, the exception is kind enough to let
                // us know what the matches were before the interruption...
                // allMatches = mie.getMatchesSoFar();

                return;
            }

            matchedServerTypes = matchResults.matches.keySet().iterator();
            while ( matchedServerTypes.hasNext() ) {
                serverDetector
                    = (ServerDetector) matchedServerTypes.next();
                stMatches = (List) matchResults.matches.get(serverDetector);
                for ( i=0; i<stMatches.size(); i++ ) {
                    matchPath = (String) stMatches.get(i);
                    prevDetector
                        = (ServerDetector) pathsToDetectors.get(matchPath);
                    if ( prevDetector != null ) {
                        throw new AutoinventoryException
                            ("Multiple servers ("
                             + prevDetector.getServerSignature().getServerTypeName()
                             + " and " 
                             + serverDetector.getServerSignature().getServerTypeName() 
                             + ") can detect for path: " + matchPath);
                    }
                    PluginLoader.setClassLoader(serverDetector);
                    try {
                        detectedServers = ((FileServerDetector)serverDetector).
                            getServerResources(platformConfig, matchPath);
                    } finally {
                        PluginLoader.resetClassLoader();
                    }

                    if ( detectedServers != null && 
                         detectedServers.size() > 0 ) {
                        // Record this match so if future server detectors
                        // claim a match and detect servers here, it will
                        // be an error condition (see exception thrown above).
                        pathsToDetectors.put(serverDetector, matchPath);

                        // Add servers to state
                        // We had a match, save the path and server value.
                        _log.info("DETECTED SERVERS=" 
                                   + StringUtil.listToString(detectedServers));
                        _state.addServers(this, detectedServers);
                    }
                }
            }
        } catch (PluginException e) {
            _log.error("PluginException in FileScan: " + e, e);
        }
        catch ( Exception e ) {
            _log.error("Big-time error in FileScan: " + e, e);
            throw new AutoinventoryException(e);

        } finally {
            _state.setScanStatus(this, "scan completed");
        }
    }

    /** @see org.hyperic.util.file.match.MatcherInterruptCallback#getIsInterrupted */
    public boolean getIsInterrupted () {
        return Thread.currentThread().isInterrupted();
    }

    /**
     * Convert our concept of which filesystems to scan
     * into one of the FS_XXX constants defined in MatcherConfig
     */
    private int convertFSType ( String aiType ) {
        if ( aiType.equals(FS_TYPE_ALL) ) {
            return MatcherConfig.FS_ALL;
        }
        if ( aiType.equals(FS_TYPE_LOCAL) ) {
            return MatcherConfig.FS_LOCAL;
        }
        if ( aiType.equals(FS_TYPE_NETWORK) ) {
            return MatcherConfig.FS_NETWORK;
        }
        throw new IllegalArgumentException("Unrecognized filesystem argument: "
                                           + aiType);
    } 

    private static final String[] WIN32_DEFAULT_DIRS = { "C:\\" };
    private static final String[] WIN32_DEFAULT_EXCLUDE_DIRS
        = { "\\WINNT", "\\TEMP", "\\TMP", 
            "\\Documents and Settings", "\\Recycled" };

    private static final String[] UNIX_DEFAULT_DIRS  = { "/usr", "/opt" };
    private static final String[] UNIX_DEFAULT_EXCLUDE_DIRS
        = { "/usr/doc", "/usr/dict", "/usr/lib", "/usr/libexec", 
            "/usr/man", "/usr/tmp", "/usr/include", "/usr/share",
            "/usr/src",
            "/usr/local/include", "/usr/local/share", "/usr/local/src" };

    private String getDefaultScanDirs () {

        String dirs = "";
        int i;
        if ( _isWindows ) {
            // XXX We should use sigar here, because IMHO the "right"
            // default value should be "all local disks", but we have no
            // way to know what is local without something like sigar.
            for ( i=0; i<WIN32_DEFAULT_DIRS.length; i++ ) {
                if ( dirs.length() > 0 ) dirs += DirArrayConfigOption.DELIM_STR;
                dirs += WIN32_DEFAULT_DIRS[i];
            }
            
        } else {
            // XXX We could also use sigar here to look for other
            // local filesystems, like /local0 /u1 etc.
            for ( i=0; i<UNIX_DEFAULT_DIRS.length; i++ ) {
                if ( dirs.length() > 0 ) dirs += DirArrayConfigOption.DELIM_STR;
                dirs += UNIX_DEFAULT_DIRS[i];
            }
        }
        return dirs;
    }

    private String getDefaultExcludeDirs () {

        String dirs = "";
        int i;
        if ( _isWindows ) {
            for ( i=0; i<WIN32_DEFAULT_EXCLUDE_DIRS.length; i++ ) {
                if ( dirs.length() > 0 ) dirs += DirArrayConfigOption.DELIM_STR;
                dirs += WIN32_DEFAULT_EXCLUDE_DIRS[i];
            }
            
        } else {
            for ( i=0; i<UNIX_DEFAULT_EXCLUDE_DIRS.length; i++ ) {
                if ( dirs.length() > 0 ) dirs += DirArrayConfigOption.DELIM_STR;
                dirs += UNIX_DEFAULT_EXCLUDE_DIRS[i];
            }
        }
        return dirs;
    }

    private MatchSelector getMatchSelector ( ServerDetector sd ) {
        MatchSelector ms = getMatchSelector(sd.getServerSignature());
        ms.setKey(sd);
        return ms;
    }

    private MatchSelector getMatchSelector ( ServerSignature sig ) {
        MatchSelector ms = new MatchSelector(sig);
        ms.addMatchPatterns  (sig.getFileMatchPatterns());
        ms.addExcludePatterns(sig.getFileExcludePatterns());
        // System.err.println("FileScan.getMatchSelector(" + sig + ") returning: " + ms);
        return ms;
    }

    /**
     * @see org.hyperic.util.file.match.MatcherProgressCallback#notifyScanDir
     */
    public void notifyScanDir (File dir) {
        if ( (_statusUpdateCounter % 10) == 0 ) {
            _state.setScanStatus(this, "scanning: " + dir.getAbsolutePath());
        }
        _statusUpdateCounter++;
    }
}
