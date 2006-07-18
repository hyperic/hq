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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.hyperic.util.jdbc.JDBC;
import org.hyperic.util.jdbc.DBUtil;

/**
 * Sets a property if a table already exists in a database.
 */
public class CheckTableExists extends Task {

    private String jdbcUrl      = null;
    private String jdbcUser     = null;
    private String jdbcPassword = null;

    private String  property   = null;
    private String  table      = null;

    private String  ifProp     = null;
    private String  unlessProp = null;

    private long    timeoutMillis = 10000;

    public CheckTableExists () {}

    public void setJdbcUrl ( String jdbcUrl ) {
        this.jdbcUrl = jdbcUrl;
    }

    public void setJdbcUser ( String jdbcUser ) {
        this.jdbcUser = jdbcUser;
    }

    public void setJdbcPassword ( String jdbcPassword ) {
        this.jdbcPassword = jdbcPassword;
    }

    public void setProperty ( String property ) {
        this.property = property;
    }

    public void setIf ( String ifProp ) {
        this.ifProp = ifProp;
    }

    public void setUnless ( String unlessProp ) {
        this.unlessProp = unlessProp;
    }

    public void setTable ( String table ) {
        this.table = table;
    }

    public void setTimeoutMillis ( long timeout ) {
        this.timeoutMillis = timeout;
    }

    public void execute () throws BuildException {

        Project project = getOwningTarget().getProject();
        if ( ifProp != null ) {
            if ( project.getProperty(ifProp) == null ) return;
        }
        if ( unlessProp != null ) {
            if ( project.getProperty(unlessProp) != null ) return;
        }

        validateAttributes();
        TableChecker tc = new TableChecker(property, project);
        Thread t = new Thread(tc);
        t.setDaemon(true);

        // We run the checker in a separate thread in case it hangs
        // trying to connect to the DB.
        t.start();
        long start = System.currentTimeMillis();
        while ( System.currentTimeMillis() - start < timeoutMillis 
                && !tc.isDone()
                && t.isAlive()) {
            try { Thread.sleep(1000); } catch (InterruptedException ie) {
                break;
            }
        }
        if ( t.isAlive() ) {
            // Oh well, it looks like we're hung
            t.interrupt();
            try { t.join(1000); } catch (InterruptedException ie) {}
            if ( t.isAlive() ) {
                project.log("CheckTableExists: killing thread with thread.stop...", Project.MSG_WARN);
                t.stop();
            }
        }
    }

    private void validateAttributes () throws BuildException {
        if ( property == null )
            throw new BuildException("CheckTableExists: No 'property' attribute specified.");
        if ( table == null )
            throw new BuildException("CheckTableExists: No 'table' attribute specified.");
        if ( jdbcUrl == null )
            throw new BuildException("CheckTableExists: No 'jdbcUrl' attribute specified.");
    }

    class TableChecker implements Runnable {
        private volatile boolean done;
        private String property;
        private Project project;
        public TableChecker ( String property, Project p ) {
            this.property = property;
            this.project = p;
            this.done = false;
        }
        public void run () {
            Connection c = null;
            Statement s = null;
            ResultSet rs = null;
            try {
                JDBC.loadDriver(jdbcUrl);
                c = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
                s = c.createStatement();
                rs = s.executeQuery("SELECT COUNT(*) FROM " + table);
                if ( rs.next() ) {
                    project.setProperty(property, "true");
                }
                return;
                
            } catch ( Exception e ) {
                project.log("CheckTableExists: error occurred, so table "
                            + "probably does not exist: " + e, Project.MSG_WARN);
            } finally {
                DBUtil.closeJDBCObjects(this, c, s, rs);
                this.done = true;
            }
        }
        public boolean isDone () { return done; }
    }
}

