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

package org.hyperic.util.filter.test;

import junit.framework.TestCase;
import org.hyperic.util.filter.TokenReplacerFilterReader;

public class TokenReplacerFilterReader_test extends TestCase {

    public TokenReplacerFilterReader_test () 
    {
        this("TokenReplacerFilterReader_test");
    }

    public TokenReplacerFilterReader_test(String name) 
    {
        super(name);
    }

    public void testValidStringReplacement () throws Exception
    {
        char buf[] = new char[1024];

        String testStr = "test replacement of ${user}";
        TokenReplacerFilterReader pr = 
            new TokenReplacerFilterReader(testStr);
        pr.addFilter("user", "rmorgan");

        int len = pr.read(buf, 0, buf.length);
        String result = new String(buf, 0, len);
        assertEquals("test replacement of rmorgan", result.trim());
    }

    public void testInvalidStringReplacement() throws Exception
    {
        char buf[] = new char[1024];

        String testStr = "nothing will be ${replaced} ${here}";
        TokenReplacerFilterReader pr = 
            new TokenReplacerFilterReader(testStr);
        pr.addFilter("", "test");
        pr.addFilter("user", "rmorgan");

        int len = pr.read(buf, 0, buf.length);
        String result = new String(buf, 0, len);
        assertEquals(testStr, result.trim());
    }

    public static void main ( String[] args ) {
        try {
            TokenReplacerFilterReader_test pr =
                new TokenReplacerFilterReader_test("main");
            pr.testValidStringReplacement();
            pr.testInvalidStringReplacement();
            System.err.println("Everything is OK");
        } catch ( Exception e ) {
            System.err.println("Exception! " + e.toString());
        }
    }
}
