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
        File pluginDir = new File(testPluginFile.getParentFile().getParentFile().getParentFile().getParentFile().getParent() + "/hq-plugin");
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
