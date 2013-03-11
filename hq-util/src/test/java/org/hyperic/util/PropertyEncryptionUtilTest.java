/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2012], VMware, Inc.
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
package org.hyperic.util;

import junit.framework.TestCase;
import org.hyperic.util.file.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Testing {@link PropertyEncryptionUtil} functionality.
 */
public class PropertyEncryptionUtilTest extends TestCase {

    /**
     * The test encryption key file name.
     */
    private static final String ENCRYPTION_KEY_FILE_NAME = "test.scu";

    /**
     * The test properties file name.
     */
    private static final String PROPERTIES_FILE_NAME = "src/test/resources/test.properties";

    /**
     * The set of secure properties
     */
    private static final Set<String> SECURE_PROPS = new HashSet<String>() { { add("secure.a"); add("secure.b"); } };


    /**
     * Create an encryption key file providing a valid file name.
     */
    public void testCreateAndStoreEncryptionKeyFile() {
        try {
            // Create and store the encryption key.
            this.createEncryptionKeyFile(ENCRYPTION_KEY_FILE_NAME);
        } catch (PropertyUtilException exc) {
            TestCase.fail(exc.getMessage());
        }
    } // EOM

    /**
     * Create an encryption key file providing an invalid (null) file name.
     */
    public void testCreateAndStoreEncryptionInvalidKeyFile() {
        Exception expected = null;
        try {
            // Create and store the encryption key.
            this.createEncryptionKeyFile(null);
        } catch (PropertyUtilException exc) {
            expected = exc;
        }

        if (expected == null) {
            TestCase.fail("Managed to create an encryption key file providing null as file name.");
        }
    } // EOM

    /**
     * Try overriding an existing encryption key file.
     */
    public void testOverrideEncryptionKeyFile() {
        // Create the encryption-key file for the first time.
        this.testCreateAndStoreEncryptionKeyFile();

        Exception expected = null;
        try {
            // Try overriding the file
            this.createEncryptionKeyFile(ENCRYPTION_KEY_FILE_NAME);
        } catch (PropertyUtilException exc) {
            expected = exc;
        }

        if (expected == null) {
            TestCase.fail("Managed to override an existing encryption-key file");
        }
    } // EOM

    /**
     * Create a new encryption key file, load the key (after it is saved) and make sure they're equal.
     */
    public void testGetPropertyEncryptionKey() {
        try {
            // Create and store the encryption key.
            String encryptionKey = this.createEncryptionKeyFile(ENCRYPTION_KEY_FILE_NAME);
            // Load the encryption key from the disk.
            String loadedEncryptionKey = PropertyEncryptionUtil.getPropertyEncryptionKey(ENCRYPTION_KEY_FILE_NAME);
            // Make sure the saved and loaded match.
            TestCase.assertEquals(encryptionKey, loadedEncryptionKey);
        } catch (PropertyUtilException exc) {
            TestCase.fail(exc.getMessage());
        }
    } // EOM

    /**
     * Try to load the encryption key from a non-existing file.
     */
    public void testGetNonExistingPropertyEncryptionKey() {
        Exception expected = null;
        String encryptionKey = null;
        try {
            // Try getting the key.
            encryptionKey = PropertyEncryptionUtil.getPropertyEncryptionKey(ENCRYPTION_KEY_FILE_NAME);
        } catch (PropertyUtilException exc) {
            expected = exc;
        }

        if (expected == null) {
            TestCase.fail("Managed to get an non-existing key: " + encryptionKey);
        }
    } // EOM

    /**
     * Try to load the encryption key providing null as the file name.
     */
    public void testGetNullPropertyEncryptionKey() {
        Exception expected = null;
        String encryptionKey = null;
        try {
            // Try getting the key.
            encryptionKey = PropertyEncryptionUtil.getPropertyEncryptionKey(null);
        } catch (PropertyUtilException exc) {
            expected = exc;
        }

        if (expected == null) {
            TestCase.fail("Managed to get an non-existing key: " + encryptionKey);
        }
    } // EOM

    /**
     * Ensure properties encryption
     */
    public void testEnsurePropertiesEncryption() {
        // Create a backup of the test properties file.
        backupPropertiesFile();

        try {
            PropertyEncryptionUtil.ensurePropertiesEncryption(
                    PROPERTIES_FILE_NAME, ENCRYPTION_KEY_FILE_NAME, SECURE_PROPS);
        } catch (PropertyUtilException exc) {
            restorePropertiesFile();
            TestCase.fail(exc.getMessage());
        }

        // Restore the test properties file from backup.
        restorePropertiesFile();
    } // EOM

