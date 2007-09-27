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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.apache.tools.tar.TarOutputStream;

/**
 * Update a gzipped tarfile with an updated file.
 */
public class TgzUpdater extends ArchiveUpdater {

    /**
     * @see ArchiveUpdater#update
     */
    public void update (File tgz,
                        File out,
                        String pathToReplace,
                        InputStream replacement,
                        long rsize) throws IOException {
        FileInputStream file_in = null;
        GZIPInputStream gzip_in = null;
        TarInputStream tar_in = null;

        FileOutputStream file_out = null;
        GZIPOutputStream gzip_out = null;
        TarOutputStream tar_out = null;

        boolean isOverwrite = false;
        if (out == null || out.equals(tgz)) {
            isOverwrite = true;
            out = File.createTempFile("TgzUpdater", ".tgz.tmp");
        }
        TarEntry entry;
        boolean completedOK = false;
        boolean didReplacement = false;
        byte[] buf = new byte[1024*8];

        try {
            // Setup input streams
            file_in = new FileInputStream(tgz);
            gzip_in = new GZIPInputStream(new BufferedInputStream(file_in));
            tar_in  = new TarInputStream(gzip_in);

            // Setup output streams
            file_out = new FileOutputStream(out);
            gzip_out = new GZIPOutputStream(new BufferedOutputStream(file_out));
            tar_out  = new TarOutputStream(gzip_out);
            
            // Walk through the tgz, looking for our matching entry
            entry = tar_in.getNextEntry();
            while (entry != null) {
                if (!didReplacement && 
                    matches(entry.getName(), pathToReplace)) {
                    // Update with new file
                    addFile(entry, tar_out, replacement, rsize, buf);
                    didReplacement = true;

                } else {
                    // Copy existing entry unchanged to tar_out
                    tar_out.putNextEntry(entry);
                    if (!entry.isDirectory()) {
                        FileUtil.copyStream(tar_in, tar_out, buf);
                    }
                    tar_out.closeEntry();
                }

                // Move on
                entry = tar_in.getNextEntry();
            }

            if (!didReplacement) {
                // Add a new TarEntry
                entry = new TarEntry(pathToReplace);
                addFile(entry, tar_out, replacement, rsize, buf);
            }
            file_out.flush();
            gzip_out.flush();
            tar_out.flush();
            completedOK = true;

        } finally {
            doClose(tar_in);
            doClose(gzip_in);
            doClose(file_in);
            doClose(tar_out);
            doClose(gzip_out);
            doClose(file_out);
            if ( !completedOK ) out.delete();
        }

        if (isOverwrite && !out.renameTo(tgz)) {
            throw new IOException("Error renaming " + out.getPath()
                                  + " to " + tgz.getPath());
        }
    }

    private void addFile (TarEntry entry,
                          TarOutputStream tar_out,
                          InputStream in,
                          long size,
                          byte[] buf) throws IOException {

        entry.setModTime(System.currentTimeMillis());
        entry.setSize(size);
        tar_out.putNextEntry(entry);
        FileUtil.copyStream(in, tar_out, buf);
        tar_out.closeEntry();
    }
}
