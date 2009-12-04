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

package org.hyperic.util.filter;

import java.io.ByteArrayInputStream;

import java.util.Map;
import java.util.HashMap;

import junit.framework.TestCase;

import org.hyperic.util.filter.TokenReplacerFilterInputStream;

public class TokenReplacerFilterInputStreamTest extends TestCase {

    public TokenReplacerFilterInputStreamTest () 
    {
        this("TokenReplacerFilterInputStream_test");
    }

    public TokenReplacerFilterInputStreamTest(String name) 
    {
        super(name);
    }

    // Private utilities
    private Map getFilters()
    {
        Map filters = new HashMap();
        filters.put("user", "rmorgan");
        filters.put("id", "10001");

        return filters;
    }

    public void testValidStringReplacement () throws Exception
    {
        String test = "user id is ${id}, username is ${user}";
        byte[] buf = new byte[1024];

        ByteArrayInputStream byteStream = 
            new ByteArrayInputStream(test.getBytes());
        TokenReplacerFilterInputStream in = 
            new TokenReplacerFilterInputStream(byteStream,
                                               getFilters());

        int len = in.read(buf, 0, buf.length);
        String result = new String(buf, 0, len);

        assertEquals(result.trim(),
                     "user id is 10001, username is rmorgan");
    }
    
    public static void main ( String[] args ) {
        try {
            TokenReplacerFilterInputStreamTest test =
                new TokenReplacerFilterInputStreamTest("main");
            test.testValidStringReplacement();
            System.err.println("Everything is OK");
        } catch ( Exception e ) {
            System.err.println("Exception! " + e.toString());
        }
    }
}
