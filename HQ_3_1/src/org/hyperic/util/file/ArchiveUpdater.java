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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Common superclass for TgzUpdater and ZipUpdater
 */
public abstract class ArchiveUpdater {

    public static ZipUpdater zipUpdater = new ZipUpdater();
    public static TgzUpdater tgzUpdater = new TgzUpdater();

    public ArchiveUpdater () {}

    public static ArchiveUpdater getUpdater (File f) 
        throws FileNotFoundException {

        if (!f.exists()) {
            throw new FileNotFoundException("Archive does not exist: " 
                                            + f.getAbsolutePath());
        }
        if (f.getPath().toLowerCase().endsWith(".tgz") ||
            f.getPath().toLowerCase().endsWith(".tar.gz")) {
            return tgzUpdater;

        } else if (f.getPath().toLowerCase().endsWith(".zip") ||
                   f.getPath().toLowerCase().endsWith(".jar")) {
            return zipUpdater;

        } else {
            throw new IllegalArgumentException("Unrecognized archive: " + f);
        }
    }

    /**
     * Update an archive.  args array is interpreted as:
     *   args[0]   - the archive to update
     *   args[1]   - destination file, or "SAME" to update the original 
     *               file in place
     *   args[2]   - the path within the archive to update
     *   args[3]   - the file to use to update the archive
     */
    public static void main (String[] args) {
        if (args.length != 4) {
            System.err.println("Usage: java " 
                               + ArchiveUpdater.class.getName() 
                               + " <archive> <dest|SAME> <path> <file>");
            return;
        }
        File archive = new File(args[0]);
        File destFile = getDestFile(args[1]);
        try {
            ArchiveUpdater updater = getUpdater(archive);
            updater.update(archive, destFile, args[2], new File(args[3]));
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Update an archive.  If the pathToReplace does not exist, the file
     * will be added to the archive.
     * @param arch The archive to update
     * @param out The destination file.  If null or equal to arch, then the
     * original archive will be overwritten.
     * @param pathToReplace The path within the archive that will be replaced.
     * This may optionally begin with the String "**", in which case the first
     * file whose name matches the filename after the ** will be updated.
     * @param replacementFile The new file whose contents will be used to
     * populate the replacement path.
     */
    public void update (File arch,
                        File out,
                        String pathToReplace,
                        File replacementFile) throws IOException {
        FileInputStream fi = null;
        try {
            fi = new FileInputStream(replacementFile);
            update(arch, out, pathToReplace, fi, replacementFile.length());
        } finally {
            if (fi != null) try { fi.close(); } catch (IOException e) {}
        }
    }

    /**
     * Same as the other update method, but this accepts an InputStream as 
     * the replacement file instead of a File object.
     * @param rsize The size of the replacement stream, in bytes.
     */
    public abstract void update (File arch,
                                 File out,
                                 String pathToReplace,
                                 InputStream replacement,
                                 long rsize) throws IOException;

    protected boolean matches (String archPath, String match) {
        if (match.startsWith("**")) {
            return archPath.endsWith(match.substring(2, match.length()));
        } else {
            return archPath.equals(match);
        }
    }

    protected void doClose (InputStream is) {
        if (is != null) {
            try { is.close(); } catch (IOException e) {}
        }
    }
    protected void doClose (OutputStream os) {
        if (os != null) {
            try { os.close(); } catch (IOException e) {}
        }
    }

    public static File getDestFile (String arg) {
        if ( arg.equalsIgnoreCase("SAME") ) return null;
        return new File(arg);
    }
}
