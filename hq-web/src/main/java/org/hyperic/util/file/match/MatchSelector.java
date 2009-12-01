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
import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.apache.tools.ant.BuildException;

import org.hyperic.util.JDK;
import org.hyperic.util.StringUtil;

public class MatchSelector implements FileSelector {

    private boolean isWindows;

    private Object key;
    private List matchPatterns;
    private List excludePatterns;

    private MatcherScanner scanner;

    // These are fetched from the scanner.
    private Log log;
    
    public MatchSelector ( Object key ) {
        this.key = key;
        this.matchPatterns = new ArrayList();
        this.excludePatterns = new ArrayList();
        this.isWindows = JDK.IS_WIN32;
    }

    public MatchSelector ( Object key,
                           List matchPatterns,
                           List excludePatterns ) {
        this.key = key;
        this.matchPatterns = matchPatterns;
        this.excludePatterns = excludePatterns;
        this.isWindows = JDK.IS_WIN32;
        if (this.isWindows) {
            int i;
            String pattern;
            if ( this.matchPatterns != null ) {
                for ( i=0; i<this.matchPatterns.size(); i++ ) {
                    pattern = this.matchPatterns.get(i).toString();
                    this.matchPatterns.set(i, transformIfWindows(pattern));
                }
            }
            if ( this.excludePatterns != null ) {
                for ( i=0; i<this.excludePatterns.size(); i++ ) {
                    pattern = this.excludePatterns.get(i).toString();
                    this.excludePatterns.set(i, transformIfWindows(pattern));
                }
            }
        }
    }

    public void setMatcherScanner ( MatcherScanner scanner ) {
        this.scanner = scanner;
        this.log = scanner.getLog();
    }

    public Object getKey () { return key; }
    public void setKey (Object key) { this.key = key; }

    public List getMatchPatterns () { return matchPatterns; }
    public void addMatchPattern (String pattern) { 
        matchPatterns.add(transformIfWindows(pattern));
    }
    public void addMatchPatterns (String[] patterns) { 
        if (patterns == null) {
            return;
        }
        for ( int i=0; i<patterns.length; i++ ) {
            matchPatterns.add(transformIfWindows(patterns[i]));
        }
    }

    public List getExcludePatterns () { return excludePatterns; }
    public void addExcludePattern (String pattern) { 
        excludePatterns.add(transformIfWindows(pattern));
    }
    public void addExcludePatterns (String[] patterns) { 
        if (patterns == null) {
            return;
        }
        for ( int i=0; i<patterns.length; i++ ) {
            excludePatterns.add(transformIfWindows(patterns[i]));
        }
    }

    /** 
     * If case-sensitive comparisons are off, this is Windows system so
     * substitute unix-style path-separators with Windows-style separators
     * (i.e. we replace / with \)
     */
    private String transformIfWindows ( String pattern ) {
        if ( isWindows ) {
            pattern = StringUtil.normalizePath(pattern);
        }
        return pattern;
    }

    private String stripDriveLetter ( String path ) {
        if ( isWindows ) {
            if ( path.length() > 2 && path.charAt(1) == ':' ) {
                path = path.substring(2);
            }
        }
        return path;
    }

    public boolean isSelected(File basedir, String filename, File file)
        throws BuildException {

        int i;
        int size;
        String fullpath = stripDriveLetter(file.getAbsolutePath());

        size = excludePatterns.size();
        for ( i=0; i<size; i++ ) {
            if ( SelectorUtils.matchPath(excludePatterns.get(i).toString(),
                                         fullpath, !isWindows) ) {
                // log.info("MatchSelector(" + key + ") returning false because path (" + fullpath + ") was an excluded path.");
                return false;
            }
        }

        size = matchPatterns.size();
        for ( i=0; i<size; i++ ) {
            // log.info("MatchSelector(" + this + ") testing path=" + fullpath + " against pattern=" + matchPatterns.get(i));
            if ( SelectorUtils.matchPath(StringUtil.normalizePath(matchPatterns.get(i).toString().toLowerCase()),
                                         StringUtil.normalizePath(fullpath.toLowerCase()), !isWindows) ) {
                // No longer need to search any further, 
                scanner.addMatch(key, file.toString());
                // log.info("MatchSelector(" + this + ") returning TRUE because path (" + fullpath + ") MATCHED PATTERN=" + matchPatterns.get(i));
                return true;
            }
        }
        // log.info("MatchSelector(" + this + ") returning FALSE because path (" + fullpath + ") FOUND NO MATCHES.");
        return false;
    }

    public String toString () {
        String rstr = "";
        rstr += "MatchSelector {key=" + key
            + ", match=" + StringUtil.listToString(matchPatterns)
            + ", exclude=" + StringUtil.listToString(excludePatterns) 
            + "}";
        return rstr;
    }
}
