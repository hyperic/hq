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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.FileList;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public abstract class BaseFileSetTask extends Task {

    public BaseFileSetTask () {}

    private List _fileSets = new ArrayList();

    public void addFileset(FileSet set) {
        _fileSets.add(set);
    }

    public void addFilelist(FileList list) {
        _fileSets.add(list);
    }

    protected void validateAttributes () throws BuildException {
        if (_fileSets.size() == 0) {
            throw new BuildException("At least one file must be provided.");
        }
    }

    protected List getAllFileSets () {
        List sets = new ArrayList();
        
        // Iterate the FileSet collection.
        for (Iterator iter = _fileSets.iterator(); iter.hasNext();) {
            // Get the next file set, which could be a FileSet or a
            // FileList instance.
            sets.add(new SimpleFileSet(project, iter.next()));
        }

        return sets;
    }

    protected List getAllFiles () {
        
        Set files = new HashSet();

        // Iterate the FileSet collection.
        for (Iterator iter = _fileSets.iterator(); iter.hasNext();) {

            // Get the next file set, which could be a FileSet or a
            // FileList instance.
            SimpleFileSet sfs = new SimpleFileSet(project, iter.next());

            // Create a list of absolute paths for the src files.
            int len = sfs.files.length;
            for (int i = 0; i < len; i++) {
                File current = new File(sfs.baseDir, sfs.files[i]);

                // Make sure the file exists. This will rarely fail when
                // using file sets, but it could be rather common when
                // using file lists.
                if (!current.exists()) {
                    // File does not exist, log a warning and continue.
                    log("File " + current + " does not exist.", 
                        Project.MSG_WARN);
                    continue;
                }

                files.add(current);
            }
        }

        // We need to return a list, not a set
        List fileList = new ArrayList();
        fileList.addAll(files);
        return fileList;
    }
}

