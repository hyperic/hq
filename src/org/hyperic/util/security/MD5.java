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

package org.hyperic.util.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.Enumeration;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.hyperic.util.encoding.Base64;

/**
 * MD5 helper methods. 
 */
public class MD5 {

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

    public void add(File file)
        throws IOException {

        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            add(is);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {}
            }
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

    public String getDigestString()
        throws IOException {

        return getDigestString(getDigest());
    }

    /* static helpers follow */
    public static byte[] getDigest(InputStream is)
        throws IOException {

        MD5 md5 = new MD5();
        md5.add(is);
        return md5.getDigest();
    }

    public static byte[] getDigest(String input)
        throws IOException {

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

    public static MD5 getJarDigest(String file)
        throws IOException {

        JarFile jar = new JarFile(file, false);
        MD5 md5 = new MD5();

        try {
            for (Enumeration e = jar.entries(); e.hasMoreElements();) {
                JarEntry entry = (JarEntry)e.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                //manifest contains jdk version string
                if (entry.getName().equals(JarFile.MANIFEST_NAME)) {
                    continue;
                }
                md5.add(jar.getInputStream(entry));
            }
        } finally {
            jar.close();
        }

        return md5;
    }

    public MessageDigest getMessageDigest() {
        return this.md;
    }

    /**
     * Returns an MD5 Digest string calculated from file entries within
     * the jar file rather than the jar file itself.
     */
    public static String getJarDigestString(String file)
        throws IOException {

        return getJarDigest(file).getDigestString();
    }

    public static void main(String[] args) {
        String file = args[0];
        String digest, jarDigest=null;
        boolean isJar = file.endsWith(".jar");

        try {
            digest = MD5.getDigestString(new File(file));
            if (isJar) {
                jarDigest = MD5.getJarDigestString(file);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        System.out.println("File digest=" + digest);
        if (isJar) {
            System.out.println(" Jar digest=" + jarDigest);
        }
    }

}
