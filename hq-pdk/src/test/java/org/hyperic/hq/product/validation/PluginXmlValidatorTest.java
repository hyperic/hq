/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.product.validation;

import java.io.File;

import org.hyperic.hq.product.PluginException;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
/**
 * Unit test of the {@link PluginXmlValidator}
 * @author jhickey
 *
 */
public class PluginXmlValidatorTest {

    @Test
    public void testValidate() throws Exception {
        ClassPathResource testPlugin = new ClassPathResource("/hq-plugin.xml");
        File testPluginFile = testPlugin.getFile();
        //Point to the hq-plugin folder under src control to resolve shared XML
        File pluginDir = new File(testPluginFile.getParentFile().getParentFile().getParentFile().getParent() + "/hq-plugin");
        PluginXmlValidator validator = new PluginXmlValidator(pluginDir.getAbsolutePath());
        validator.validatePluginXML(testPluginFile.getAbsolutePath());
    }
    
    @Test(expected=PluginException.class)
    public void testValidateInvalidSharedXmlLocation() throws Exception {
        ClassPathResource testPlugin = new ClassPathResource("/hq-plugin.xml");
        File testPluginFile = testPlugin.getFile();
        File pluginDir = new File("fake");
        PluginXmlValidator validator = new PluginXmlValidator(pluginDir.getAbsolutePath());
        validator.validatePluginXML(testPluginFile.getAbsolutePath());
    }
}
