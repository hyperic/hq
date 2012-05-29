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

import org.hyperic.util.security.SecurityUtil;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.*;

/**
 * A set of utilities to use when encrypting/decryption properties.
 *
 * @author Adi Baron
 */
public class PropertyEncryptionUtil {

    /**
     * When encrypting properties, the encryption key is saved as a serialized object to the disk. However, since an
     * encryption key is a string, it is easily extracted from the binary (serialized) file. Therefore, before
     * serialization the key is encrypted using this obvious value is the key.
     */
    private static final String KEY_ENCRYPTION_KEY = "Security Kung-Fu";

    /**
     * Creates a new encryption key and saves it as a serialized object to the files system.
     *
     * @param fileName the path and name of the file into which the encryption key is serialized.
     * @return the encryption key.
     * @throws PropertyUtilException if the file already exists, the provided file name is invalid,
     *                               or the object serialization fails.
     */
    public static String createAndStorePropertyEncryptionKey(String fileName) throws PropertyUtilException {
        // Validate the file-name
        if (fileName == null || fileName.isEmpty()) {
            throw new PropertyUtilException("Illegal Argument: fileName [" + fileName + "]");
        }

        // Create a new file instance.
        File encryptionKeyFile = new File(fileName);

        // Check if the file already exists. Overriding an encryption file isn't allowed.
        if (encryptionKeyFile.exists()) {
            throw new PropertyUtilException("Attempt to override an encryption key file [" + fileName + "]");
        }

        ObjectOutput output = null;
        String encryptionKey = null;
        try {
            // Create a file output stream and a buffer.
            OutputStream file = new FileOutputStream(encryptionKeyFile);
            OutputStream buffer = new BufferedOutputStream(file);
            // Create the object output stream using the buffer.
            output = new ObjectOutputStream(buffer);
            // Create the encryption key.
            encryptionKey = SecurityUtil.generateRandomToken();
            // Encrypt the key and save it ot the disk.
            String encryptedKey = SecurityUtil.encrypt(
                    SecurityUtil.DEFAULT_ENCRYPTION_ALGORITHM, KEY_ENCRYPTION_KEY, encryptionKey);
            output.writeObject(encryptedKey);
        } catch (Exception exc) {
            throw new PropertyUtilException(exc);
        } finally {
            // Try to close the output stream.
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ignore) { /* ignore */ }
            }
        }

        // Return the key
        return encryptionKey;
    } // EOM

    /**
     * Read the properties encryption key from a file.
     *
     * @param fileName the path and name of the file into which the encryption key is serialized.
     * @return the encryption key.
     * @throws PropertyUtilException if the file doesn't exist, the provided file name is invalid,
     *                               or if the deserialization fails.
     */
    public static String getPropertyEncryptionKey(String fileName) throws PropertyUtilException {
        // Validate the file-name
        if (fileName == null || fileName.isEmpty()) {
            throw new PropertyUtilException("Illegal Argument: fileName [" + fileName + "]");
        }

        // Create a new file instance.
        File encryptionKeyFile = new File(fileName);

        // Make sure that the file exists.
        if (!encryptionKeyFile.exists()) {
            throw new PropertyUtilException("The encryption key file [" + fileName + "] doesn't exist");
        }

        ObjectInput input = null;
        String encryptionKey = null;
        try {
            // Create a file input stream and a buffer.
            InputStream file = new FileInputStream(encryptionKeyFile);
            InputStream buffer = new BufferedInputStream(file);
            // Create the object input stream using the buffer.
            input = new ObjectInputStream(buffer);
            // Read the encrypted key.
            String encryptedKey = (String) input.readObject();
            // Decrypt the hey.
            encryptionKey = SecurityUtil.decrypt(
                    SecurityUtil.DEFAULT_ENCRYPTION_ALGORITHM, KEY_ENCRYPTION_KEY, encryptedKey);
        } catch (Exception exc) {
            throw new PropertyUtilException(exc);
        } finally {
            // Try to close the output stream.
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignore) { /* ignore */ }
            }
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
    public static void ensurePropertiesEncryption(
            String propsFileName, String encryptionKeyFileName, Set<String> secureProps)
            throws PropertyUtilException {

        // Make sure that the properties file exists.
        if (propsFileName == null || propsFileName.isEmpty()) {
            throw new PropertyUtilException("Illegal Argument: propsFileName [" + propsFileName + "]");
        }

        // Make sure that the encryption key name is valid.
        if (encryptionKeyFileName == null || encryptionKeyFileName.isEmpty()) {
            throw new PropertyUtilException("Illegal Argument: encryptionKeyFileName [" + encryptionKeyFileName + "]");
        }

        // If there's nothing to secure then return
        if (secureProps == null || secureProps.size() < 1) {
            return;
        }

        // Acquire lock on the properties file.
        FileLock lock = lockFile(propsFileName);

        // The rest of the code goes inside a try-finally block to make sure we release the lock on the file.
        try {
            // A flag indicating a new key.
            boolean newEncryptionKey = false;
            // The properties encryption key.
            String encryptionKey;

            // 'Reference' the encryption-key file.
            File encryptionFile = new File(encryptionKeyFileName);

            // If the file doesn't exist, create a new one; otherwise load the key.
            if (encryptionFile.exists()) {
                encryptionKey = getPropertyEncryptionKey(encryptionKeyFileName);
            } else {
                encryptionKey = createAndStorePropertyEncryptionKey(encryptionKeyFileName);
                newEncryptionKey = true;
            }

            // Load the properties from the filesystem.
            Properties props = PropertyUtil.loadProperties(propsFileName);

            // Check if the properties are already encrypted.
            boolean alreadyEncrypted = isAlreadyEncrypted(props);

            if (alreadyEncrypted && newEncryptionKey) {
                // The encryption key is new, but the properties are already encrypted.
                throw new PropertyUtilException(
                        "The properties are already encrypted, but the encryption key is missing");
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
            // Release the lock on the properties file.
            releaseFileLock(lock);
        }
    } // EOM

    /**
     * Lock the file <code>propsFileName</code>.
     *
     * @param propsFileName the name of the file to lock.
     * @return a <code>FileLock</code> instance.
     * @throws PropertyUtilException if locking operation fails.
     */
    private static FileLock lockFile(String propsFileName) throws PropertyUtilException {
        try {
            // 'Reference' the file
            File file = new File(propsFileName);
            // Make sure that the file exists.
            if (!file.exists()) {
                throw new PropertyUtilException("Properties file: [" + propsFileName + "] doesn't exist");
            }
            // Create a channel for the file.
            FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
            // Return a lock (this code blocks until a lock is acquired).
            return channel.lock();
        } catch (IOException exc) {
            throw new PropertyUtilException(exc);
        }
    } // EOM

    /**
     * Release the file-lock.
     *
     * @param lock the lock to release.
     * @throws PropertyUtilException if the release operation fails.
     */
    private static void releaseFileLock(FileLock lock) throws PropertyUtilException {
        try {
            lock.release();
        } catch (IOException exc) {
            throw new PropertyUtilException(exc);
        }
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
