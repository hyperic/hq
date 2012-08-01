package org.hyperic.util;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.Properties;

/**
 *
 */
public class AutoApproveConfigTest {

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
        testCtorException("", "Managed to create new AutoApproveConfig instance providing string of spaces");
    }

    @Test
    public void createAutoConfigProvidingNonExistingFolder() {
        testCtorException("blah", "Managed to create new AutoApproveConfig instance providing an invalid directory");
    }

    @Test
    public void createAutoConfigProvidingNonExistingFile() {
        AutoApproveConfig autoApproveConfig = new AutoApproveConfig("src");
        TestCase.assertFalse(autoApproveConfig.exists());
    }

    @Test
    public void getPropertiesForResource() {
        AutoApproveConfig autoApproveConfig = new AutoApproveConfig("src/test/resources");
        TestCase.assertTrue(autoApproveConfig.exists());
        TestCase.assertTrue(autoApproveConfig.isAutoApproved("MySQL 5.1.x"));

        Properties props4res = autoApproveConfig.getPropertiesForResource("MySQL 5.1.x");
        TestCase.assertEquals(2, props4res.size());
    }

    @Test
    public void getPropertiesForNoExistingResource() {
        AutoApproveConfig autoApproveConfig = new AutoApproveConfig("src/test/resources");
        TestCase.assertTrue(autoApproveConfig.exists());

        Properties props4res = autoApproveConfig.getPropertiesForResource("your sql");
        TestCase.assertEquals(0, props4res.size());
    }

    private void testCtorException(String agentConfigFolderName, String msg) {
        Throwable expected = null;

        try {
            new AutoApproveConfig(agentConfigFolderName);
        } catch (Exception exc) {
            expected = exc;
        }

        TestCase.assertNotNull(msg, expected);
    }

}
