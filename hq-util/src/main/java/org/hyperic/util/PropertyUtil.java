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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

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
    
//    public static void storeProperties(String file,Set<Map.Entry<String,String>> entriesToStore) {
//        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(file, "8859_1"))
//        FileOutputStream os = null;
//
//        os = new FileOutputStream(file);
////            synchronized (this) {
//            for (Iterator<Map.Entry<String,String>> entriesItr = entriesToStore.iterator(); entriesItr.hasNext();) {
//                Map.Entry<String,String> entry = entriesItr.next();
//                String key = saveConvert((String) entry.getKey(), true, escUnicode);
//                String val = saveConvert((String) entry.getValue(), false, escUnicode);
//                bw.write(key + "=" + val);
//                        bw.newLine();
//            }
////        }
//        bw.flush();
//    }
    
    private static String saveConvert(String theString,
            boolean escapeSpace,
            boolean escapeUnicode) {
         int len = theString.length();
         int bufLen = len * 2;
         if (bufLen < 0) {
             bufLen = Integer.MAX_VALUE;
         }
         StringBuffer outBuffer = new StringBuffer(bufLen);
        
         for(int x=0; x<len; x++) {
             char aChar = theString.charAt(x);
             // Handle common case first, selecting largest block that
             // avoids the specials below
             if ((aChar > 61) && (aChar < 127)) {
                 if (aChar == '\\') {
                     outBuffer.append('\\'); outBuffer.append('\\');
                     continue;
                 }
                 outBuffer.append(aChar);
                 continue;
             }
             switch(aChar) {
         case ' ':
             if (x == 0 || escapeSpace) 
             outBuffer.append('\\');
             outBuffer.append(' ');
             break;
                 case '\t':outBuffer.append('\\'); outBuffer.append('t');
                           break;
                 case '\n':outBuffer.append('\\'); outBuffer.append('n');
                           break;
                 case '\r':outBuffer.append('\\'); outBuffer.append('r');
                           break;
                 case '\f':outBuffer.append('\\'); outBuffer.append('f');
                           break;
                 case '=': // Fall through
                 case ':': // Fall through
                 case '#': // Fall through
                 case '!':
                     outBuffer.append('\\'); outBuffer.append(aChar);
                     break;
                 default:
//                     if (((aChar < 0x0020) || (aChar > 0x007e)) & escapeUnicode ) {
//                         outBuffer.append('\\');
//                         outBuffer.append('u');
//                         outBuffer.append(toHex((aChar >> 12) & 0xF));
//                         outBuffer.append(toHex((aChar >>  8) & 0xF));
//                         outBuffer.append(toHex((aChar >>  4) & 0xF));
//                         outBuffer.append(toHex( aChar        & 0xF));
//                     } else {
                         outBuffer.append(aChar);
//                     }
             }
         }
         return outBuffer.toString();
        }
    
    public static void storeProperties(String file, Properties props, String header,
            String pbePass, String[] encryptedKeys) throws IOException {
        Properties encryptedProps = new Properties(props);
        StandardPBEStringEncryptor encryptor = SecurityUtil.getStandardPBEStringEncryptor(pbePass);
        for (String encryptedKey : encryptedKeys) {
            String encryptedVal = props.getProperty(encryptedKey);
            if(encryptedVal!=null) {
                encryptedProps.setProperty(encryptedKey, encryptor.encrypt(encryptedVal));
            }
        }
        storeProperties(file,encryptedProps,header);
    }
    
    /**
     * Store properties to a file
     */
    public static void storeProperties(String file, Properties props, 
                                       String header)
        throws IOException
    {
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(file);
            props.store(os, header);
        } finally {
            if (os != null) os.close();
        }
    }
}
