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

/**
 * An ant task that converts a relative path (file or directory)
 * to an absolute path.  The path to convert is specified with
 * the "path" attribute".  The converted absolute path is 
 * stored in the property named by the "property" attribute.
 */
public class AbsolutePath extends Task {

    private String itsPropName;
    private String itsPath;
    private File itsPwd = null;
    private boolean itsFailOnMissing = true;

    public AbsolutePath () {}

    public void setProperty ( String propName ) {
        itsPropName = propName;
    }

    public void setPath ( String path ) {
        itsPath = StringUtil.normalizePath(path);
    }

    public void setPwd ( File pwd ) {
        itsPwd = pwd;
    }

    public void setFailOnMissing ( boolean failOnMissing ) {
        itsFailOnMissing = failOnMissing;
    }

    public void execute () throws BuildException {

        validateAttributes();

        Project project = null;
        File aPath = null;
        String absPath;

        aPath = new File(itsPath);
        if (itsPwd != null) {
            aPath = relativeToAbsolute(aPath, itsPwd);
        }

        if ( !aPath.exists() ) {
            if ( itsFailOnMissing ) {
                throw new BuildException("Path does not exist: " + itsPath);
            } else {
                absPath = itsPath;
            }
        } else {
            absPath = aPath.getAbsolutePath();
        }

        project = getOwningTarget().getProject();
        project.setNewProperty(itsPropName, absPath);
    }

    /**
     * Adjust a path that is relative to the current directory to include the
     * full path to the current directory, so that it becomes an absolute path.
     * For example:
     *   adjustRelativeToPwd("./foo", "/tmp") --> /tmp/foo
     *   adjustRelativeToPwd("foo\bar", "C:\") --> C:\foo\bar
     */
    private static File relativeToAbsolute(File path, File pwd) {
        // don't do anything with paths that are already absolute
        if (path.isAbsolute()) return path;
        return new File(pwd, path.getPath());
    }

    private void validateAttributes () throws BuildException {
        if ( itsPropName == null ) 
            throw new BuildException("AbsolutePath: No 'property' attribute specified.");
        if ( itsPath == null ) 
            throw new BuildException("AbsolutePath: No 'path' attribute specified.");
    }
}
