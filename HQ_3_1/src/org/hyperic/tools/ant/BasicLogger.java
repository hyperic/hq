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
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * For stupid programs like CruiseControl that call ant
 * directly and swallow all the output, this task
 * allows the build itself to start listening for its
 * own events.
 */
public class BasicLogger extends Task implements BuildListener {

    private PrintStream itsOutStream;
    private File itsOutFile;

    private String itsLevelName;
    private int itsLevel;

    private BuildLogger itsLogger; 

    public BasicLogger () {
        // For some weird reason we need to do these calls to 
        // prevent a ClassCircularityError later on...
        StringBuffer sb = new StringBuffer();
        System.currentTimeMillis();
    }
    
    public void setFile ( File outFile ) {
        itsOutFile = outFile;
    }

    public void setLevel ( String levelName ) {
        itsLevelName = levelName;
    }

    public void execute () throws BuildException {
        validateAttributes();
        getOwningTarget().getProject().addBuildListener(this);
    }

    public void register (Project p) throws BuildException {
        validateAttributes();
        p.addBuildListener(this);
    }

    public void buildFinished (BuildEvent be) {
        // itsOutStream.println("buildFinished");
        itsLogger.buildFinished(be);
    }
    public void taskFinished (BuildEvent be) {
        // itsOutStream.println("taskFinished");
        itsLogger.taskFinished(be);
    }
    public void targetFinished (BuildEvent be) {
        // itsOutStream.println("targetFinished");
        itsLogger.targetFinished(be);
    }
    public void buildStarted (BuildEvent be) {
        // itsOutStream.println("buildStarted");
        itsLogger.buildStarted(be);
    }
    public void taskStarted (BuildEvent be) {
        // itsOutStream.println("taskStarted");
        itsLogger.taskStarted(be);
    }
    public void targetStarted (BuildEvent be) {
        // itsOutStream.println("targetStarted");
        itsLogger.targetStarted(be);
    }
    public void messageLogged (BuildEvent be) {
        // itsOutStream.println("messageLogged");
        itsLogger.messageLogged(be);
    }

    private void validateAttributes () throws BuildException {

        if ( itsOutFile == null ) {
            throw new BuildException("BasicLogger: No 'file' attribute specified.");
        }
        try {
            itsOutStream = new PrintStream(new FileOutputStream(itsOutFile), true);
        } catch ( Exception e ) {
            throw new BuildException("BasicLogger: couldn't open stream "
                                     + "to file", e);
        }

        if ( itsLevelName == null ) {
            itsLevel = Project.MSG_VERBOSE;

        } else if ( itsLevelName.equalsIgnoreCase("error") ) {
            itsLevel = Project.MSG_ERR;

        } else if ( itsLevelName.equalsIgnoreCase("warn") ) {
            itsLevel = Project.MSG_WARN;

        } else if ( itsLevelName.equalsIgnoreCase("info") ) {
            itsLevel = Project.MSG_INFO;

        } else if ( itsLevelName.equalsIgnoreCase("verbose") ) {
            itsLevel = Project.MSG_VERBOSE;

        } else if ( itsLevelName.equalsIgnoreCase("DEBUG") ) {
            itsLevel = Project.MSG_DEBUG;

        } else {
            throw new BuildException("BasicLogger: Illegal value for 'level' attribute: " 
                                     + itsLevelName);
        }

        itsLogger = new DefaultLogger();
        itsLogger.setErrorPrintStream(itsOutStream);
        itsLogger.setOutputPrintStream(itsOutStream);
        itsLogger.setMessageOutputLevel(itsLevel);
    }
}
