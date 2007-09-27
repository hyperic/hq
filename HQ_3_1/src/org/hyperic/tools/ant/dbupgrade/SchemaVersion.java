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

package org.hyperic.tools.ant.dbupgrade;

import java.util.StringTokenizer;

import org.apache.tools.ant.Project;

public class SchemaVersion implements Comparable {

    public static final String INITIAL_VERSION = "R38";
    public static final String DEV_VERSION = "@@@CAM_SCHEMA_VERSION@@@";
    public static final String LATEST_VERSION = "LATEST";

    private String versionString = null;

    private int majorVersion = 0;
    private int minorVersion = 0;
    private int patchVersion = 0;
    private boolean isLatest = false;

    public SchemaVersion (String version) throws IllegalArgumentException {
        versionString = version;
        if (versionString == null || versionString.equals(DEV_VERSION)) {
            versionString = LATEST_VERSION;
        }
        if (versionString.equals(INITIAL_VERSION)) {
            versionString = "0.0.0";
        }
        if (versionString.equalsIgnoreCase(LATEST_VERSION)) {
            isLatest = true;
            majorVersion = 1234567890;
            minorVersion = 1234567890;
            patchVersion = 1234567890;

        } else {
            isLatest = false;
            
            StringTokenizer strtok = new StringTokenizer(versionString, ".");
            if (!strtok.hasMoreTokens())
            {
               throw new IllegalArgumentException("Invalid version: " 
                     + versionString);
            }

            majorVersion = parseInt(strtok.nextToken());

            if (strtok.hasMoreTokens())
            {
               minorVersion = parseInt(strtok.nextToken());
               if (strtok.hasMoreTokens())
               {
                  patchVersion = parseInt(strtok.nextToken());
               }
            }
        }
    }

    public int getMajorVersion () {return majorVersion;}
    public int getMinorVersion () {return minorVersion;}
    public int getPatchVersion () {return patchVersion;}
    public boolean getIsLatest () {return isLatest;}

    public int compareTo(Object o) {
        // System.out.print("Comparing: " + this + " to " + o);
        if ( o == null ) throw new IllegalArgumentException("Cannot compare "
                                                            + "null value");
        if ( o instanceof SchemaVersion ) {
            SchemaVersion v = (SchemaVersion) o;

            int rval;
            if ( this.getMajorVersion() != v.getMajorVersion() ) {
                rval = (this.getMajorVersion() - v.getMajorVersion());
                // System.out.println(" -- major versions differ, comparing majors, returning " + rval);
            } else if (this.getMinorVersion() != v.getMinorVersion() ) {
                rval = (this.getMinorVersion() - v.getMinorVersion());
                // System.out.println(" -- major versions same, comparing minors, returning " + rval);
            } else {
                rval = (this.getPatchVersion() - v.getPatchVersion());
                // System.out.println(" -- minor versions same, comparing patch, returning " + rval);
            }
            return rval;
        }
        
        throw new IllegalArgumentException("Cannot compare "
                                           + "non-SchemaVersion object");
    }

    private int parseInt(String s) throws IllegalArgumentException {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Invalid version: "
                                               + versionString + ": " + nfe);
        }
    }

    public String toString () {
        return versionString;
    }

    public boolean equals ( Object o ) {
        if ( o != null && o instanceof SchemaVersion ) {
            SchemaVersion other = (SchemaVersion) o;
            return (this.majorVersion == other.majorVersion)
                   && (this.minorVersion == other.minorVersion)
                   && (this.patchVersion == other.patchVersion);
        }
        return false;
    }
    
    public boolean between ( SchemaVersion start, SchemaVersion target ) {
        return ( compareTo(start) > 0 && compareTo(target) <= 0 );
    }
}
