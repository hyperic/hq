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

import org.hyperic.util.file.FileUtil;
import org.hyperic.util.security.SecurityUtil;

import java.io.*;
import java.util.*;

/**
 * A set of utilities to use when encrypting/decryption properties.
 */
public class PropertyEncryptionUtil {

    /**
     * When encrypting properties, the encryption key is saved as a serialized object to the disk. However, since an
     * encryption key is a string, it is easily extracted from the binary (serialized) file. Therefore, before
     * serialization the key is encrypted using this obvious value is the key.
     */
    private static final String KEY_ENCRYPTION_KEY = "Security Kung-Fu";

    private static final String ENCRYPTION_KEY_PROP = "k";

    private static final String LOCK_FILE_NAME = System.getProperty("java.io.tmpdir") + "/agent.encrypt.lock";

    /**
     * Read the properties encryption key from a file.
     *
     * @param fileName the path and name of the file into which the encryption key is serialized.
     * @return the encryption key.
     * @throws PropertyUtilException if the file doesn't exist, the provided file name is invalid,
     *                               or if the deserialization fails.
     */
    public static synchronized String getPropertyEncryptionKey(String fileName) throws PropertyUtilException {

        // Validate the file-name
        if (fileName == null || fileName.trim().length() < 1) {
            throw new PropertyUtilException("Illegal Argument: fileName [" + fileName + "]");
        }

        // Create a new file instance.
        File encryptionKeyFile = new File(fileName);

        // Make sure that the file exists.
        if (!encryptionKeyFile.exists()) {
            throw new PropertyUtilException("The encryption key file [" + fileName + "] doesn't exist");
        }

        String encryptionKey;
        try {
            // Read the properties file
            Properties props = new Properties();
            props.load(new FileInputStream(encryptionKeyFile));

            // Get the encrypted key.
            String encryptedKey = props.getProperty(ENCRYPTION_KEY_PROP);

            if (encryptedKey != null) {
                // Decrypt the hey.
                encryptionKey = SecurityUtil.decrypt(
                        SecurityUtil.DEFAULT_ENCRYPTION_ALGORITHM, KEY_ENCRYPTION_KEY, encryptedKey);
            } else {
                throw new PropertyUtilException("Invalid properties encryption key");
            }
        } catch (Exception exc) {
            throw new PropertyUtilException(exc);
        }

        // Return the key.
        return encryptionKey;
    } // EOM

    /**
     * Make sure that the values all secure properties (in the given property file) get encrypted.
     *
     * @param propsFileName         the name of the properties file to encrypt.
     * @param encryptionKeyFileName the name of the encryption key file.
     * @param secureProps           a set of names of secure properties.
     * @throws PropertyUtilException
     */
    public static synchronized void ensurePropertiesEncryption(
            String propsFileName, String encryptionKeyFileName, Set<String> secureProps)
            throws PropertyUtilException {

        // Wait while another thread is executing this process.
        int tries = 10;
        while (!lock() && tries > 0) {
	    tries--;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) { /* ignore */ }
        }
        if (tries <=0) {
            throw new PropertyUtilException(LOCK_FILE_NAME + " is locked. can't continue.");
        }
        
