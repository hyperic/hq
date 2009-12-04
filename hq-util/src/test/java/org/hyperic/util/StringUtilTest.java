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

package org.hyperic.util;

import java.util.Arrays;

import org.hyperic.util.StringUtil;

import junit.framework.TestCase;

public class StringUtilTest extends TestCase {
    private static final String[][] CMDS = {
        { " \" \" " }, { "''" },
        { "99", "bottles", "of", "beer", "on", "the", "wall" },
        { "\"this is a \\\"single\\\" argument\"" },
        { "'this is a \"single\" argument'" },
        { "one", "'two too'", "three" },
        { "one", "'\"two too\"'", "three" },
        { "one", "\"\"", "three" },
        { "\"yao ming is 7' 6\\\" tall\"" },
        { "one", "two", "\"'th*ee' four\"" },
        {
            "the", "quick", "fox", "'type=\"brown\"'", "jumped",
            "over", "the", "dog", "\"type='lazy'\""
        },
        { //EXEC-36
            "./script/jrake",
            "cruise:publish_installers", 
            "INSTALLER_VERSION=unstable_2_1", 
            "INSTALLER_PATH=\"/var/lib/ cruise-agent/installers\"", 
            "INSTALLER_DOWNLOAD_SERVER='something'", 
            "WITHOUT_HELP_DOC=true"
        },
        {
            "one", "\\$two", "\"buckle my \\$shoe.\"",
            "\"'three'=\\\"four\\\"\"", "shut", "\"'the\"", "door"
        }
    };

    private static String toString(String[] strings, boolean extract) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < strings.length; i++) {
            if (i > 0) {
                sb.append(" ");
            }
            String item = strings[i];
            if (extract) {
                item = StringUtil.extractQuoted(item.trim());
            }
            sb.append(item);
        }
        return sb.toString();
    }

    public void testSplitCommandLine() throws Exception {
        for (int i=0; i<CMDS.length; i++) {
            String cmdline = toString(CMDS[i], false);
            String[] args;
            try {
                args = StringUtil.explodeQuoted(cmdline);
            } catch (IllegalArgumentException e) {
                System.err.println(cmdline + ": " + e);
                throw e;
            }
            cmdline = toString(CMDS[i], true);
            String result = toString(args, false);
            boolean eq = cmdline.equals(result);
            if (!eq) {
                System.err.println("\n" +
                                   "origin-->" + cmdline + "<--" +
                                   "\n" +
                                   "result-->" + result  + "<--");
            }
            assertTrue(cmdline, eq);
            
            assertTrue(CMDS[i].length + "!=" + args.length +
                       "-->" + Arrays.asList(args),
                       args.length == CMDS[i].length);
        }

        assertTrue(StringUtil.explodeQuoted("").length == 0);
        assertTrue(StringUtil.explodeQuoted(" ").length == 0);
        assertTrue(StringUtil.explodeQuoted(null).length == 0);

        String[] fail = {
           "one \"two three",
           "one 'two three"
        };
        for (int i=0; i<fail.length; i++) {
            try {
                StringUtil.explodeQuoted(fail[i]);
                assertTrue(fail[i] + ": should have failed", false);
            } catch (IllegalArgumentException e) {
                assertTrue(true);
            }
        }
    }
}
