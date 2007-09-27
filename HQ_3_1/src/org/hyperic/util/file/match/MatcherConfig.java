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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;

public class MatcherConfig {

    public static final int DEPTH_INFINITE = -1;

    public static final int FS_ALL     = 0;
    public static final int FS_LOCAL   = 1;
    public static final int FS_NETWORK = 2;

    private List _searchDirs;
    private List _matchSelectors;
    private List _excludePatterns;
    private boolean _followSymlinks = false;
    private boolean _allowMultipleMatches = false;
    private int _filesystemTypes = FS_ALL;
    private int _maxDepth = -1;
    private Log _log;
    private MatcherInterruptCallback _interruptCB;
    private MatcherProgressCallback _progressCB;

    /** 
     * Default constructor.  You've got to manually set everything up
     * with the setXXX and addXXX methods.
     */
    public MatcherConfig () {
        _searchDirs = new ArrayList();
        _matchSelectors = new ArrayList();
        _excludePatterns = new ArrayList();
    }

    /** 
     * Construct an Matcher with initial data.
     * @param dirs The dirs to search.
     * @param matchSelectors A List of MatchSelector objects to use to
     * determine what are matches.
     * @param excludePatterns The patterns to exclude.
     * @param followSymlinks If true, symlinks will be followed.  If false,
     * symlinks will not be followed.
     * @param allowMultipleMatches If true, multiple selectors will be allowe
     * to match a single path.  If false, a MultipleMatchesException will be
     * thrown when multiple matchselectors match a single path.
     * @param fsTypes What filesystems to scan.  Use one of the FS_XXX 
     * constants.
     * @param maxDepth How many directory levels below each search dir will
     * be recursively searched.  Must be a nonnegative number.
     * @param icb The interrupt callback class to use.  This class
     * is used to tell the Matcher when it should stop early and bail out
     * because an interrupt has occurred.
     * @param pcb The progress callback class to use.  This class
     * is notified about each directory that is being scanned.
     * @param log the logging context to use.  This can be null, in which
     * case it is ignored and no log messages are generated.
     */
    public MatcherConfig ( List dirs, 
                           List matchSelectors, 
                           List excludePatterns,
                           boolean followSymlinks,
                           boolean allowMultipleMatches,
                           int fsTypes,
                           int maxDepth,
                           MatcherInterruptCallback icb,
                           MatcherProgressCallback pcb,
                           Log log ) {
        _searchDirs = dirs;
        _matchSelectors = matchSelectors;
        _excludePatterns = excludePatterns;

        _followSymlinks = followSymlinks;
        _allowMultipleMatches = allowMultipleMatches;

        _filesystemTypes = validateFSTypes(fsTypes);
        setMaxDepth(maxDepth);
        _interruptCB = icb;
        _progressCB = pcb;

        _log = log;
    }

    private int validateFSTypes(int fsTypes) {
        if ( fsTypes < 0 || fsTypes > 2 ) {
            throw new IllegalArgumentException("Illegal filesystem types");
        }
        return fsTypes;
    }

    
    public void addSearchDir ( String dir ) { _searchDirs.add(dir); }
    public List getSearchDirs () { return _searchDirs; }
    public void setSearchDirs (List dirs) { _searchDirs = dirs; }

    public void addMatchSelector ( MatchSelector ms ) {
        _matchSelectors.add(ms);
    }
    public List getMatchSelectors () { return _matchSelectors; }
    public void setMatchSelectors (List ms) {
        _matchSelectors = ms;
    }

    public void addExcludePattern ( String pattern ) {
        _excludePatterns.add(pattern);
    }
    public List getExcludePatterns () { return _excludePatterns; }
    public void setExcludePatterns (List patterns) {
        _excludePatterns = patterns;
    }

    public int getFSTypes () { return _filesystemTypes; }
    public void setFSTypes (int fs) { _filesystemTypes = fs; }

    public boolean getFollowSymlinks () { return _followSymlinks; }
    public void setFollowSymlinks (boolean fsm) { _followSymlinks = fsm; }

    public boolean getAllowMultipleMatches () { return _allowMultipleMatches; }
    public void setAllowMultipleMatches (boolean amm) { _allowMultipleMatches = amm; }

    public Log getLog () { return _log; }
    public void setLog (Log log) { _log = log; }

    public int getMaxDepth () { return _maxDepth; }
    public void setMaxDepth (int max) {
        if ( max < 0 && max != DEPTH_INFINITE ) {
            throw new IllegalArgumentException("Max Depth cannot be negative.");
        }
        _maxDepth = max;
    }

    public MatcherInterruptCallback getMatcherInterruptCB () { 
        return _interruptCB;
    }
    public void setMatcherInterruptCB (MatcherInterruptCallback icb) {
        _interruptCB = icb;
    }

    public MatcherProgressCallback getMatcherProgressCB () { 
        return _progressCB;
    }
    public void setMatcherProgressCB (MatcherProgressCallback pcb) {
        _progressCB = pcb;
    }
}
