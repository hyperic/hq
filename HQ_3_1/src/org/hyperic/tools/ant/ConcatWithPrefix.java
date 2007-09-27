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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;

import org.hyperic.util.file.FileUtil;

public class ConcatWithPrefix extends Task {

    private File itsFile;
    private String itsPrefix;
    private boolean itsFailOnMissing;

    public ConcatWithPrefix () {}

    public void setPrefix ( String prefix ) {
        itsPrefix = prefix;
    }

    public void setFile ( File file ) {
        itsFile = file;
    }

    public void setFailOnMissing (boolean failOnMissing) {
        itsFailOnMissing = failOnMissing;
    }

    public void execute () throws BuildException {

        validateAttributes();

        Project project = getProject();
        FileInputStream fi = null;
        ByteArrayOutputStream bo = null;
        try {
            fi = new FileInputStream(itsFile);
            bo = new ByteArrayOutputStream();
            FileUtil.copyStream(fi, bo);
            project.log(itsPrefix + "\n" + bo.toString());

        } catch (FileNotFoundException e) {
            if (itsFailOnMissing) {
                throw new BuildException("Path not found: " + itsFile.getPath(), e);
            }
        } catch (IOException e) {
            throw new BuildException("Error reading from " + itsFile.getPath(), e);
        } finally {
            if (fi != null) try { fi.close(); } catch (IOException e) {}
        }
    }

    private void validateAttributes () throws BuildException {
        if ( itsFile == null ) 
            throw new BuildException("ConcatWithPrefix: No 'file' attribute specified.");
        if ( itsPrefix == null ) 
            throw new BuildException("ConcatWithPrefix: No 'prefix' attribute specified.");
    }
}