    /**
     * Ensure properties encryption providing invalid properties name.
     */
    public void testEnsureInvalidPropertiesEncryption() {
        // Create a backup of the test properties file.
        backupPropertiesFile();

        Exception expected = null;
        try {
            PropertyEncryptionUtil.ensurePropertiesEncryption(
                    "non-existing.properties", ENCRYPTION_KEY_FILE_NAME, SECURE_PROPS);
        } catch (PropertyUtilException exc) {
            expected = exc;
        }

        // Restore the test properties file from backup.
        restorePropertiesFile();

        if (expected == null) {
            TestCase.fail("Managed to ensure non-existing properties");
        }
    } // EOM

    /**
     * Ensure properties encryption providing null properties name.
     */
    public void testEnsureNullPropertiesEncryption() {
        // Create a backup of the test properties file.
        backupPropertiesFile();

        Exception expected = null;
        try {
            PropertyEncryptionUtil.ensurePropertiesEncryption(
                    null, ENCRYPTION_KEY_FILE_NAME, SECURE_PROPS);
        } catch (PropertyUtilException exc) {
            expected = exc;
        }

        // Restore the test properties file from backup.
        restorePropertiesFile();

        if (expected == null) {
            TestCase.fail("Managed to ensure null properties");
        }
    } // EOM

    /**
     * Ensure properties encryption providing null as the encryption key.
     */
    public void testEnsureNullEncryptionKeyFileEncryption() {
        // Create a backup of the test properties file.
        backupPropertiesFile();

        Exception expected = null;
        try {
            PropertyEncryptionUtil.ensurePropertiesEncryption(
                    PROPERTIES_FILE_NAME, null, SECURE_PROPS);
        } catch (PropertyUtilException exc) {
            expected = exc;
        }

        // Restore the test properties file from backup.
        restorePropertiesFile();

        if (expected == null) {
            TestCase.fail("Managed to ensure null properties");
        }
    } // EOM

    /**
     * Delete existing encryption key file before next test.
     */
    @Override
    protected void setUp() throws Exception {
        // Delegate
        super.setUp();
        // Delete existing encryption-key files.
        this.deleteEncryptionKeyFileIfExists(ENCRYPTION_KEY_FILE_NAME);
    } // EOM

    /**
     * Delete the encryption key file after every test.
     */
    @Override
    protected void tearDown() throws Exception {
        // Delegate
        super.tearDown();
        try {
            // Ensure unlock.
            PropertyEncryptionUtil.unlock(false);

            // Delete existing encryption-key files. Ignore exceptions.
            this.deleteEncryptionKeyFileIfExists(ENCRYPTION_KEY_FILE_NAME);
        } catch (Exception ignore) { /* ignore */ }
    } // EOM

    /**
     * A DRY method that creates an encryption key file named <code>fileName</code>.
     *
     * @param fileName the name of the encryption key file to create.
     * @return the encryption key.
     * @throws PropertyUtilException if something goes wrong (duh...)
     */
    private String createEncryptionKeyFile(String fileName) throws PropertyUtilException {
        // Create and store the encryption key.
        return PropertyEncryptionUtil.createAndStorePropertyEncryptionKey(fileName);
    } // EOM

    /**
     * A DRY method that deletes an existing encryption key file.
     *
     * @param fileName the name of the file to delete.
     */
    private void deleteEncryptionKeyFileIfExists(String fileName) {
        // 'Reference' the encryption key file.
        File encryptionKeyFile = new File(fileName);

        // Make sure the encryption file doesn't exist.
        if (encryptionKeyFile.exists()) {
            if (!encryptionKeyFile.delete()) {
                throw new RuntimeException("Unable to delete an existing key file!");
            }
        }
    } // EOM

    /**
     * A DRY method that backs up the test properties file.
     */
    private void backupPropertiesFile() {
        try {
            // Backup the properties file.
            File orig = new File(PROPERTIES_FILE_NAME);
            File bak = new File(PROPERTIES_FILE_NAME + ".bak");

            FileUtil.copyFile(orig, bak);
        } catch (IOException exc) {
            TestCase.fail(exc.getMessage());
        }
    } // EOM

    /**
     * A DRY method that restores the test properties file.
     */
    private void restorePropertiesFile() {
        try {
            // Backup the properties file.
            File orig = new File(PROPERTIES_FILE_NAME + ".bak");
            File bak = new File(PROPERTIES_FILE_NAME);

            FileUtil.copyFile(orig, bak);
        } catch (IOException exc) {
            TestCase.fail(exc.getMessage());
        }
    } // EOM

} // EOC
