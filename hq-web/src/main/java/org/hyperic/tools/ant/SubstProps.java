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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;

import org.hyperic.util.StringUtil;
import org.hyperic.util.file.FileUtil;

/**
 * Reads in src, substitutes all tokens of the form ${...} with
 * the values of those properties as defined in the current ant project,
 * writes the substituted text to dest.
 *
 * If no dest file is specified, we will overwrite the src file.
 */
public class SubstProps extends Task {

    private File src;
    private File dest;

    public SubstProps () {}

    public void setSrc ( File src ) {
        this.src = src;
    }
    public void setDest ( File dest ) {
        this.dest = dest;
    }

    public void execute () throws BuildException {

        validateAttributes();

        Project project = null;
        File aPath = null;
        String absPath;

        // Read in src file
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FileInputStream fi = null;
        try {
            fi = new FileInputStream(src);
            FileUtil.copyStream(fi, bos);

        } catch (IOException e) {
            throw new BuildException("Error reading src file: " + e, e);

        } finally {
            if (fi != null) try { fi.close(); } catch (Exception e) {}
        }

        // Substitute
        project = getProject();
        String subst = project.replaceProperties(bos.toString());
        ByteArrayInputStream bis = new ByteArrayInputStream(subst.getBytes());
        
        // Write to dest
        FileOutputStream fo = null;
        try {
            fo = new FileOutputStream(dest);
            FileUtil.copyStream(bis, fo);
        } catch (IOException e) {
            throw new BuildException("Error writing dest file: " + e, e);

        } finally {
            if (fo != null) try { fo.close(); } catch (Exception e) {}
        }
    }

    private void validateAttributes () throws BuildException {
        if ( src == null ) 
            throw new BuildException("SubstProps: No 'src' attribute specified.");
        if ( !src.exists() ) 
            throw new BuildException("SubstProps: src file does not exist: " 
                                     + src.getPath());

        // Dest defaults to src
        if ( dest == null ) dest = src;
    }
}
