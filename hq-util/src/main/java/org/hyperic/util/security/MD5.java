/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2011], VMWare, Inc.
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

package org.hyperic.util.security;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.encoding.Base64;

/**
 * MD5 helper methods. 
 */
public class MD5 {
    
    private static final Log log = LogFactory.getLog(MD5.class);

    private MessageDigest md;

    public MD5() {
        try {
            this.md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not find MD5 Algorithm");
        }
    }

    public void add(InputStream is)
        throws IOException {

        byte[] bytes = new byte[1024];
        int len;
        try {
            while ((len = is.read(bytes, 0, bytes.length)) != -1) {
                md.update(bytes, 0, len);
            }
        } catch (IOException e) {
            throw new IOException("Couldn't read data stream: " +
                                  e.getMessage());
        }
    }

    public void add(String input) {
        add(input.getBytes());
    }
    
    public void add(byte[] input) {
        this.md.update(input);
    }
    
    public byte[] getDigest()
        throws IOException {

        return this.md.digest();
    }

    public String getDigestString(byte[] bytes) {
        StringBuffer sb = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            int hi = (bytes[i] >> 4) & 0xf;
            int lo = bytes[i] & 0xf;
            sb.append(Character.forDigit(hi, 16));
            sb.append(Character.forDigit(lo, 16));
        }
        return sb.toString();
    }

    public String getDigestString() throws IOException {
        return getDigestString(getDigest());
    }

    public static byte[] getDigest(String input) throws IOException {
        MD5 md5 = new MD5();
        md5.add(input);
        return md5.getDigest();
    }

    public static String getEncodedDigest(String input)
        throws IOException {

        return Base64.encode(getDigest(input));
    }
    
    public static String getDigestString(InputStream is)
        throws IOException {

        MD5 md5 = new MD5();
        md5.add(is);
        return md5.getDigestString();
    }

    // The md5 string returned should agree with the output you get from
    // md5(1) on bsd's and md5sum on linux
    /**
     * @deprecated use getMD5Checksum(File file), doesn't always agree with output from md5sum
     */
    public static String getDigestString(File file)
        throws IOException,
               FileNotFoundException {

        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            return new String(getDigestString(is));
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException(" couldn't find file " + file +
                        ": " + e.getMessage());
        } finally {
            if (is != null) is.close();
        }
    }

    public static String getMD5Checksum(String buf) {
        ByteArrayInputStream stream = new ByteArrayInputStream(buf.getBytes());
        return getMD5Checksum(stream);
    }

    public static String getMD5Checksum(File file) {
        try {
            InputStream fin = new FileInputStream(file);
            return getMD5Checksum(fin);
        } catch (FileNotFoundException e) {
            log.error(e,e);
            return null;
        }
    }

    private static String getMD5Checksum(InputStream fin) {
        try {
            final MessageDigest md5er = MessageDigest.getInstance("MD5");
            final byte[] buffer = new byte[1024];
            int read;
            do {
                read = fin.read(buffer);
                if (read > 0) {
                    md5er.update(buffer, 0, read);
                }
            } while (read != -1);
            fin.close();
            final byte[] digest = md5er.digest();
            if (digest == null) {
                return null;
            }
            final StringBuilder strDigest = new StringBuilder();
            for (final Byte b : digest) {
                strDigest.append(
                    Integer.toString((b & 0xff) + 0x100, 16).substring(1).toLowerCase());
            }
            return strDigest.toString();
        } catch (Exception e) {
            log.error(e,e);
            return null;
        }
    }

}
