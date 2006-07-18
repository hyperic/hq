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
import org.apache.tools.ant.types.Environment;
import java.util.ArrayList;
import java.util.Iterator;

import org.hyperic.tools.db.DBPing;

public class DBPingTask extends Task {

    private String jdbcUrl      = null;
    private String jdbcUser     = null;
    private String jdbcPassword = null;
    private boolean quiet     = false;

    public DBPingTask () {}

    public void setJdbcUrl ( String jdbcUrl ) {
        this.jdbcUrl = jdbcUrl;
    }

    public void setJdbcUser ( String jdbcUser ) {
        this.jdbcUser = jdbcUser;
    }

    public void setJdbcPassword ( String jdbcPassword ) {
        this.jdbcPassword = jdbcPassword;
    }

    public void setQuiet ( boolean quiet ) {
        this.quiet = quiet;
    }

    public void execute () throws BuildException {

        // There really is no validation that we can do.  If the jdbc string
        // starts with ${, then the property that was specified doesn't exist
        // in the .properties file, which means that we are using an
        // embedded DB.
        if (jdbcUrl.startsWith("${")) {
            return;
        }

        try {
            DBPing dbp = new DBPing(quiet);
            
            dbp.ping(null, jdbcUrl, jdbcUser, jdbcPassword);
        
        } catch ( Exception e ) {
            e.printStackTrace();
            throw new BuildException("Could not connect to database: " +
                                     jdbcUrl);
        }
    }
}
