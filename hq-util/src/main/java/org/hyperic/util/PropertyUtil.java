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

public class PropertyUtil {

    private static final Log LOG = LogFactory.getLog(PropertyUtil.class);

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

        storeProperties(file,encryptedEntriesToStore);
    }

    public static void storeProperties(String propFilePath, Map<String,String> newEntries)
            throws PropertyUtilException {

        List<String> lineData = new ArrayList<String>();
        Map<String,String> tmdEntries = new HashMap<String,String>(newEntries);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(propFilePath), "ISO-8859-1"));
            String line;

            while ((line = reader.readLine()) != null) {
                char c = 0;
                int pos = 0;
                while (pos < line.length() && Character.isWhitespace(c = line.charAt(pos))) {pos++;}
                if ((line.length() - pos) == 0 || line.charAt(pos) == '#' || line.charAt(pos) == '!') {
                    lineData.add(line);
                    continue;
                }
                int start = pos;
                boolean needsEscape = line.indexOf('\\', pos) != -1;
                StringBuilder key = needsEscape ? new StringBuilder() : null;
                while ( pos < line.length()
                        && ! Character.isWhitespace(c = line.charAt(pos++))
                        && c != '=' && c != ':') {
                    if (needsEscape && c == '\\') {
                        if (pos == line.length()) {
                            line = reader.readLine();
                            if (line == null) {line = "";}
                            pos = 0;
                            while ( pos < line.length() && Character.isWhitespace(c = line.charAt(pos))) {pos++;}
                        } else {
                            c = line.charAt(pos++);
                            key.append(c);
                        }
                    } else if (needsEscape) {
                        key.append(c);
                    }
                }

                boolean isDelim = (c == ':' || c == '=');
                String keyString;
                if (needsEscape) {
                    keyString = key.toString();
                } else if (isDelim || Character.isWhitespace(c)) {
                    keyString = line.substring(start, pos - 1);
                } else {
                    keyString = line.substring(start, pos);
                }
                while ( pos < line.length() && Character.isWhitespace(c = line.charAt(pos))){
                    pos++;
                }

                if (! isDelim && (c == ':' || c == '=')) {
                    pos++;
                    while (pos < line.length() && Character.isWhitespace(line.charAt(pos))) {
                        pos++;
                    }
                }
                if (!needsEscape) {
                    String val;
                    if ((val = tmdEntries.remove(keyString)) == null) {
                        val = line.substring(pos);
                    }
                    lineData.add(keyString + "=" + val);
                    continue;
                }

                StringBuilder element = new StringBuilder(line.length() - pos);
                while (pos < line.length()) {
                    c = line.charAt(pos++);
                    if (c == '\\') {
                        if (pos == line.length()) {
                            line = reader.readLine();
                            if (line == null) {break;}
                            pos = 0;
                            while ( pos < line.length() && Character.isWhitespace(line.charAt(pos))) {pos++;}
                            element.ensureCapacity(line.length() - pos + element.length());
                        } else {
                            c = line.charAt(pos++);
                            element.append(c);
                            break;
                        }
                    } else {
                        element.append(c);
                    }
                }
                String val;
                if ((val = tmdEntries.remove(keyString)) == null) {
                    val = line.substring(pos);
                }
                lineData.add(keyString + "=" + val);
            }
            for (Map.Entry<String,String> entry : tmdEntries.entrySet()) {
                lineData.add(entry.getKey() + "=" + entry.getValue());
            }
        } catch(Exception exc) {
            throw new PropertyUtilException(exc);
        } finally {
            if (reader!=null) {
                try {
                    reader.close();
                } catch (IOException ignore) {
                    LOG.trace(ignore);
                }
            }
        }

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(propFilePath), "ISO-8859-1"));
            for (String aLineData : lineData) {
                writer.println(aLineData);
            }
            writer.flush ();
        } catch(Exception exc) {
            throw new PropertyUtilException(exc);
        } finally {
            if (writer!=null) {writer.close();}
        }
    }
}
