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

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.FileList;

public class SimpleFileSet {

    public File baseDir;
    public String[] files;

    public SimpleFileSet (Project project, Object fileSetThingy) {
        if (fileSetThingy instanceof FileSet) {
            init(project, (FileSet) fileSetThingy);

        } else if (fileSetThingy instanceof FileList) {
            init(project, (FileList) fileSetThingy);

        } else {
            throw new IllegalArgumentException("Expected a FileSet or "
                                               + "Filelist: " + fileSetThingy);
        }
    }

    public void init (Project project, FileSet fileSet) {
                
        // Get a directory scanner from the file set, which will
        // determine the files from the set which need to be
        // concatenated.
        DirectoryScanner scanner = 
            fileSet.getDirectoryScanner(project);
                
        // Determine the root path.
        baseDir = fileSet.getDir(project);
        
        // Get the list of files.
        files = scanner.getIncludedFiles();
    }   

    public void init (Project project, FileList fileList) {
                
        // Determine the root path.
        baseDir = fileList.getDir(project);
                
        // Get the list of files.
        files = fileList.getFiles(project);
    }
}
