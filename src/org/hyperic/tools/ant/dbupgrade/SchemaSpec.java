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

package org.hyperic.tools.ant.dbupgrade;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.UnknownElement;

public class SchemaSpec extends Task implements TaskContainer, Comparable {

    private DBUpgrader owner = null;
    private String versionString = null;
    private SchemaVersion version = null;
    private List schemaSpecTasks = new ArrayList();
    private Connection conn = null;
    private DBUpgrader upgrader = null;

    public SchemaSpec(DBUpgrader parent) {
        owner = parent;
    }

    public void setVersion (String v) {
        versionString = v;
    }
    public SchemaVersion getVersion () throws BuildException { 
        if (version == null ) {
            if ( versionString == null )
                throw new BuildException("SchemaSpec: No 'version' attribute "
                                         + "specified.");
            try {
                version = new SchemaVersion(versionString);
            } catch (IllegalArgumentException e) {
                throw new BuildException("SchemaSpec: " + e.getMessage(), e);
            }
        }
        return version;
    }

    public void initialize ( Connection conn,
                             DBUpgrader upgrader ) {
        this.conn = conn;
        this.upgrader = upgrader;
    }

    public void addTask (Task t) {

        if (t instanceof SchemaSpecTask) {
            schemaSpecTasks.add(t);
        } else if (t instanceof UnknownElement) {
            ((UnknownElement)t).maybeConfigure();
            t = ((UnknownElement)t).getTask();

            if (t != null && t instanceof SchemaSpecTask) {
                schemaSpecTasks.add(t);
            } else {
                throw new BuildException("SchemaSpec: task "
                                         + "'" + t.getTaskName() + "'"
                                         + " is not a SchemaSpecTask");
            }
        }
    }

    public void execute () throws BuildException {

        validateAttributes();

        Project p = getProject();
        try {
            int size = schemaSpecTasks.size();
            SchemaSpecTask sst;
            for ( int i=0; i<size; i++ ) {
                sst = (SchemaSpecTask) schemaSpecTasks.get(i);
                sst.initialize(conn, upgrader);
                try {
                    sst.execute();
                } catch ( Exception e ) {
                    throw new BuildException("Error running SchemaSpecTask: "
                                             + sst.getClass().getName() 
                                             + ": " + e, e);
                }
            }

        } catch ( BuildException be ) {
            throw be;

        } catch ( Exception e ) {
            throw new BuildException("Error running SchemaSpec: "
                                     + getVersion() + ": " + e, e);
        }
    }

    private void validateAttributes () throws BuildException {
        getVersion();
    }

    public String toString() {
        return "SchemaSpec[" + getVersion() + "]";
    }

    public int compareTo(Object o) {
        if (o instanceof SchemaSpec) {
            SchemaSpec ss = (SchemaSpec) o;
            return getVersion().compareTo(ss.getVersion());
        }
        throw new IllegalArgumentException("Cannot compare "
                                           + "non-SchemaSpec object.");
    }
}
