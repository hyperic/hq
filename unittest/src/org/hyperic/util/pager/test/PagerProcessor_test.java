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

package org.hyperic.util.pager.test;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import junit.framework.TestCase;

import org.hyperic.util.pager.Pager;

public class PagerProcessor_test extends TestCase {

    public PagerProcessor_test () {
        super("PagerProcessor_test");
    }
    public PagerProcessor_test (String s) {
        super(s);
    }

    public void testDefaultProcessor () throws Exception {

        int[] default_test_data =
            { 2314, 123, 987, 231, 676, -18, 0, 43,
              9763, 184, 128, 121, -92, 981, 5, 22,
              1986, 747, 865, 810, 796, 397, 2, 32
            };
        int[][] expected_sets = {
            { 2314, 123, 987, 231, 676, -18, 0, 43 },
            { 9763, 184, 128, 121, -92, 981, 5, 22 },
            { 1986, 747, 865, 810, 796, 397, 2, 32 },
            { 22, 1986, 747 }
        };
        int[] expected = null;
        List default_test_data_List = new Vector();
        for ( int i=0; i<default_test_data.length; i++ ) {
            default_test_data_List.add(new Integer(default_test_data[i]));
        }

        Pager p = Pager.getDefaultPager();
        List results = new Vector();
        
        // Test page 0 of size 8
        results.clear();
        p.seek(default_test_data_List, results, 0, 8);
        expected = expected_sets[0];
        validateCorrectResults("set 1: ", results, expected);

        // Test page 1 of size 8
        results.clear();
        p.seek(default_test_data_List, results, 1, 8);
        expected = expected_sets[1];
        validateCorrectResults("set 2: ", results, expected);

        // Test page 2 of size 8
        results.clear();
        p.seek(default_test_data_List, results, 2, 8);
        expected = expected_sets[2];
        validateCorrectResults("set 3: ", results, expected);

        // Test page 0 of size 24
        results.clear();
        p.seek(default_test_data_List, results, 0, 24);
        expected = default_test_data;
        validateCorrectResults("set 4: ", results, expected);

        // Test page 0 of size 240
        results.clear();
        p.seek(default_test_data_List, results, 0, 240);
        expected = default_test_data;
        validateCorrectResults("set 5: ", results, expected);

        // Test page 5 of size 3
        results.clear();
        p.seek(default_test_data_List, results, 5, 3);
        expected = expected_sets[3];
        validateCorrectResults("set 6: ", results, expected);

        // Test page 42 of size -1 (should retrieve everything)
        results.clear();
        p.seek(default_test_data_List, results, 42, -1);
        expected = default_test_data;
        validateCorrectResults("set 7: ", results, expected);
 
        // Test page -1 of size 10 (should retrieve everything)
        results.clear();
        p.seek(default_test_data_List, results, -1, 10);
        expected = default_test_data;
        validateCorrectResults("set 8: ", results, expected);

        // Test page -1 of size -1 (should retrieve everything)
        results.clear();
        p.seek(default_test_data_List, results, -1, -1);
        expected = default_test_data;
        validateCorrectResults("set 9: ", results, expected);
   }

    public void testProcessor () throws Exception {
        String[] test_data = { "one", "two", "three" };
        String[] expected = { "ONE", "TWO", "THREE" };
        List results = new Vector();
        Pager p = Pager.getPager("org.hyperic.util.pager.test.UpcaseProcessor");

        // Get page 0 of size 3
        p.seek(Arrays.asList(test_data), results, 0, 3);
        validateCorrectResults("testProcessor: ", results, expected);
    }

    public void validateCorrectResults ( String msg, List results, int[] expected ) {
        int size = results.size();
        assertEquals(msg + "Incorrect result set size", expected.length, size );
        for ( int i=0; i<size; i++ ) {
            assertEquals(msg + "Incorrect value at index " + i, 
                         expected[i],
                         ((Integer) results.get(i)).intValue());
        }
    }

    public void validateCorrectResults ( String msg, List results, String[] expected ) {
        int size = results.size();
        assertEquals(msg + "Incorrect result set size", expected.length, size );
        for ( int i=0; i<size; i++ ) {
            assertEquals(msg + "Incorrect value at index " + i, 
                         expected[i],
                         ((String) results.get(i)));
        }
    }
}
