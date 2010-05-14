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

package org.hyperic.util.validator.common;

import java.util.ResourceBundle;

import junit.framework.*;

import org.hyperic.util.validator.common.CommonValidator;
import org.hyperic.util.validator.common.CommonValidatorException;
import org.xml.sax.SAXException;



/**
 * JUnit TestCase.
 * @testfamily JUnit
 * @testkind testcase
 * @testsetup Default TestCase
 * @testedclass TestCommonValidator
 */
public class CommonValidatorTest extends TestCase {

    private static ResourceBundle messages = null;

    /** Constructs a test case with the given name. */
    public CommonValidatorTest(String name) {
        super(name);
    }

    protected void setUp() {
        System.setProperty(
                           "org.apache.commons.logging.Log",
                           "org.apache.commons.logging.impl.NoOpLog");
        messages = ResourceBundle.getBundle("org.hyperic.util.validator.common.TestResourceBundle");
        // Write your code here
    }

    protected void tearDown() {
        // Write your code here
    }
    
    public void testValidate () {

      
            String TEST_FORM_1 = "testFormOne";
            
            CommonValidator commonValidator =
                new CommonValidator(this.messages);
            
            String validationMappingRes = "test-form-validation.xml";
            MockBean bean = new MockBean();
            
            // Try a good username
            try {
                // test the validation
                bean.setFieldStr1 ("dave------------------24");
                commonValidator.validate(validationMappingRes,
                                         TEST_FORM_1,
                                         bean);
                // If we haven't thrown an exception we're okay.
                this.assertTrue ("good username",true);
            } catch (Throwable t) {
                this.fail("failed testing username dave"+t.getMessage());
            }
            // Try a bad username (x>25 chars)
            try {
                // test the validation
            	bean.setFieldStr1 ("dave-------------------25");
                commonValidator.validate(validationMappingRes, TEST_FORM_1, bean);
                // If we haven't thrown an exception we're okay.
                fail("should have failed on max username violation");
            } catch (CommonValidatorException ex) {
                assertTrue (ex.collapseMessages(),true);
            } catch (SAXException e) {
            	fail("SAXException thrown" + e.getMessage());
            }
            // Try a bad username (x<3 chars)
            try {
                // test the validation
            	bean.setFieldStr1 ("dE");
                commonValidator.validate(validationMappingRes, TEST_FORM_1, bean);
                // If we haven't thrown an exception we're okay.
                fail("should have failed on min username violation");
            } catch (CommonValidatorException ex) {
                assertTrue (ex.collapseMessages(),true);
            } catch (SAXException e) {
            	fail("SAXException thrown" + e.getMessage());
            }
            // Try a bad username (bad chars)
            try {
                // test the validation
            	bean.setFieldStr1 ("desmond@covalent.net");
                commonValidator.validate(validationMappingRes, TEST_FORM_1, bean);
                // If we haven't thrown an exception we're okay.
                fail("should have failed");
            } catch (CommonValidatorException ex) {
                assertTrue (ex.collapseMessages(),true);
            } catch (SAXException e) {
            	fail("SAXException thrown" + e.getMessage());
            }
       
    }
}
