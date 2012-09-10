package org.hyperic.util;

import junit.framework.TestCase;
import org.hyperic.util.security.SecurityUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Properties;

/**
 *
 */
public class AutoApproveConfigTest {

    private static final String PROPS_FOLDER_NAME = "target/test-classes/";
    private static final String ENCRYPTION_KEY_FILE_NAME = "target/test-classes/agent.scu";

    @BeforeClass
    public static void setup() throws Exception {
        // Prepare the encryption key file for the test.
        File encKeyFile = new File(ENCRYPTION_KEY_FILE_NAME);
        if (encKeyFile.exists()) {
            boolean deleted = encKeyFile.delete();
            if (!deleted) { TestCase.fail("failed to delete encryption key file."); }
        }
        boolean created = encKeyFile.createNewFile();
        if (!created) { TestCase.fail("failed to create encryption key file."); }

        PropertyUtil.storeProperties(ENCRYPTION_KEY_FILE_NAME, new HashMap<String, String>() {{
            put("k", "ENC(MHi1LeIpjDZxebFCEsHkLl6TGUBzq5ZfPcZ15ZFqS1apC+taJSjUdSsI+a5UHMdsqvB/8FVHxbIaNXfaGDDWVw\\=\\=)");
        }});

        // Prepare the properties file for the test.
        File propsFile = new File(PROPS_FOLDER_NAME + "auto-approve.properties");
        if (propsFile.exists()) {
            boolean deleted = propsFile.delete();
            if (!deleted) { TestCase.fail("failed to delete auto-approve properties file."); }
        }
        created = propsFile.createNewFile();
        if (!created) { TestCase.fail("failed to create auto-approve properties file."); }

        PropertyUtil.storeProperties(PROPS_FOLDER_NAME + "auto-approve.properties", new HashMap<String, String>() {{
            put("platform", "true");
            put("MySQL\\ 5.1.x", "true");
            put("MySQL\\ 5.1.x.jdbcUser", "hqadmin");
            put("MySQL\\ 5.1.x.jdbcPassword", "hqadmin");
        }});

    }

    @Test
    public void createAutoConfigProvidingNull() {
        testCtorException(null, "Managed to create new AutoApproveConfig instance providing null");
    }

    @Test
    public void createAutoConfigProvidingEmptyString() {
        testCtorException("", "Managed to create new AutoApproveConfig instance providing an empty string");
    }

    @Test
    public void createAutoConfigProvidingStringOfSpaces() {
        testCtorException("  ", "Managed to create new AutoApproveConfig instance providing string of spaces");
    }

    @Test
    public void createAutoConfigProvidingNonExistingFolder() {
        testCtorException("blah", "Managed to create new AutoApproveConfig instance providing an invalid directory");
    }

    @Test
    public void createAutoConfigProvidingNonExistingFile() {
        AutoApproveConfig autoApproveConfig = new AutoApproveConfig("src", ENCRYPTION_KEY_FILE_NAME);
        TestCase.assertFalse(autoApproveConfig.exists());
    }

    @Test
    public void getPropertiesForNonExistingResource() {
        AutoApproveConfig autoApproveConfig = new AutoApproveConfig(PROPS_FOLDER_NAME, ENCRYPTION_KEY_FILE_NAME);
        TestCase.assertTrue(autoApproveConfig.exists());

        Properties props4res = autoApproveConfig.getPropertiesForResource("your sql");
        TestCase.assertEquals(0, props4res.size());
    }

    @Test
    public void getPropertiesForResource() {
        AutoApproveConfig autoApproveConfig = new AutoApproveConfig(PROPS_FOLDER_NAME , ENCRYPTION_KEY_FILE_NAME);
        TestCase.assertTrue(autoApproveConfig.exists());
        TestCase.assertTrue(autoApproveConfig.isAutoApproved("MySQL 5.1.x"));

        Properties props4res = autoApproveConfig.getPropertiesForResource("MySQL 5.1.x");
        TestCase.assertEquals(2, props4res.size());
    }

    @Test
    public void addProperties() throws Exception {
        String propsFileName = PROPS_FOLDER_NAME + "auto-approve.properties";
        Properties properties = PropertyUtil.loadProperties(propsFileName);
        properties.setProperty("New Prop", "true");
        properties.setProperty("New Prop.p1", "New Value1");
        properties.setProperty("New Prop.p2", "New Value2");
        properties.store(new FileOutputStream(propsFileName), null);

        AutoApproveConfig autoApproveConfig = new AutoApproveConfig(PROPS_FOLDER_NAME, ENCRYPTION_KEY_FILE_NAME);

        Properties props4res = autoApproveConfig.getPropertiesForResource("New Prop");
        TestCase.assertEquals(2, props4res.size());

        properties = PropertyUtil.loadProperties(propsFileName);

        for (Object key : properties.keySet()) {
            if (!SecurityUtil.isMarkedEncrypted(properties.getProperty((String) key))) {
                TestCase.fail("properties not encrypted");
            }
        }
    }

    private void testCtorException(String agentConfigFolderName, String msg) {
        AutoApproveConfig config = new AutoApproveConfig(agentConfigFolderName, ENCRYPTION_KEY_FILE_NAME);
        TestCase.assertFalse(msg, config.exists());
    }

}
