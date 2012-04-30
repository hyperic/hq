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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
     * store only the particular given properties to the given file,
     * using the properties file format
     * (in order for the file to be readable later on by Properties.load, etc.)
     * 
     * @param propFile
     * @param encKey
     * @param entriesToStore
     * @throws IOException 
     */
    public static void storeProperties(String propFilePath, Map<String,String> entriesToStore) throws IOException {
        Properties props = loadProperties(propFilePath);
//        // find entries position in the file
//        InputStreamReader propReader = null;
//        List<PropEntry> propEntries = null;
//        try {
//            propReader = new InputStreamReader(new FileInputStream(propFile), "8859_1");
//            propEntries = findKeysPosition(new LineReader(propReader),entriesToStore.keySet());
//        } finally {
//            if (propReader!=null) {propReader.close();}
//        }
//        // write-over/append entries to file
//        OutputStreamWriter propWriter = null;
//        try {
//            propWriter = new OutputStreamWriter(new FileOutputStream(propFile), "8859_1");
//            writeOverOrAppend(propWriter, propEntries);
//        } finally {
//            if (propWriter!=null) {propWriter.close();}
//        }
//        
//        //            synchronized (this) {
        for (Iterator<Map.Entry<String,String>> entriesItr = entriesToStore.entrySet().iterator(); entriesItr.hasNext();) {
                Map.Entry<String,String> entry = entriesItr.next();
                props.setProperty(entry.getKey(), entry.getValue());
//                String key = saveConvert(entry.getKey(), true);
//                String val = saveConvert(entry.getValue(), false);
//                bw.write(key + "=" + val);
//                        bw..newLine();
//            }
        }
        storeProperties(propFilePath, props, "");
//        bw.flush();
    }
    
    private static void writeOverOrAppend(OutputStreamWriter propWriter ,List<PropEntry> propEntries) {
//        for (PropEntry propEntry : propEntries) {
//            if (!propEntry.isOldEntryExists()) {
//                propWriter.write(str, off, len)
//            } else {
//                propWriter.append()
//            }
// 
//        }
    }
    
    private static class PropEntry implements Comparable {
        public PropEntry(int oldValPos, int oldValLen, String key, String newVal) {
            this.oldValPos = oldValPos;
            this.oldValLen = oldValLen;
            this.key = key;
            this.newVal = newVal;
        }

        public boolean isOldEntryExists() {
            return -1!=this.oldValPos;
        }
        public int getOldValPos() {
            return oldValPos;
        }

        public int getOldValLen() {
            return oldValLen;
        }

        public String getKey() {
            return key;
        }

        public String getNewVal() {
            return newVal;
        }

        private final int oldValPos;
        private final int oldValLen;
        private final String key;
        private final String newVal;
        
        public int compareTo(Object o) {
            PropEntry other = (PropEntry)o;
            return this.isOldEntryExists() == other.isOldEntryExists()?
                    this.oldValPos-other.oldValPos:
                    this.isOldEntryExists()?1:-1;
                                
        }
    }
    
    private static List<PropEntry> findKeysPosition (LineReader lr, Set<String> keys) throws IOException {
        char[] convtBuf = new char[1024];
        int limit;
        int keyLen;
        int valueStart;
        char c;
        boolean hasSep;
        boolean precedingBackslash;

        while ((limit = lr.readLine()) >= 0) {
            c = 0;
            keyLen = 0;
            valueStart = limit;
            hasSep = false;

            precedingBackslash = false;
            while (keyLen < limit) {
                c = lr.lineBuf[keyLen];
                //need check if escaped.
                if ((c == '=' ||  c == ':') && !precedingBackslash) {
                    valueStart = keyLen + 1;
                    hasSep = true;
                    break;
                } else if ((c == ' ' || c == '\t' ||  c == '\f') && !precedingBackslash) {
                    valueStart = keyLen + 1;
                    break;
                } 
                if (c == '\\') {
                    precedingBackslash = !precedingBackslash;
                } else {
                    precedingBackslash = false;
                }
                keyLen++;
            }
            while (valueStart < limit) {
                c = lr.lineBuf[valueStart];
                if (c != ' ' && c != '\t' &&  c != '\f') {
                    if (!hasSep && (c == '=' ||  c == ':')) {
                        hasSep = true;
                    } else {
                        break;
                    }
                }
                valueStart++;
            }
            String key = loadConvert(lr.lineBuf, 0, keyLen, convtBuf);
            String value = loadConvert(lr.lineBuf, valueStart, limit - valueStart, convtBuf);
//            put(key, value);
        }
        return null;
    }

    /* Read in a "logical line" from an InputStream/Reader, skip all comment
     * and blank lines and filter out those leading whitespace characters 
     * (\u0020, \u0009 and \u000c) from the beginning of a "natural line". 
     * Method returns the char length of the "logical line" and stores 
     * the line in "lineBuf". 
     */
    static class LineReader {
        public LineReader(InputStream inStream) {
            this.inStream = inStream;
            inByteBuf = new byte[8192]; 
        }

        public LineReader(Reader reader) {
            this.reader = reader;
            inCharBuf = new char[8192]; 
        }

        byte[] inByteBuf;
        char[] inCharBuf;
        char[] lineBuf = new char[1024];
        int inLimit = 0;
        int inOff = 0;
        InputStream inStream;
        Reader reader;

        int readLine() throws IOException {
            int len = 0;
            char c = 0;

            boolean skipWhiteSpace = true;
            boolean isCommentLine = false;
            boolean isNewLine = true;
            boolean appendedLineBegin = false;
            boolean precedingBackslash = false;
            boolean skipLF = false;

            while (true) {
                if (inOff >= inLimit) {
                    inLimit = (inStream==null)?reader.read(inCharBuf)
                                      :inStream.read(inByteBuf);
                    inOff = 0;
                    if (inLimit <= 0) {
                        if (len == 0 || isCommentLine) { 
                            return -1; 
                        }
                        return len;
                    }
                }
                if (inStream != null) {
                    //The line below is equivalent to calling a 
                    //ISO8859-1 decoder.
                    c = (char) (0xff & inByteBuf[inOff++]);
                } else {
                    c = inCharBuf[inOff++];
                }
                if (skipLF) {
                    skipLF = false;
                    if (c == '\n') {
                        continue;
                    }
                }
            if (skipWhiteSpace) {
                if (c == ' ' || c == '\t' || c == '\f') {
                    continue;
                }
                if (!appendedLineBegin && (c == '\r' || c == '\n')) {
                    continue;
                }
                skipWhiteSpace = false;
                appendedLineBegin = false;
            }
            if (isNewLine) {
                isNewLine = false;
                if (c == '#' || c == '!') {
                    isCommentLine = true;
                    continue;
                }
            }
            if (c != '\n' && c != '\r') {
                lineBuf[len++] = c;
                if (len == lineBuf.length) {
                    int newLength = lineBuf.length * 2;
                    if (newLength < 0) {
                        newLength = Integer.MAX_VALUE;
                    }
                    char[] buf = new char[newLength];
                    System.arraycopy(lineBuf, 0, buf, 0, lineBuf.length);
                    lineBuf = buf;
                }
                //flip the preceding backslash flag
                if (c == '\\') {
                    precedingBackslash = !precedingBackslash;
                } else {
                    precedingBackslash = false;
                }
            } else {
                // reached EOL
                if (isCommentLine || len == 0) {
                    isCommentLine = false;
                    isNewLine = true;
                    skipWhiteSpace = true;
                    len = 0;
                    continue;
                }
                if (inOff >= inLimit) {
                    inLimit = (inStream==null)
                          ?reader.read(inCharBuf)
                          :inStream.read(inByteBuf);
                    inOff = 0;
                    if (inLimit <= 0) {
                        return len;
                    }
                }
                if (precedingBackslash) {
                    len -= 1;
                    //skip the leading whitespace characters in following line
                    skipWhiteSpace = true;
                    appendedLineBegin = true;
                    precedingBackslash = false;
                    if (c == '\r') {
                        skipLF = true;
                    }
                } else {
                    return len;
                }
            }
            }
        }
    }
    
    /*
     * Converts encoded &#92;uxxxx to unicode chars
     * and changes special saved chars to their original forms
     */
    private static String loadConvert (char[] in, int off, int len, char[] convtBuf) {
        if (convtBuf.length < len) {
            int newLen = len * 2;
            if (newLen < 0) {
            newLen = Integer.MAX_VALUE;
        } 
        convtBuf = new char[newLen];
        }
        char aChar;
        char[] out = convtBuf; 
        int outLen = 0;
        int end = off + len;

        while (off < end) {
            aChar = in[off++];
            if (aChar == '\\') {
                aChar = in[off++];   
                if(aChar == 'u') {
                    // Read the xxxx
                    int value=0;
            for (int i=0; i<4; i++) {
                aChar = in[off++];  
                switch (aChar) {
                  case '0': case '1': case '2': case '3': case '4':
                  case '5': case '6': case '7': case '8': case '9':
                     value = (value << 4) + aChar - '0';
                 break;
              case 'a': case 'b': case 'c':
                          case 'd': case 'e': case 'f':
                 value = (value << 4) + 10 + aChar - 'a';
                 break;
              case 'A': case 'B': case 'C':
                          case 'D': case 'E': case 'F':
                 value = (value << 4) + 10 + aChar - 'A';
                 break;
              default:
                              throw new IllegalArgumentException(
                                           "Malformed \\uxxxx encoding.");
                        }
                     }
                    out[outLen++] = (char)value;
                } else {
                    if (aChar == 't') aChar = '\t'; 
                    else if (aChar == 'r') aChar = '\r';
                    else if (aChar == 'n') aChar = '\n';
                    else if (aChar == 'f') aChar = '\f'; 
                    out[outLen++] = aChar;
                }
            } else {
            out[outLen++] = aChar;
            }
        }
        return new String (out, 0, outLen);
    }

    private static String saveConvert(String theString,
            boolean escapeSpace) {
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
                     if ((aChar < 0x0020) || (aChar > 0x007e)) {
                         outBuffer.append('\\');
                         outBuffer.append('u');
                         outBuffer.append(toHex((aChar >> 12) & 0xF));
                         outBuffer.append(toHex((aChar >>  8) & 0xF));
                         outBuffer.append(toHex((aChar >>  4) & 0xF));
                         outBuffer.append(toHex( aChar        & 0xF));
                     } else {
                         outBuffer.append(aChar);
                     }
             }
         }
         return outBuffer.toString();
        }
    
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
     * Convert a nibble to a hex character
     * @param   nibble  the nibble to convert.
     */
    private static char toHex(int nibble) {
    return hexDigit[(nibble & 0xF)];
    }

    /** A table of hex digits */
    private static final char[] hexDigit = {
    '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
    };

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

    public static String getPropEncKey(String propEncKeyPath) throws IOException, ClassNotFoundException {
        String propEncKey = null;
        File propEncKeyFile = new File(propEncKeyPath);
    
        // get prop encryption from file
        if (propEncKeyFile.exists()) {
            ObjectInputStream propKeyOIS = null;
            try {
                propKeyOIS = new ObjectInputStream(new FileInputStream(propEncKeyFile));
            } finally {
                if (propKeyOIS!=null) {propKeyOIS.close();}
            }
            propEncKey = (String) propKeyOIS.readObject();
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
    
    
    
    public static void main(String[] args) {
        File f = new File("/work/agent.properties");
        InputStreamReader r=null;
        OutputStreamWriter w=null;
        try {
//             r = new InputStreamReader(new FileInputStream(f), "8859_1");
//             w = new OutputStreamWriter(new FileOutputStream(f), "8859_1");

            RandomAccessFile ra = new RandomAccessFile(f,"rw");
//            CharBuffer t = CharBuffer.allocate(10);
//            for(int i=0;i!=-1;i=r.read(t)) {
                for (int j=0;j<1000;j++) {
//                    if (t.get(j)=='#') {
                        ra.writeBytes("a");
//                        break;
//                    }
//                }
//                t.rewind();
            }
            
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(r!=null) {r.close();}
                if(w!=null) {w.close();}
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
