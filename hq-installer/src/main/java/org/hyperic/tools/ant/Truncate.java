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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;

/**
 * Truncates files.
 */
public class Truncate extends BaseFileSetTask {

    public Truncate () {}
 
    private static void truncate(File f) 
        throws FileNotFoundException, IOException {

        FileOutputStream fo = null;
        try {
            fo = new FileOutputStream(f, false);
        } finally {
            if (fo != null) {
                fo.close();
            }
        }
    }

    public void execute() throws BuildException {

        validateAttributes();

        List filesToTruncate = getAllFiles();

        File currentFile;
        for (int i=0; i<filesToTruncate.size(); i++) {
            currentFile = (File) filesToTruncate.get(i);
            try {
                truncate(currentFile);
            } catch (FileNotFoundException e) {
                log("Could not truncate file that does not exist: " 
                            + currentFile, Project.MSG_WARN);
            } catch (IOException e) {
                log("IOException ("+e+") truncating file: " 
                    + currentFile, Project.MSG_WARN);
            }
        }
    }
}
