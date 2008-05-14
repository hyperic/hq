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

package org.hyperic.util.file;

import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.hyperic.util.ArrayUtil;
import org.hyperic.util.collection.IntHashMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class FileUtil {
    private static IntHashMap invalidChars = null;

    private FileUtil(){}
    
    /**
     * Try REALLY hard to find a writable place to create a file.
     * @param preferredDir The preferred directory.  If this is not a
     * directory, then an IllegalArgumentException will be thrown.
     * If this names a directory that does not exist,
     * this method will still return the preferred path if it could be
     * created.
     * @param filename The name of the file that should be written to
     * the directory.  This filename may include some preceding directory
     * names.
     * @param alternateDirs An array of Strings indicating paths to try
     * to write the file.  If this is null, the default array contains the temp
     * directory, the user's home directory, and finally the current directory.
     * @param alternatePrefixDir If the file can't be written to the preferred
     * directory and must be written to an alternate directory, this prefix
     * directory is created first.  For example, if you wanted to write to
     * /some/dir/here (with a filename of logs/somefile) but /some/dir/here
     * was not writable, so /tmp was chosen instead, you might not want
     * the ultimate file location to be /tmp/logs/somefile.  By specifying
     * an alternatePrefixDir (for example "cam"), the file would then be 
     * written to: /tmp/cam/logs/somefile
     * NOTE: If this is null it will be ignored.
     * @return A WritableFile object that can be written to, or null if
     * nothing can be found.  A WritableFile is identical to a regular
     * File object, with the addition of a method "getOriginalLocationWasUsed"
     * which can be used to determine if the original desired file location
     * was used or if an alternate location was used.
     * @exception IllegalArgumentException if the preferredDir argument
     * is not actually a directory.
     */
    public static WritableFile findWritableFile ( File preferredDir, 
                                                  String filename,
                                                  String[] alternateDirs,
                                                  String alternatePrefixDir) 
        throws IllegalArgumentException {

        // This class assumes that filename doesn't contain a directory.
        // To ensure that we create a new File representing the requested
        // preferredDir/filename and reset preferredDir and filename so that
        // all dirs are in preferredDir and the filename is a simple
        // filename.
        File tempfile = new File(preferredDir, filename);
        preferredDir = tempfile.getParentFile();
        filename = tempfile.getName();

        if (alternateDirs == null) {
            alternateDirs = new String[] {
                System.getProperty("java.io.tmpdir"), 
                System.getProperty("user.home") + File.separator + "tmp",
                System.getProperty("user.tmp"),
                System.getProperty("user.dir")
            };
        }

        if ( !preferredDir.isDirectory() ) {
            throw new IllegalArgumentException("preferredDir is not a "
                                               + "directory: "
                                               + preferredDir);
        }

        WritableFile fileAttempt;
        alternateDirs
            = (String[]) ArrayUtil.combine(new String[] {preferredDir.getAbsolutePath()},
                                            alternateDirs);
        File dirToTest;
        for (int i=0; i<alternateDirs.length; i++) {

            if (alternateDirs[i] == null) continue;
            if (alternatePrefixDir == null || i==0) {
                fileAttempt = new WritableFile(alternateDirs[i], filename);
            } else {
                fileAttempt = new WritableFile(alternateDirs[i],
                                               alternatePrefixDir
                                               + File.separator
                                               + filename);
            }

            dirToTest = fileAttempt;
            if ( !dirToTest.isDirectory() ) {
                dirToTest = dirToTest.getParentFile();
            }
            while (dirToTest != null && !dirToTest.exists()) {
                dirToTest = dirToTest.getParentFile();
            }
            if (dirToTest != null
                && dirToTest.exists()
                && dirToTest.canWrite() ) {
                fileAttempt.setOriginalLocationWasUsed(i==0);
                return fileAttempt;
            }
        }
        return null;
    }

    /** Copy a file from one file to another */
    public static void copyFile(File inFile, File outFile)
            throws FileNotFoundException, IOException {
        
        BufferedInputStream is = null;
        BufferedOutputStream os = null;
        
        try {
            is = new BufferedInputStream(new FileInputStream(inFile));
            os = new BufferedOutputStream(new FileOutputStream(outFile));
            copyStream(is, os);            
        } finally {
            safeCloseStream(is);
            safeCloseStream(os);
        }
    }

    /** Default buffer size for copyStream method */
    public static final int BUFSIZ = 2048;

    /**
     * Copy a stream, using a buffer
     */
    public static void copyStream(InputStream is, OutputStream os) 
        throws IOException {
        copyStream(is, os, new byte[BUFSIZ]);
    }

    public static void copyStream(InputStream is, OutputStream os, 
                                  byte[] buf) throws IOException {
        int bytesRead = 0;
        while (true) {
            bytesRead = is.read(buf);
            if (bytesRead == -1) break;
            os.write(buf, 0, bytesRead);
        }
    }
    
    public static String findString(String fname, String toFind)
        throws IOException {

        StringBuffer result = null;

        BufferedReader in = new BufferedReader(new FileReader(fname));
        
        try {
            char[] data = new char[8096];
            
            int numread;
            int toFindIndex = 0;
            /* Just need to initialize this, because the compiler doesn't
             * realize that it can't be used before it is assigned a value
             */
            char lastchar = 'a';
            while ((numread = in.read(data, 0, 8096)) != -1) {
                for (int i = 0; i < numread; i++) {
                    /* If we have found the string already or if we our current
                     * character matches the current char in the target string
                     * then we just add the current character to our result
                     * string and move on.
                     */
                    if (toFindIndex >= toFind.length() ||
                        data[i] == toFind.charAt(toFindIndex)) {
                        if (result == null) {
                            result = new StringBuffer();
                        }
                        if (Character.isISOControl(data[i])) {
                            return result.toString();
                        }
                        result.append(data[i]);
                        toFindIndex++;
                    } else {
                        /* Otherwise things can get complex.  If we haven't
                         * started to match, then just keep going.  If we have
                         * started to match, then we need to move backwards
                         * to make sure we don't miss a match.  For example:
                         * looking for HI in HHI.  If the current character
                         * isn't the same as the last character, then we aren't
                         * going to match, so null everything out and keep
                         * going.  Otherwise, decrment everything by one,
                         * because we didn't match the first character, and
                         * go through the loop on this character again.
                         */
                        if (toFindIndex > 0) {
                            if (data[i] != lastchar) {
                                result = null;
                                toFindIndex = 0;
                                continue;
                            }
                            toFindIndex--;
                            i--;
                            result.deleteCharAt(result.length() - 1);
                            continue;
                        }
                    }
                    lastchar = data[i];
                }
            }
        } catch (IOException e) {
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException e) {}
            }
        }
        if (result != null) {
            return result.toString();
        }
        return null;
    }

   /**
     * The base attribute specifies what the directory base
     * the relative path should be considered relative to.  The base
     * must be part of the absolute path specified by the path attribute.
     */
    public static String getRelativePath(File path, File base) {
        String path_abs = path.getAbsolutePath();
        String base_abs = base.getAbsolutePath();
        int idx = path_abs.indexOf(base_abs);
        if ( idx == -1 ) {
            throw new IllegalArgumentException("Path (" + path_abs + ") "
                                               + "does not contain "
                                               + "base (" + base_abs + ")");
        }
        String relativePath = "." + path_abs.substring(idx + base_abs.length());
        return relativePath;
    }
    
    private static void initInvalidChars() {
        if (invalidChars != null) {
            return;
        }

        invalidChars = new IntHashMap();

        char[] invalid = {
            '\\', '/', ':', '*', '?', '\'', '"', '~',
            '<', '>', '|', '#', '{', '}', '%', '&', ' '             
        };

        for (int i=0; i<invalid.length; i++) {
            invalidChars.put(invalid[i], Boolean.TRUE);
        }
    }

    /**
     * Escape invalid characters in a filename, replacing with "_"
     */
    public static String escape(String name) {
        initInvalidChars();

        int len = name.length();
        StringBuffer buf = new StringBuffer(len);
        char[] chars = name.toCharArray();

        for (int i=0; i<len; i++) {
            char c = chars[i];
            if (invalidChars.get(c) == Boolean.TRUE) {
                buf.append("_");
            }
            else {
                buf.append(c);
            }
        }

        return buf.toString();
    }

    /**
     * Test if a directory is writable
     *
     * java.io.File#canWrite() has problems on windows for properly detecting
     * if a directory is writable by the current user.  For example, 
     * C:\Program Files is set to read-only, however the Administrator user is
     * able to write to that directory
     *
     * @throws IOException If the File is not a directory
     */
    public static boolean canWrite(File dir) 
        throws IOException
    {
        if (!dir.isDirectory()) {
            throw new IOException(dir.getPath() + " is not a directory");
        }

        File tmp = null;
        try {
            tmp = File.createTempFile("hyperic", null, dir);
            return true;
        } catch (IOException e) {
            return false;
        } finally { 
            if (tmp != null) {
                tmp.delete();
            }
        }
    }

    /**
     * Chop the last element off a path.  For example, if you pass in
     * /usr/local/foo then this will return /usr/local
     * If there is not enough to chop off, this throws IllegalArgumentException
     */
    public static String getParentDir(String path) {
        int idx = path.lastIndexOf(File.separator);
        if (idx == -1 || idx == 0) {
            throw new IllegalArgumentException("Path has no parent: " + path);
        }
        return path.substring(0, idx);
    }

    /**
     * Chop the last elements off of a path.
     */
    public static String getParentDir(String path, int levels) {
        while (levels-- > 0) {
            path = getParentDir(path);
        }
    
        return path;
    }
    
    /**
     * Read all the lines from a stream into a list
     */
    public static List readLines(InputStream is)
        throws IOException
    {
        List res = new ArrayList();
        InputStreamReader isR = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isR);
        String s;
        
        while ((s = br.readLine()) != null) {
            res.add(s);
        }
        
        return res;
    }
     
    /** 
     * 
     * Deletes all files and subdirectories under dir.
     * Returns true if all deletions were successful.
     * If a deletion fails, the method stops attempting to delete and returns
     * false.
     * @param dir Directory to delete recursively
     * @return returns true iff directory was successfully deleted including all its children.
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }
    
    public static void untar(File tarFile, File destinationDir)
    throws IOException {
        TarInputStream tin = null;
        try {
            tin = new TarInputStream(new GZIPInputStream(new FileInputStream(
                    tarFile)));
            // get the first entry in the archive
            TarEntry tarEntry = tin.getNextEntry();

            while (tarEntry != null) {
                // create a file with the same name as the tarEntry
                File destPath = new File(destinationDir, tarEntry.getName());
                // create any parent directories
                File parent = destPath.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                // if entry is directory, create it
                if (tarEntry.isDirectory()) {
                    destPath.mkdirs();
                }
                else {
                    FileOutputStream fout = null;
                    try {
                        // delete the file it already exists
                        if (destPath.exists())
                            destPath.delete();
                        fout = new FileOutputStream(destPath);
                        tin.copyEntryContents(fout);
                    }
                    finally {
                        safeCloseStream(fout);
                    }
                }
                tarEntry = tin.getNextEntry();
            }
        }
        finally {
            safeCloseStream(tin);
        }
    }

    public static void safeCloseStream(InputStream in) 
    {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                // just swallow it
            }            
        }
    }
    
    public static void safeCloseStream(OutputStream out) 
    {
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                // just swallow it
            }            
        }
    }
 
}
