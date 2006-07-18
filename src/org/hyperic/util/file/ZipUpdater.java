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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

import java.util.zip.Deflater;
import java.util.zip.ZipInputStream;
import org.apache.tools.zip.ZipOutputStream;


/**
 * Update a zipfile with an updated file.
 */
public class ZipUpdater extends ArchiveUpdater {

    public ZipUpdater () {}

    /**
     * @see ArchiveUpdater#update
     */
    public void update (File zip,
                        File out,
                        String pathToReplace,
                        InputStream replacement,
                        long rsize) throws IOException {
        FileInputStream file_in = null;
        ZipInputStream zip_in = null;

        FileOutputStream file_out = null;
        ZipOutputStream zip_out = null;

        boolean isOverwrite = false;
        if (out == null || out.equals(zip)) {
            isOverwrite = true;
            out = File.createTempFile("ZipUpdater", ".zip.tmp");
        }
        java.util.zip.ZipEntry inEntry;
        org.apache.tools.zip.ZipEntry outEntry;
        boolean completedOK = false;
        boolean didReplacement = false;
        byte[] buf = new byte[1024*8];

        try {
            // Setup input streams
            file_in = new FileInputStream(zip);
            zip_in  = new ZipInputStream(new BufferedInputStream(file_in));

            // Setup output streams
            file_out = new FileOutputStream(out);
            zip_out = new ZipOutputStream(new BufferedOutputStream(file_out));
            zip_out.setLevel(Deflater.BEST_COMPRESSION);
            zip_out.setMethod(Deflater.DEFLATED);

            // Walk through the zip, looking for our matching entry
            inEntry = zip_in.getNextEntry();
            while (inEntry != null) {
                outEntry = new org.apache.tools.zip.ZipEntry(inEntry);
                outEntry.setMethod(Deflater.DEFLATED);
                if (!didReplacement && 
                    matches(outEntry.getName(), pathToReplace)) {
                    // Update with new file
                    addFile(outEntry, zip_out, replacement, rsize, buf);
                    didReplacement = true;

                } else {
                    // Copy existing entry unchanged to zip_out
                    zip_out.putNextEntry(outEntry);
                    FileUtil.copyStream(zip_in, zip_out, buf);
                    zip_out.closeEntry();
                }

                // Move on
                inEntry = zip_in.getNextEntry();
            }

            if (!didReplacement) {
                // Add a new ZipEntry
                outEntry = new org.apache.tools.zip.ZipEntry(pathToReplace);
                addFile(outEntry, zip_out, replacement, rsize, buf);
            }
            file_out.flush();
            zip_out.flush();
            completedOK = true;

        } finally {
            doClose(zip_in);
            doClose(file_in);
            doClose(zip_out);
            doClose(file_out);
            if ( !completedOK ) out.delete();
        }

        if (isOverwrite && !out.renameTo(zip)) {
            throw new IOException("Error renaming " + out.getPath()
                                  + " to " + zip.getPath());
        }
    }

    private void addFile (org.apache.tools.zip.ZipEntry entry,
                          ZipOutputStream zip_out,
                          InputStream in,
                          long size,
                          byte[] buf) throws IOException {

        entry.setTime(System.currentTimeMillis());
        entry.setSize(size);
        zip_out.putNextEntry(entry);
        FileUtil.copyStream(in, zip_out, buf);
        zip_out.closeEntry();
    }
}
