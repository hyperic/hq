package org.hyperic.util;

import junit.framework.TestCase;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.StringConfigOption;

import javax.swing.text.html.HTMLDocument;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ConfigSchemaTest extends TestCase {

    public ConfigSchemaTest(String name) {
        super(name);
    }

    private ConfigOption[] generateConfigOptions(int num) {
        ConfigOption[] opts = new ConfigOption[num];
        for (int i = 0 ; i < num; i++) {
            StringConfigOption opt = new StringConfigOption("Opt Name", "Opt Description");
            opts[i] = opt;
        }
        return opts;
    }

    public void testConfigOptionsImmutable() throws Exception {

        ConfigOption[] opts = generateConfigOptions(10);
        ConfigSchema schema = new ConfigSchema(opts);

        try {
            schema.getOptions().remove(0);
            fail("Able to remove options from getOptions()");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    public void testConfigOptionsConcurrency() throws Exception {
        final int NUM = 10;
        ConfigOption[] opts1 = generateConfigOptions(NUM);
        ConfigOption[] opts2 = generateConfigOptions(NUM);
        ConfigSchema schema = new ConfigSchema(opts1);

        int count = 0;
        List opts = schema.getOptions();
        for (Iterator i = schema.getOptions().iterator(); i.hasNext(); ) {
            ConfigOption o = (ConfigOption)i.next();
            if (++count == 5) {
                // Setting options during iteration should not throw a ConcurrentModificationException
                schema.addOptions(Arrays.asList(opts2));
            }
        }
    }
}
