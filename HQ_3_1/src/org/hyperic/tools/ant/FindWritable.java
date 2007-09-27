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

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;

import org.hyperic.util.StringUtil;
import org.hyperic.util.file.FileUtil;
import org.hyperic.util.file.WritableFile;

/**
 * An ant task that find a place to write a file.
 */
public class FindWritable extends Task {

    private String itsPropName;
    private String itsPreferredDir;
    private String itsFilename;
    private String itsAltDirPrefix;
    private String itsMessage;

    public FindWritable () {}

    public void setProperty ( String propName ) {
        itsPropName = propName;
    }

    public void setPreferredDir ( String path ) {
        itsPreferredDir = StringUtil.normalizePath(path);
    }

    public void setFilename ( String filename ) {
        itsFilename = StringUtil.normalizePath(filename);
    }

    public void setAltDirPrefix ( String altdirprefix ) {
        itsAltDirPrefix = altdirprefix;
    }

    public void addText ( String msg ) {
        if (itsMessage == null) itsMessage = msg;
        else itsMessage += msg;
    }

    public void execute () throws BuildException {

        validateAttributes();

        Project project = null;
        WritableFile file = FileUtil.findWritableFile(new File(itsPreferredDir),
                                                      itsFilename,
                                                      null,
                                                      itsAltDirPrefix);
        if (file == null) {
            throw new BuildException("No writable location could be found to "
                                     + "write file: " + itsFilename);
        }
        String absPath = file.getAbsolutePath();

        if (!file.mkdirs()) {
            throw new BuildException("Error creating necessary directories "
                                     + "to write file: " + absPath);
        }
        project = getOwningTarget().getProject();
        project.setNewProperty(itsPropName, absPath);

        if (itsMessage != null && !file.getOriginalLocationWasUsed()) {
            File original = new File(itsPreferredDir, itsFilename);
            itsMessage = StringUtil.replace(itsMessage, 
                                            "%ORIGINALFILE%",
                                            original.getAbsolutePath());
            itsMessage = StringUtil.replace(itsMessage, 
                                            "%WRITABLEFILE%",
                                            absPath);
            project.log(itsMessage);
        }
    }

    private void validateAttributes () throws BuildException {
        if ( itsPropName == null ) 
            throw new BuildException("FindWritable: No 'property' attribute specified.");
        if ( itsPreferredDir == null ) 
            throw new BuildException("FindWritable: No 'preferredDir' attribute specified.");
        if ( itsFilename == null ) 
            throw new BuildException("FindWritable: No 'filename' attribute specified.");
    }
}
