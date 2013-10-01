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

package org.hyperic.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.security.SecurityUtil;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PropertyUtil {

    private static final Log LOG = LogFactory.getLog(PropertyUtil.class);

    /**
     * A regular expression pattern that is used for splitting properties lines into three parts: key, ':' or '=', and
     * value. This pattern is used for rewriting properties file (when values need to be encrypted.
     */
    private static final Pattern PROPERTY_LINE_PATTERN = Pattern.compile("([^=:]*)([=|:])(.*)");

    /**
     * Expand variable references in property values.
     *
     * I.e. if you have a props file:
     *
     * a=foo
     * b=bar
     * c=${a} ${b}
     *
     * The value for 'c' will be 'foo bar'
     *
     * @param props  Properties to replace
     *
     */
    public static void expandVariables(Map props) {
        List<String> vars = new ArrayList<String>();

        for (Object o : props.entrySet()) {
            Map.Entry ent = (Map.Entry) o;
            String value;
            int idx;

            value = (String) ent.getValue();
            idx = value.indexOf("${");

            if (idx == -1)
                continue;

            vars.clear();
            while (idx != -1) {
                int endIdx = value.indexOf("}", idx);

                if (endIdx == -1)
                    break;

                endIdx++;
                vars.add(value.substring(idx, endIdx));
                idx = value.indexOf("${", endIdx);
            }

            for (String replace : vars) {
                String replaceVar, lookupVal;

                replaceVar = replace.substring(2, replace.length() - 1);
                lookupVal = (String) props.get(replaceVar);
                if (lookupVal == null)
                    continue;

                value = StringUtil.replace(value, replace,
                        lookupVal);
            }
            props.put(ent.getKey(), value);
        }
    }

    /**
     * Load properties from a file.
     */
    public static Properties loadProperties (String file) throws PropertyUtilException {
        FileInputStream fi = null;
        Properties props = new Properties();
        try {
            fi = new FileInputStream(file);
            props.load(fi);
        } catch (Exception exc) {
            throw new PropertyUtilException(exc);
        } finally {
            if (fi != null) try {
                fi.close();
            } catch (IOException ignore) {
                LOG.trace(ignore);
            }
        }
        return props;
    }

    /**
     * encrypt the input entries and append them at the end of the property file.
     * Any entries with identical keys would be erased.
     *
     * @param file the name of the properties file
     * @param propEncKey the properties encryption key
     * @param entriesToStore a map of properties to store
     * @throws PropertyUtilException
     */
    public static void storeProperties(String file, String propEncKey, Map<String,String> entriesToStore)
            throws PropertyUtilException {

        Map<String,String> encryptedEntriesToStore = new HashMap<String,String>();
        for (Map.Entry<String, String> entryToStore : entriesToStore.entrySet()) {
            String encryptedVal = SecurityUtil.encrypt(
                    SecurityUtil.DEFAULT_ENCRYPTION_ALGORITHM, propEncKey, entryToStore.getValue());
            encryptedEntriesToStore.put(entryToStore.getKey(), encryptedVal);
        }

        _storeProperties(file,encryptedEntriesToStore);
    }

    /**
     * Saves the provided map of keys and values into the properties file specified by <code>propFilePath</code>.
     * Values of properties that already exist int the file are overwritten. New values are placed near the end of the
     * file.
     *
     * @param propFilePath the path (absolute/relative) to the properties file to edit.
     * @param props the properties to add/update.
     */
    public static void storeProperties(String propFilePath, Map<String, String> props) throws PropertyUtilException {
        Map<String,String> duplicatedEntriesToStore = new HashMap<String,String>();
        for (Map.Entry<String, String> entryToStore : props.entrySet()) {
            duplicatedEntriesToStore.put(entryToStore.getKey(), entryToStore.getValue());
        }

        _storeProperties(propFilePath, duplicatedEntriesToStore);
    }

    /**
     * Saves the provided map of keys and values into the properties file specified by <code>propFilePath</code>.
     * Values of properties that already exist int the file are overwritten. New values are placed near the end of the
     * file.
     *
     * @param propFilePath the path (absolute/relative) to the properties file to edit.
     * @param props the properties to add/update.
     */
    private static void _storeProperties(String propFilePath, Map<String, String> props) throws PropertyUtilException {
        // If the provided properties map is null or empty then exit the method.
        if (props == null || props.size() < 1) {
            return;
        }

        // Used for writing the properties file back to the disk.
        PrintWriter writer = null;
        // Used for reading the properties file from the disk.
        FileReader reader = null;

        try {
            // Create new reader
            reader = new FileReader(propFilePath);
            // Wrap the reader with a buffer so we can walk through the file line by line.
            BufferedReader bufferedReader = new BufferedReader(reader);

            // The list of lines that will be written to disk.
            List<String> newLines = new ArrayList<String>();

            // Read the lines from the file and replace values.
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                newLines.add(processLine(line, props));
            }// EO while.

            // Iterate values that are left in the provided map and add them to the file.
            for (String key : props.keySet()) {
                newLines.add(key + " = " + props.get(key));
            }

            // TODO: change to UTF-8 and add support on property loader side. This might help:
            // http://stackoverflow.com/questions/863838/problem-with-java-properties-utf8-encoding-in-eclipse
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(propFilePath), "ISO-8859-1"));
            for (String aLineData : newLines) {
                writer.println(aLineData);
            }
            writer.flush();
        } catch (IOException exc) {
            String message = "Failed to store properties into the file: " + propFilePath;
            LOG.error(message, exc);
            throw new PropertyUtilException(exc);
        } finally {
            if (reader != null) { try { reader.close(); } catch (IOException ignore) { /* ignore */ } }
            if (writer != null) { writer.close(); }
        } // EO try-catch
    } // EOM

    /**
     * Splits the provided properties line into key, ':' or '=', and value and checks the props map for a new value for
     * the extracted key. If there is a new value then the property line is reassembled using the new value; Otherwise
     * the line is returned as is.
     *
     * @param line the property line to process.
     * @param props the properties map in which a new value for the property might reside.
     * @return the final (modified or same) property line to write in the properties file.
     */
    private static String processLine(String line, Map<String, String> props) {
        // The result line.
        String result;

        // User the regex pattern to split the line.
        Matcher matcher = PROPERTY_LINE_PATTERN.matcher(line);
        // If a match is found then replace it value if there's a matching key in the provided map.
        if (matcher.find()) {
            // Extract the key (strip the escape characters).
            String key = matcher.group(1).trim().replaceAll("\\\\", "");
            // Try removing the entry from the map.
            String value = props.remove(key);
            if (value == null) {
                // No matching found. Add the line as is.
                result = line;
            } else {
                // Found a match -- replace the value.
                result = matcher.group(1) + matcher.group(2) + value;
            }
        } else {
            // No match found. Add the line as is.
            result = line;
        }

        return result;
    }// EOM
}
