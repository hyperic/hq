/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.plugin.gfee;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class to handle Gemfire version info
 * and parsing from versions strings.
 */
public class GFVersionInfo {

    private int[] versions;

    public GFVersionInfo(int major, int minor, int mini, int patch) {
        versions = new int[]{major,minor,mini,patch};
    }

    public GFVersionInfo(int major, int minor, int mini) {
        this(major, minor, mini, -1);
    }

    public GFVersionInfo(int major, int minor) {
        this(major, minor, -1, -1);
    }

    public GFVersionInfo(String version) {
        versions = new int[]{-1,-1,-1,-1};
        String[] vArray = version.split("\\.");
        int size = vArray.length;

        if(size < 2 || size > 4)
            throw new IllegalArgumentException("Too many or less fields in given version string:" + version);

        for (int i = 0; i < size; i++) {
            String string = vArray[i];
            try {
                versions[i] = Integer.parseInt(string);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Unable to parse version from string:" + version);
            }
        }
    }

    public int getMajor() {
        return versions[0];
    }

    public int getMinor() {
        return versions[1];
    }

    public int getMini() {
        return versions[2];
    }

    public int getPatch() {
        return versions[3];
    }

    public boolean isGFVersion(String str) {
        String current = toString();
        return current.startsWith(str);
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < versions.length && versions[i]>-1; i++) {
            buf.append(versions[i]);
            buf.append('.');
        }
        String s = buf.toString();
        return s.substring(0, Math.max(s.length()-1,0));
    }

    public static GFVersionInfo parse(String str) {
        Pattern p = Pattern.compile("(\\d+\\.)(\\d+\\.)?(\\d+\\.)?(\\d+)");
        Matcher m = p.matcher(str);
        m.find();
        return new GFVersionInfo(str.substring(m.start(), m.end()));
    }

    public static void main(String[] args) {

        String[] vStrings = new String[]{
                "Java version:   6.6.1 build 33336 10/21/2011 10:29:38 PDT javac 1.5.0_17",
                "Java version:   6.5.1.4 build 30577 02/26/2011 11:57:13 PST javac 1.5.0_17",
                "Java version:   6.5.0 build 30577 02/26/2011 11:57:13 PST javac 1.5.0_17",
                "Java version:   6.0 build 30577 02/26/2011 11:57:13 PST javac 1.5.0_17"			
        };

        for (String string : vStrings) {
            GFVersionInfo info = GFVersionInfo.parse(string);
            System.out.print("Compare / " + string);
            System.out.print("\n");
            System.out.print("Version is " + info);
            System.out.print("\n");
            System.out.print("Is this 6.0: " + info.isGFVersion("6.0"));
            System.out.print("\n");
            System.out.print("Is this 6.5: " + info.isGFVersion("6.5"));
            System.out.print("\n");			
            System.out.print("Is this 6.6: " + info.isGFVersion("6.6"));
            System.out.print("\n");         
        }

    }

}
