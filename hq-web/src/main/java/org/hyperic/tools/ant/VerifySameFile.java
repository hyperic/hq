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
import java.io.IOException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.hyperic.util.file.FileComparator;

public class VerifySameFile extends Task {

    private File itsFile1;
    private File itsFile2;

    public VerifySameFile () {}

    public void setFile1 ( File file1 ) {
        itsFile1 = file1;
    }

    public void setFile2 ( File file2 ) {
        itsFile2 = file2;
    }

    public void execute () throws BuildException {

        validateAttributes();

        String path1 = itsFile1.getAbsolutePath();
        String path2 = itsFile2.getAbsolutePath(); 

        // Verify that files exist
        if ( !itsFile1.exists() ) {
            throw new BuildException("VerifySameFile: file does not exist: " + path1);
        }
        if ( !itsFile2.exists() ) {
            throw new BuildException("VerifySameFile: file does not exist: " + path2);
        }

        // Verify that files are the same size
        if ( itsFile1.length() != itsFile2.length () ) {
            throw new BuildException("VerifySameFile: file sizes differ between " + path1 + " and " + path2);
        }

        // Check that file contents are the same
        try {
            if ( ! (new FileComparator()).compare(itsFile1, itsFile2) ) {
                throw new BuildException("VerifySameFile: file contents differ between " + path1 + " and " + path2);
            }
        } catch ( IOException e ) {
            e.printStackTrace();
            throw new BuildException("VerifySameFile: error comparing files", e);
        }
    }

    private void validateAttributes () throws BuildException {
        if ( itsFile1 == null ) 
            throw new BuildException("VerifySameFile: No 'file1' attribute specified.");
        if ( itsFile2 == null ) 
            throw new BuildException("VerifySameFile: No 'file2' attribute specified.");
    }
}
