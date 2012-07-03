package org.hyperic.hq.autoinventory.agent.server;

import junit.framework.TestCase;
import org.junit.Test;

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
    public void createAutoConfigProvidingExistingFileAndApprovedPlatform() {
        AutoApproveConfig autoApproveConfig = new AutoApproveConfig("src/test/resources");
        TestCase.assertTrue(autoApproveConfig.exists());
        TestCase.assertTrue(autoApproveConfig.isAutoApproved("platform"));
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
