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

package org.hyperic.hq.autoinventory;

import java.io.Serializable;
import java.util.List;

import org.hyperic.util.StringUtil;

/**
 * The server signature described HOW to detect whether a server exists
 * on a platform, without providing any details about how to discover anything
 * about it beyond the fact that it exists.  Useful when the agent is
 * trying to discover what it has installed on its platform to decide
 * which plugins it will need to pull from the server. Before the agent has
 * any plugins it can use this class to determine which ones it will need.
 */
public class ServerSignature implements Serializable {

    private String _stName;
    private String[] _fileMatchPatterns;
    private String[] _fileExcludePatterns;
    private String[] _registryMatchPatterns;
    private static final String[] NO_PATTERNS = new String[0];
    
    public ServerSignature () {}

    public ServerSignature (String name,
                            String[] match,
                            String[] exclude,
                            String[] regMatch) {
        _stName = name; 
        _fileMatchPatterns = match;
        _fileExcludePatterns = exclude;
        _registryMatchPatterns = regMatch;
    }

    public ServerSignature(String name,
                           List match,
                           List exclude,
                           List regMatch) {
        _stName = name; 
        if (match != null) {
            _fileMatchPatterns = (String[])match.toArray(new String[0]);
        }
        if (exclude != null) {
            _fileExcludePatterns = (String[])exclude.toArray(new String[0]);
        }
        if (regMatch != null) {
            _registryMatchPatterns = (String[])regMatch.toArray(new String[0]);
        }
    }
    
    public String getServerTypeName () { return _stName; }
    public void setServerTypeName ( String stName ) { _stName = stName; }

    public String[] getFileMatchPatterns () {
        if (_fileMatchPatterns == null) {
            return NO_PATTERNS;
        }
        else {
            return _fileMatchPatterns;
        }
    }
    public void setFileMatchPatterns (String[] pats) { _fileMatchPatterns = pats; }

    public String[] getFileExcludePatterns () {
        if (_fileExcludePatterns == null) {
            return NO_PATTERNS;
        }
        else {
            return _fileExcludePatterns;
        }
    }
    public void setFileExcludePatterns (String[] pats) { _fileExcludePatterns = pats; }

    public String[] getRegistryMatchPatterns () {
        if (_registryMatchPatterns == null) {
            return NO_PATTERNS;
        }
        else {
            return _registryMatchPatterns;
        }
    }
    public void setRegistryMatchPatterns (String[] pats) { _registryMatchPatterns = pats; }

    public String toString () {
        String rstr = "";
        rstr += "ServerSignature {stName=" + _stName
            + ", match=[" + StringUtil.arrayToString(_fileMatchPatterns)
            + "], exclude=[" + StringUtil.arrayToString(_fileExcludePatterns) 
            + "], regMatch=[" + StringUtil.arrayToString(_registryMatchPatterns)
            + "]}";
        return rstr;
    }

    public boolean equals ( Object o ) {
        if ( o instanceof ServerSignature ) {
            ServerSignature ss = (ServerSignature) o;
            if ( !getServerTypeName().equals(ss.getServerTypeName()) ) {
                return false;
            }

            int i;
            String[] patterns1, patterns2;

            patterns1 = getFileMatchPatterns();
            patterns2 = ss.getFileMatchPatterns();
            if ( patterns1.length != patterns2.length ) return false;
            for ( i=0; i<patterns1.length; i++ ) {
                if ( !patterns1[i].equals(patterns2[i]) ) return false;
            }

            patterns1 = getFileExcludePatterns();
            patterns2 = ss.getFileExcludePatterns();
            if ( patterns1.length != patterns2.length ) return false;
            for ( i=0; i<patterns1.length; i++ ) {
                if ( !patterns1[i].equals(patterns2[i]) ) return false;
            }

            patterns1 = getRegistryMatchPatterns();
            patterns2 = ss.getRegistryMatchPatterns();
            if ( patterns1.length != patterns2.length ) return false;
            for ( i=0; i<patterns1.length; i++ ) {
                if ( !patterns1[i].equals(patterns2[i]) ) return false;
            }

            return true;
        }
        return false;
    }
}
