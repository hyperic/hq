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

package org.hyperic.tools.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import org.hyperic.util.file.FileUtil;

/**
 * Prepend one file to another.
 */
public class Prepend extends Task {

    private File   itsSrcFile;
    private File   itsDestFile;

    public Prepend () {}

    public void setSrcFile ( File srcFile ) {
        itsSrcFile = srcFile;
    }

    public void setDestFile ( File destFile ) {
        itsDestFile = destFile;
    }

    public void execute () throws BuildException {

        validateAttributes();

        File tmpFile = null;
        FileOutputStream outStream = null;
        FileInputStream srcFileInStream = null;
        FileInputStream destFileInStream = null;
        try {
            if ( itsDestFile.exists() ) {
                tmpFile = File.createTempFile("ant", "prepend-task");
                outStream = new FileOutputStream(tmpFile);
                srcFileInStream = new FileInputStream(itsSrcFile);
                destFileInStream = new FileInputStream(itsDestFile);

                // Dump both files to temp file
                FileUtil.copyStream(srcFileInStream, outStream);
                FileUtil.copyStream(destFileInStream, outStream);
                outStream.flush();

                srcFileInStream.close();
                destFileInStream.close();
                outStream.close();
                
                // Move temp file over dest file
                if ( !itsDestFile.delete() ) {
                    throw new BuildException("Prepend: Error removing dest file '"
                                             + itsDestFile.getAbsolutePath()
                                             + "' (we remove dest before moving "
                                             + " the merged tempfile over it)");
                }

                outStream = new FileOutputStream(itsDestFile);
                srcFileInStream = new FileInputStream(tmpFile);
                FileUtil.copyStream(srcFileInStream, outStream);

            } else {
                // No dest file, so just copy src to dest
                outStream = new FileOutputStream(itsDestFile);
                srcFileInStream = new FileInputStream(itsSrcFile);

                FileUtil.copyStream(srcFileInStream, outStream);
                outStream.flush();
            }

        } catch ( Exception e ) {
            throw new BuildException("Prepend: unexpected exception: " + e.toString(), e);

        } finally {
            try { if ( outStream != null ) outStream.close(); }
            catch ( Exception e ) {}

            try { if ( srcFileInStream != null ) srcFileInStream.close(); }
            catch ( Exception e ) {}

            try { if ( destFileInStream != null ) destFileInStream.close(); }
            catch ( Exception e ) {}
        }
    }

    private void validateAttributes () throws BuildException {
        if ( itsSrcFile == null ) 
            throw new BuildException("Prepend: No 'srcFile' attribute specified");
        if ( itsDestFile == null ) 
            throw new BuildException("Prepend: No 'destFile' attribute specified");
        if ( !itsSrcFile.exists() )
            throw new BuildException("Prepend: srcFile '"
                                     + itsSrcFile.getAbsolutePath() 
                                     + "' does not exist");
    }
}
