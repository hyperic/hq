/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.product;

import junit.framework.TestCase;

/**
 * Tests the Metric class.
 */
public class MetricTest extends TestCase {

    /**
     * Creates an instance.
     *
     * @param name
     */
    public MetricTest(String name) {
        super(name);
    }
    
    /**
     * 
     */
    public void testDecodeTemplateNoChange() throws Exception {        
        // special encoded characters are:
        //          %2C
        //          %3A
        //          %3D

        // add template strings to test here
        String[] templates = 
            new String[] {
                "no special characters here",
                "another test with an invalid special character %3B",
                "this should not change because \\\\%3B is an invalid escaped character"
        };
        
        for (int i=0; i<templates.length; i++) {
            String original = templates[i];
            String decoded = Metric.decode(original);
            
            assertEquals(original, decoded);
        }
    }
    
    /**
     * 
     */
    public void testDecodeTemplateChange() throws Exception {        
        // special encoded characters are:
        //          %2C
        //          %3A
        //          %3D
        
        // add template strings to test here
        String[] templates = 
            new String[] {
                "an encoded special character here %3A",
                "%3D is another encoded special character",
                "this is the last encoded special character %2C for now",
                "all three (%3A %2C %3D) included here",
                "this should change from \\%3A to \\:",
                "\\\\%3A should be decoded to :",
                "testing the double backslash \\\\%3D",
                "and the comma \\\\%2C needs to be escaped also",
                "all three escaped sequences (\\\\%3A \\\\%2C \\\\%3D) include here",
                "/REQUEST?xmldata=%3C%3Fxml+version\\\\%3D%221.0%22+encoding\\\\%3D%22UTF-8%22%3F%3E%3Clogin_echo_request+schemaVersion\\\\%3D%224.9%22%3E%3Cauthentication%3E%3Cshortcut%3Eacme%3C%2Fshortcut%3E%3Cemail%3Eprajash%3C%2Femail%3E%3Cpassword%3Eprash%3C%2Fpassword%3E%3CuserAgent%3EChannelOnline+Connect%3C%2FuserAgent%3E%3C%2Fauthentication%3E%3Cecho+something\\\\%3D%22whatever%22%3EHello+world%3C%2Fecho%3E%3C%2Flogin_echo_request%3E%0D%0A"
        };
        
        // added expected decoded template strings here
        String[] expected =
            new String[] {
                "an encoded special character here :",
                "= is another encoded special character",
                "this is the last encoded special character , for now",
                "all three (: , =) included here",
                "this should change from \\: to \\:",
                "%3A should be decoded to :",
                "testing the double backslash %3D",
                "and the comma %2C needs to be escaped also",
                "all three escaped sequences (%3A %2C %3D) include here",
                "/REQUEST?xmldata=%3C%3Fxml+version%3D%221.0%22+encoding%3D%22UTF-8%22%3F%3E%3Clogin_echo_request+schemaVersion%3D%224.9%22%3E%3Cauthentication%3E%3Cshortcut%3Eacme%3C%2Fshortcut%3E%3Cemail%3Eprajash%3C%2Femail%3E%3Cpassword%3Eprash%3C%2Fpassword%3E%3CuserAgent%3EChannelOnline+Connect%3C%2FuserAgent%3E%3C%2Fauthentication%3E%3Cecho+something%3D%22whatever%22%3EHello+world%3C%2Fecho%3E%3C%2Flogin_echo_request%3E%0D%0A"
        };        
        
        for (int i=0; i<templates.length; i++) {
            String original = templates[i];
            String decoded = Metric.decode(original);
                        
            assertFalse(original.equals(decoded));
            assertTrue(decoded.equals(expected[i]));
        }
    }
    
    /**
     * 
     */
    public void testEncodeTemplateNoChange() throws Exception {
        // special characters to encode:
        //          ,
        //          :
        //          =

        // add template strings to test here
        String[] templates = 
            new String[] {
                "\\\\%3A should not be encoded",
                "testing the double backslash \\\\%3D",
                "and the escaped comma \\\\%2C also"
        };
        
        for (int i=0; i<templates.length; i++) {
            String original = templates[i];
            String encoded = Metric.encode(original);
            
            assertEquals(original, encoded);
        }
    }
    
    /**
     * 
     */
    public void testEncodeTemplateChange() throws Exception {
        // special characters to encode:
        //          ,
        //          :
        //          =
        
        // add template strings to test here
        String[] templates = 
            new String[] {
                "comma , tested here",
                "colon : tested here",
                "equals sign = tested here"
        };
        
        // added expected encoded template strings here
        String[] expected =
            new String[] {
                "comma %2C tested here",
                "colon %3A tested here",
                "equals sign %3D tested here"                   
        };
        
        for (int i=0; i<templates.length; i++) {
            String original = templates[i];
            String encoded = Metric.encode(original);
            
            assertFalse(original.equals(encoded));
            assertTrue(encoded.equals(expected[i]));
        }       
    }
}