        try {
    
            // Make sure that the properties file exists.
            if (propsFileName == null || propsFileName.trim().length() < 1) {
                throw new PropertyUtilException("Illegal Argument: propsFileName [" + propsFileName + "]");
            }
    
            // Make sure that the encryption key name is valid.
            if (encryptionKeyFileName == null || encryptionKeyFileName.trim().length() < 1) {
                throw new PropertyUtilException("Illegal Argument: encryptionKeyFileName [" + encryptionKeyFileName + "]");
            }
    
            // If there's nothing to secure then return
            if (secureProps == null || secureProps.size() < 1) {
                return;
            }
    
            // Load the properties from the filesystem.
            Properties props = PropertyUtil.loadProperties(propsFileName);
    
            // Check if the properties are already encrypted.
            boolean alreadyEncrypted = isAlreadyEncrypted(props);
    
            // 'Reference' the encryption-key file.
            File encryptionKeyFile = new File(encryptionKeyFileName);
    
            // The properties encryption key.
            String encryptionKey;
    
            // If the encryption key file exists then load it. If it doesn't exist then create one, only if the properties
            // aren't already encrypted. If the properties are encrypted then throw an exception (we can't do much with
            // encrypted properties if the encryption key is missing).
            if (encryptionKeyFile.exists()) {
                encryptionKey = getPropertyEncryptionKey(encryptionKeyFileName);
            } else {
                if (alreadyEncrypted) {
                    // The encryption key is new, but the properties are already encrypted.
                    throw new PropertyUtilException("The properties are already encrypted, but the encryption key is missing");
                }
                encryptionKey = createAndStorePropertyEncryptionKey(encryptionKeyFileName);
            }
    
            // Collect all the properties that should be encrypted but still aren't
            Map<String, String> unEncProps = new HashMap<String, String>();
            for (Enumeration<?> propKeys = props.propertyNames(); propKeys.hasMoreElements(); ) {
                String key = (String) propKeys.nextElement();
                String value = props.getProperty(key);
    
                if (value != null && secureProps.contains(key) && !SecurityUtil.isMarkedEncrypted(value)) {
                    unEncProps.put(key, value);
                }
            }
    
            // Encrypt secure properties.
            if (unEncProps.size() > 0) {
                PropertyUtil.storeProperties(propsFileName, encryptionKey, unEncProps);
            }
        } finally {  
            //Delete the lock file to lock file that prevent parallel processing.
            unlock(false);
        }
    } // EOM

    /**
     * Prevents from other threads to change to properties file.
     *
     * @return true if lock was successful; false otherwise.
     */
    static boolean lock() {
        File lockFile = new File(LOCK_FILE_NAME);
        try {
            return lockFile.createNewFile();
        } catch (IOException ignore) {
            return false;
        }
    }

    /**
     * Remove the lock.
     *
     * @return true if the lock was successfully removed; false otherwise.
     */
    public static boolean unlock(boolean shouldLog) {
        File lockFile = new File(LOCK_FILE_NAME);
        boolean res = lockFile.delete();
        if (res && shouldLog){
            System.out.println(LOCK_FILE_NAME + " was deleted.");//not sure we have log file at this stage, so using system.out.
        }
        return res;
    }

    /**
     * Creates a new encryption key and saves it as a serialized object to the files system.
     *
     * @param fileName the path and name of the file into which the encryption key is serialized.
     * @return the encryption key.
     * @throws PropertyUtilException if the file already exists, the provided file name is invalid,
     *                               or the object serialization fails.
     */
    static synchronized String createAndStorePropertyEncryptionKey(String fileName) throws PropertyUtilException {
        // Validate the file-name
        if (fileName == null || fileName.trim().length() < 1) {
            throw new PropertyUtilException("Illegal Argument: fileName [" + fileName + "]");
        }

        // Create a new file instance.
        File encryptionKeyFile = new File(fileName);

        // Check if the file already exists. Overriding an encryption file isn't allowed.
        if (encryptionKeyFile.exists()) {
            throw new PropertyUtilException("Attempt to override an encryption key file [" + fileName + "]");
        }

        String encryptionKey;
        try {
            // Create the encryption key.
            encryptionKey = SecurityUtil.generateRandomToken();
            // Encrypt the key and save it ot the disk.
            String encryptedKey = SecurityUtil.encrypt(
                    SecurityUtil.DEFAULT_ENCRYPTION_ALGORITHM, KEY_ENCRYPTION_KEY, encryptionKey);
            // Store the key
            Properties props = new Properties();
            props.put(ENCRYPTION_KEY_PROP, encryptedKey);
            
            props.store(new FileOutputStream(fileName), null);
            
            // set read/write permissions to be given to the owner only
            File encKeyFile = new File(fileName);
            FileUtil.setReadWriteOnlyByOwner(encKeyFile);
        } catch (Exception exc) {
            throw new PropertyUtilException(exc);
        }

        // Return the key
        return encryptionKey;
    } // EOM

    /**
     * Check if the provided properties are already encrypted (the secure ones).
     *
     * @param props the properties to check
     * @return true if an encrypted property is found; otherwise false.
     */
    private static boolean isAlreadyEncrypted(Properties props) {
        // Iterate the values
        for (Object value : props.values()) {
            // Check for encryption.
            if (SecurityUtil.isMarkedEncrypted((String) value)) {
                // Encryption found -- return (true) without completing the iteration.
                return true;
            }
        }
        // Non of the values is encrypted -- return false.
        return false;
    } // EOM
}
