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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.hyperic.util.security.SecurityUtil;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

public class PropertyUtil {
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
        ArrayList vars = new ArrayList();

        for(Iterator i=props.entrySet().iterator(); i.hasNext(); ){
            Map.Entry ent = (Map.Entry)i.next();
            String value;
            int idx;

            value  = (String)ent.getValue();
            idx    = value.indexOf("${");

            if(idx == -1)
                continue;

            vars.clear();
            while(idx != -1){
                int endIdx = value.indexOf("}", idx);

                if(endIdx == -1)
                    break;

                endIdx++;
                vars.add(value.substring(idx, endIdx));
                idx = value.indexOf("${", endIdx);
            }

            for(Iterator j=vars.iterator(); j.hasNext(); ){
                String replace = (String)j.next();
                String replaceVar, lookupVal;

                replaceVar = replace.substring(2, replace.length() - 1);
                lookupVal  = (String)props.get(replaceVar);
                if(lookupVal == null)
                    continue;

                value = StringUtil.replace(value, replace, 
                        lookupVal);
            }
            props.put(ent.getKey(), value);
        }
    }

    /**
     * Strip a prefix from the keys in a properties object.
     *
     * Mainly used for backwards compatibility of net.covalent
     * property keys.
     */
    public static void stripKeys(Properties props, String prefix)
    {
        Properties stripped = new Properties();

        for(Iterator i = props.entrySet().iterator(); i.hasNext(); ){
            Map.Entry ent = (Map.Entry)i.next();
            String key = (String)ent.getKey();

            if (key.startsWith(prefix)) {
                stripped.setProperty(key.substring(prefix.length()),
                        (String)ent.getValue());
                // Remove, will be re-merged later
                i.remove();
            }
        }

        props.putAll(stripped);
    }

    /**
     * Load properties from a file.
     */
    public static Properties loadProperties (String file) throws IOException {
        FileInputStream fi = null;
        Properties props = new Properties();
        try {
            fi = new FileInputStream(file);
            props.load(fi);
        } finally {
            if (fi != null) fi.close();
        }
        return props;
    }

    public static Properties loadProperties(String file, String pbePass, String[] encryptedKeys) throws IOException {
        Properties props = loadProperties(file);
        if (pbePass==null || encryptedKeys==null || encryptedKeys.length==0){
            return props;
        }
        StandardPBEStringEncryptor encryptor = SecurityUtil.getStandardPBEStringEncryptor(pbePass);
        for (String encryptedKey : encryptedKeys) {
            String encryptedVal = props.getProperty(encryptedKey);
            if(encryptedVal!=null) {
                props.setProperty(encryptedKey, encryptor.decrypt(encryptedVal));
            }
        }
        return props;
    }

    /**
     * encrypt the input entries and append them at the end of the property file.
     * Any entries with identical keys would be erased.
     * 
     * @param file
     * @param propEncKey
     * @param entriesToStore
     * @throws IOException
     */
    public static void storeProperties(String file, String propEncKey, Map<String,String> entriesToStore) throws IOException {
        Map<String,String> encryptedEntriesToStore = new HashMap<String,String>();
        for (Iterator<Map.Entry<String, String>> entriesToStoreItr = entriesToStore.entrySet().iterator();
                entriesToStoreItr.hasNext();) {
            Map.Entry<String, String> entryToStore = entriesToStoreItr.next();
            String encryptedVal = SecurityUtil.encrypt(SecurityUtil.DEFAULT_ENCRYPTION_ALGORITHM,propEncKey,entryToStore.getValue());
            encryptedEntriesToStore.put(entryToStore.getKey(), encryptedVal);
        }

        storeProperties(file,encryptedEntriesToStore);
    }

    /**
     * Store properties to a file
     */
    public static void storeProperties(String file, Properties props, String header) throws IOException {
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(file);
            props.store(os, header);
        } finally {
            if (os != null) os.close();
        }
    }

    public static String getPropEncKey(String propEncKeyPath) throws IOException, ClassNotFoundException {
        String propEncKey = null;
        File propEncKeyFile = new File(propEncKeyPath);

        // get prop encryption from file
        if (propEncKeyFile.exists()) {
            ObjectInputStream propKeyOIS = null;
            try {
                propKeyOIS = new ObjectInputStream(new FileInputStream(propEncKeyFile));
                propEncKey = (String) propKeyOIS.readObject();
            } finally {
                if (propKeyOIS!=null) {propKeyOIS.close();}
            }
            // generate key file
        } else {
            ObjectOutputStream propEncKeyOOS = null;
            try {
                propEncKeyOOS = new ObjectOutputStream(new FileOutputStream(propEncKeyFile));
                propEncKey = SecurityUtil.generateRandomToken();
                propEncKeyOOS.writeObject(propEncKey);
            } finally {
                if (propEncKeyOOS!=null) {propEncKeyOOS.close();}
            }
        }
        return propEncKey;
    }

    public static void storeProperties(String propFilePath, Map<String,String> newEntries) throws NumberFormatException, IOException {
        Vector<String> lineData = new Vector<String>();
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
                StringBuffer key = needsEscape ? new StringBuffer() : null;
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
                    while (pos < line.length() && Character.isWhitespace(c = line.charAt(pos))) {
                        pos++;
                    }
                }
                if (!needsEscape) {
                    String val=null;
                    if ((val = tmdEntries.remove(keyString)) == null) {
                        val = line.substring(pos);
                    }
                    lineData.add(keyString + "=" + val);
                    continue;
                }

                StringBuffer element = new StringBuffer(line.length() - pos);
                while (pos < line.length()) {
                    c = line.charAt(pos++);
                    if (c == '\\') {
                        if (pos == line.length()) {
                            line = reader.readLine();
                            if (line == null) {break;}
                            pos = 0;
                            while ( pos < line.length() && Character.isWhitespace(c = line.charAt(pos))) {pos++;}
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
                String val=null;
                if ((val = tmdEntries.remove(keyString)) == null) {
                    val = line.substring(pos);
                }
                lineData.add(keyString + "=" + val);
            }
            for (Map.Entry<String,String> entry : tmdEntries.entrySet()) {
                lineData.add(entry.getKey() + "=" + entry.getValue());
            }
        } finally {
            if (reader!=null) {reader.close();}
        }

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(propFilePath), "ISO-8859-1"));
            for (Iterator<String> itr = lineData.iterator(); itr.hasNext();) {
                writer.println (itr.next());
            } 
            writer.flush ();
        } finally {
            if (writer!=null) {writer.close();}
        }
    }

    public static void main(String[] args) throws NumberFormatException, IOException {
        Map<String,String> m = new HashMap<String,String>();
        m.put("agent.setup.camPword", "AAAA");
        m.put("accept.unverified.certificates", "BBB");
        m.put("new", "CCC");
        storeProperties("/work/agent-4.7-ee/conf/agent.properties", m);
    }
}
