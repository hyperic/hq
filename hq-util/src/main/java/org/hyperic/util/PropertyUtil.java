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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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

    public static void removeProperties(String propFilePath, Set<String> entriesToRemove) throws IOException {
        // find entries position in the file
        FileInputStream propReader = null;
        List<PropEntry> propEntries = null;
        try {
            propReader = new FileInputStream(propFilePath);
            propEntries = findEntriesPosition(new LineReader(propReader),entriesToRemove);
        } finally {
            if (propReader!=null) {propReader.close();}
        }
        // write-over/append entries to file
        RandomAccessFile propWriter = null;
        try {
            propWriter = new RandomAccessFile(propFilePath,"w");
            removeProperties(propWriter, propEntries);
        } finally {
            if (propWriter!=null) {propWriter.close();}
        }
    }
    
    public static void appendProperties(String propFilePath, Map<String,String> entriesToStore) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(propFilePath), "8859_1"));
        for (Iterator<String> e = entriesToStore.keySet().iterator(); e.hasNext();) {
            String key = e.next();
            String val = entriesToStore.get(key);
            key = convert(key, true);
            val = convert(val, false);
            bw.append(key + "=" + val);
            bw.newLine();
        }
        bw.flush();
    }
    
    /**
     * Appends the input entries at the end of the property file.
     * Any entries with identical keys would be erased.
     * 
     * @param propFilePath
     * @param entriesToStore
     * @throws IOException 
     */
    public static void storeProperties(String propFilePath, Map<String,String> entriesToStore) throws IOException {
        // erase entries with identical keys from the property file
        removeProperties(propFilePath,entriesToStore.keySet());
        // append the new entries at the end of the property file
        appendProperties(propFilePath,entriesToStore);
    }

    private static void removeProperties(RandomAccessFile propWriter ,List<PropEntry> propEntries) throws IOException {
        for (PropEntry propEntry : propEntries) {
            propWriter.seek(propEntry.getPosition());
            byte[] erasure = new byte[propEntry.getLength()];
            propWriter.write(erasure);
        }
    }

    private static class PropEntry {
        private final long position;
        private final int length;

        public PropEntry(long position, int length) {
            this.position = position;
            this.length = length;
        }

        public long getPosition() {
            return this.position;
        }

        public int getLength() {
            return this.length;
        }
    }

    private static List<PropEntry> findEntriesPosition (LineReader lr, Set<String> keys) throws IOException {
        char[] convtBuf = new char[1024];
        int limit;
        int keyLen;
        int valueStart;
        char c;
        boolean hasSep;
        boolean precedingBackslash;
        List<PropEntry> entriesToErase = null;
        
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
            if (keys.contains(key)) {
                if (entriesToErase==null) {
                    entriesToErase = new ArrayList<PropEntry>();
                }
                entriesToErase.add(new PropEntry(lr.getStartPosInFile(),limit));
            }
        }
        return entriesToErase;
    }

    /* Read in a "logical line" from an InputStream/Reader, skip all comment
     * and blank lines and filter out those leading whitespace characters 
     * (\u0020, \u0009 and \u000c) from the beginning of a "natural line". 
     * Method returns the char length of the "logical line" and stores 
     * the line in "lineBuf". 
     */
    static class LineReader {
        public LineReader(FileInputStream inStream) {
            this.inStream = inStream.getChannel();
            inByteBuf = ByteBuffer.allocate(8192); 
        }

        ByteBuffer inByteBuf;
        char[] lineBuf = new char[1024];
        int inLimit = 0;
        int inOff = 0;
        FileChannel inStream;
        
        long getStartPosInFile() throws IOException {
            return inStream.position()-inLimit;
        }
        
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
                    inLimit = inStream.read(inByteBuf);
                    inOff = 0;
                    if (inLimit <= 0) {
                        if (len == 0 || isCommentLine) { 
                            return -1; 
                        }
                        return len;
                    }
                }     
                //The line below is equivalent to calling a 
                //ISO8859-1 decoder.
                c = (char) (0xff & inByteBuf.get(inOff++));
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
                }
                else {
                    // reached EOL
                    if (isCommentLine || len == 0) {
                        isCommentLine = false;
                        isNewLine = true;
                        skipWhiteSpace = true;
                        len = 0;
                        continue;
                    }
                    if (inOff >= inLimit) {
                        inLimit = inStream.read(inByteBuf);
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

    private static String convert(String theString,
            boolean escapeSpace) {
        int len = theString.length();
        int bufLen = len * 2;
        if (bufLen < 0) {
            bufLen = Integer.MAX_VALUE;
        }
        StringBuffer outBuffer = new StringBuffer(bufLen);

        for(int x=0; x<len; x++) {
            char aChar = theString.charAt(x);
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
            case '=':
            case ':':
            case '#':
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




    public static class CommentedProperties extends java.util.Properties {
        public Vector lineData = new Vector(0, 1);
        public Vector keyData = new Vector(0, 1);

        /**
         * Load properties from the specified InputStream. 
         * Overload the load method in Properties so we can keep comment and blank lines.
         * @param   inStream   The InputStream to read.
         */
        @Override
        public void load(InputStream inStream) throws IOException
        {
            // The spec says that the file must be encoded using ISO-8859-1.
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(inStream, "ISO-8859-1"));
            String line;

            while ((line = reader.readLine()) != null) {
                char c = 0;
                int pos = 0;
                // Leading whitespaces must be deleted first.
                while ( pos < line.length()
                        && Character.isWhitespace(c = line.charAt(pos))) {
                    pos++;
                }

                // If empty line or begins with a comment character, save this line
                // in lineData and save a "" in keyData.
                if (    (line.length() - pos) == 0
                        || line.charAt(pos) == '#' || line.charAt(pos) == '!') {
                    lineData.add(line);
                    keyData.add("");
                    continue;
                }

                // The characters up to the next Whitespace, ':', or '='
                // describe the key.  But look for escape sequences.
                // Try to short-circuit when there is no escape char.
                int start = pos;
                boolean needsEscape = line.indexOf('\\', pos) != -1;
                StringBuffer key = needsEscape ? new StringBuffer() : null;

                while ( pos < line.length()
                        && ! Character.isWhitespace(c = line.charAt(pos++))
                        && c != '=' && c != ':') {
                    if (needsEscape && c == '\\') {
                        if (pos == line.length()) {
                            // The line continues on the next line.  If there
                            // is no next line, just treat it as a key with an
                            // empty value.
                            line = reader.readLine();
                            if (line == null)
                                line = "";
                            pos = 0;
                            while ( pos < line.length()
                                    && Character.isWhitespace(c = line.charAt(pos)))
                                pos++;
                        } else {
                            c = line.charAt(pos++);
                            switch (c) {
                            case 'n':
                                key.append('\n');
                                break;
                            case 't':
                                key.append('\t');
                                break;
                            case 'r':
                                key.append('\r');
                                break;
                            case 'u':
                                if (pos + 4 <= line.length()) {
                                    char uni = (char) Integer.parseInt
                                            (line.substring(pos, pos + 4), 16);
                                    key.append(uni);
                                    pos += 4;
                                }   // else throw exception?
                                break;
                            default:
                                key.append(c);
                                break;
                            }
                        }
                    } else if (needsEscape)
                        key.append(c);
                }

                boolean isDelim = (c == ':' || c == '=');
                String keyString;
                if (needsEscape)
                    keyString = key.toString();
                else if (isDelim || Character.isWhitespace(c))
                    keyString = line.substring(start, pos - 1);
                else
                    keyString = line.substring(start, pos);

                while ( pos < line.length() && Character.isWhitespace(c = line.charAt(pos)))
                    pos++;

                if (! isDelim && (c == ':' || c == '=')) {
                    pos++;
                    while ( pos < line.length()
                            && Character.isWhitespace(c = line.charAt(pos)))
                        pos++;
                }

                // Short-circuit if no escape chars found.
                if (!needsEscape) {
                    put(keyString, line.substring(pos));
                    // Save a "" in lineData and save this
                    // keyString in keyData.
                    lineData.add("");
                    keyData.add(keyString);
                    continue;
                }

                // Escape char found so iterate through the rest of the line.
                StringBuffer element = new StringBuffer(line.length() - pos);
                while (pos < line.length()) {
                    c = line.charAt(pos++);
                    if (c == '\\') {
                        if (pos == line.length()) {
                            // The line continues on the next line.
                            line = reader.readLine();

                            // We might have seen a backslash at the end of
                            // the file.  The JDK ignores the backslash in
                            // this case, so we follow for compatibility.
                            if (line == null)
                                break;

                            pos = 0;
                            while ( pos < line.length()
                                    && Character.isWhitespace(c = line.charAt(pos)))
                                pos++;
                            element.ensureCapacity(line.length() - pos +
                                    element.length());
                        } else {
                            c = line.charAt(pos++);
                            switch (c) {
                            case 'n':
                                element.append('\n');
                                break;
                            case 't':
                                element.append('\t');
                                break;
                            case 'r':
                                element.append('\r');
                                break;
                            case 'u':
                                if (pos + 4 <= line.length()) {
                                    char uni = (char) Integer.parseInt
                                            (line.substring(pos, pos + 4), 16);
                                    element.append(uni);
                                    pos += 4;
                                }   // else throw exception?
                                break;
                            default:
                                element.append(c);
                                break;
                            }
                        }
                    } else
                        element.append(c);
                }
                put(keyString, element.toString());
                // Save a "" in lineData and save this
                // keyString in keyData.
                lineData.add("");
                keyData.add(keyString);
            }
        }

        /**
         * Write the properties to the specified OutputStream.
         * 
         * Overloads the store method in Properties so we can put back comment  
         * and blank lines.                                                   
         * 
         * @param out   The OutputStream to write to.
         * @param header Ignored, here for compatability w/ Properties.
         * 
         * @exception IOException
         */
        @Override
        public void store(OutputStream out, String header) throws IOException
        {
            // The spec says that the file must be encoded using ISO-8859-1.
            PrintWriter writer
            = new PrintWriter(new OutputStreamWriter(out, "ISO-8859-1"));

            // We ignore the header, because if we prepend a commented header
            // then read it back in it is now a comment, which will be saved
            // and then when we write again we would prepend Another header...

            String line;
            String key;
            StringBuffer s = new StringBuffer ();

            for (int i=0; i<lineData.size(); i++) {
                line = (String) lineData.get(i);
                key = (String) keyData.get(i);
                if (key.length() > 0) {  // This is a 'property' line, so rebuild it
                    formatForOutput (key, s, true);
                    s.append ('=');
                    formatForOutput ((String) get(key), s, false);
                    writer.println (s);
                } else {  // was a blank or comment line, so just restore it
                    writer.println (line);
                }
            } 
            writer.flush ();
        }

        /**
         * Need this method from Properties because original code has StringBuilder,
         * which is an element of Java 1.5, used StringBuffer instead (because
         * this code was written for Java 1.4)
         * 
         * @param str   - the string to format
         * @param buffer - buffer to hold the string
         * @param key   - true if str the key is formatted, false if the value is formatted
         */
        private void formatForOutput(String str, StringBuffer buffer, boolean key)
        {
            if (key) {
                buffer.setLength(0);
                buffer.ensureCapacity(str.length());
            } else
                buffer.ensureCapacity(buffer.length() + str.length());
            boolean head = true;
            int size = str.length();
            for (int i = 0; i < size; i++) {
                char c = str.charAt(i);
                switch (c) {
                case '\n':
                    buffer.append("\\n");
                    break;
                case '\r':
                    buffer.append("\\r");
                    break;
                case '\t':
                    buffer.append("\\t");
                    break;
                case ' ':
                    buffer.append(head ? "\\ " : " ");
                    break;
                case '\\':
                case '!':
                case '#':
                case '=':
                case ':':
                    buffer.append('\\').append(c);
                    break;
                default:
                    if (c < ' ' || c > '~') {
                        String hex = Integer.toHexString(c);
                        buffer.append("\\u0000".substring(0, 6 - hex.length()));
                        buffer.append(hex);
                    } else
                        buffer.append(c);
                }
                if (c != ' ')
                    head = key;
            }
        }

        /**
         * Add a Property to the end of the CommentedProperties. 
         * 
         * @param   keyString    The Property key.
         * @param   value        The value of this Property.
         */
        public void add(String keyString, String value)
        {
            put(keyString, value);
            lineData.add("");
            keyData.add(keyString);
        }

        /**
         * Add a comment or blank line or comment to the end of the CommentedProperties. 
         * 
         * @param   line The string to add to the end, make sure this is a comment
         *             or a 'whitespace' line.
         */
        public void addLine(String line)
        {
            lineData.add(line);
            keyData.add("");
        }
    }









    public static void main(String[] args) {
        File f = new File("/work/agent-4.7-ee/conf/agent.properties");
        CommentedProperties p = new CommentedProperties();
        FileInputStream fi = null;
        try {
            fi = new FileInputStream(f);
            p.load(fi);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (fi != null)
                try {
                    fi.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
        }
        p.add("agent.keystore.path", "wwww");
        p.add("sigar.mirror.procnet", "rrrrrrrrrrrr");
        FileOutputStream fo = null;
        try {
            fo = new FileOutputStream(f);
            p.store(fo,"");
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (fi != null)
                try {
                    fi.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
        }



        //        InputStreamReader r=null;
        //        OutputStreamWriter w=null;
        //        try {
        ////             r = new InputStreamReader(new FileInputStream(f), "8859_1");
        ////             w = new OutputStreamWriter(new FileOutputStream(f), "8859_1");
        //
        //            RandomAccessFile ra = new RandomAccessFile(f,"rw");
        ////            CharBuffer t = CharBuffer.allocate(10);
        ////            for(int i=0;i!=-1;i=r.read(t)) {
        //                for (int j=0;j<1000;j++) {
        ////                    if (t.get(j)=='#') {
        //                        ra.writeBytes("a");
        ////                        break;
        ////                    }
        ////                }
        ////                t.rewind();
        //            }
        //            
        //            
        //        } catch (FileNotFoundException e) {
        //            e.printStackTrace();
        //        } catch (IOException e) {
        //            e.printStackTrace();
        //        } catch (Exception e) {
        //            e.printStackTrace();
        //        } finally {
        //            try {
        //                if(r!=null) {r.close();}
        //                if(w!=null) {w.close();}
        //            } catch (IOException e) {
        //                e.printStackTrace();
        //            }
        //        }
    }
}
