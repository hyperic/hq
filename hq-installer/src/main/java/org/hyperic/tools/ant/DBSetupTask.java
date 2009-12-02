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
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Environment;
import java.util.ArrayList;
import java.util.Iterator;

import org.hyperic.tools.db.DBSetup;

public class DBSetupTask extends Task {

    private File   xmlFile      = null;
    private String jdbcUrl      = null;
    private String jdbcUser     = null;
    private String jdbcPassword = null;

    private File    typeMap   = null;
    private boolean verbose   = false;
    private boolean quiet     = false;
    private boolean data      = false;
    private boolean uninstall = false;
    private boolean noexec    = false;
    private boolean create    = false;
    private String  sqlFile   = null;
    private boolean appendToSqlFile = false;
    private ArrayList sprops = new ArrayList();

    // These are used when "data" is true, to only
    // setup a single table
    private String table = null;
    private boolean doDelete = false;

    public DBSetupTask () {}

    public void setXmlFile ( File xmlFile ) {
        this.xmlFile = xmlFile;
    }

    public void setJdbcUrl ( String jdbcUrl ) {
        this.jdbcUrl = jdbcUrl;
    }

    public void setJdbcUser ( String jdbcUser ) {
        this.jdbcUser = jdbcUser;
    }

    public void setJdbcPassword ( String jdbcPassword ) {
        this.jdbcPassword = jdbcPassword;
    }

    public void setTypeMap ( File typeMap ) {
        this.typeMap = typeMap;
    }

    public void setVerbose ( boolean verbose ) {
        this.verbose = verbose;
    }

    public void setQuiet ( boolean quiet ) {
        this.quiet = quiet;
    }

    public void setData ( boolean data ) {
        this.data = data;
    }

    public void setUninstall ( boolean uninstall ) {
        this.uninstall = uninstall;
    }

    // this makes DBSetup connect to the database and walk
    // the schema, creating <table>'s with <data> elements
    public void setCreate ( boolean create ) {
        this.create = create;
    }

    public void setNoexec ( boolean noexec ) {
        this.noexec = noexec;
    }

    public void setSqlFile ( String sqlFile ) {
        if ( sqlFile == null
             || sqlFile.length() == 0
             || sqlFile.equals("NONE")
             || (sqlFile.startsWith("${") && sqlFile.endsWith("}")) ) {
            return;
        }
        this.sqlFile = sqlFile;
    }

    public void setAppendToSqlFile ( boolean appendToSqlFile ) {
        this.appendToSqlFile = appendToSqlFile;
    }

    public void setTable ( String table ) {
        this.table = table;
    }

    public void setDelete ( boolean del ) {
        this.doDelete = del;
    }

    /**
     * Support subelements to set System properties i.e.
     * &lt;sysproperty key="foo" value="bar" /&gt;
     */
    public void addSysproperty(Environment.Variable sprop) {
        sprops.add(sprop);
    }

    public void execute () throws BuildException {

        validateAttributes();
        for (Iterator i = sprops.iterator(); i.hasNext();) {
            Environment.Variable env = (Environment.Variable) i.next();
            System.setProperty(env.getKey(), env.getValue());
        }
        try {
            DBSetup dbs = new DBSetup(quiet, verbose, true,
                                      sqlFile,
                                      appendToSqlFile,
                                      noexec);

            DBSetup.m_bDMLonly = data; // XXX HACK, there is no other way to set this

            if ( typeMap != null ) dbs.setTypeMapFile(typeMap.getAbsolutePath());

            if ( uninstall ) {
                dbs.uninstall(xmlFile.getAbsolutePath(),
                              jdbcUrl, jdbcUser, jdbcPassword);
            } else if ( create ) {

                dbs.create(xmlFile.getAbsolutePath(),
                                jdbcUrl, jdbcUser, jdbcPassword);

            } else if ( table == null ) {
                dbs.setup(xmlFile.getAbsolutePath(),
                          jdbcUrl, jdbcUser, jdbcPassword);
            } else {
                dbs.setup(xmlFile.getAbsolutePath(),
                          jdbcUrl, jdbcUser, jdbcPassword,
                          table, doDelete);
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            throw new BuildException("DBSetup failed: " + e.toString(), e);
        }
    }

    private void validateAttributes () throws BuildException {
        if ( xmlFile == null )
            throw new BuildException("DBSetup: No 'xmlFile' attribute specified.");
        if ( jdbcUrl == null )
            throw new BuildException("DBSetup: No 'jdbcUrl' attribute specified.");
    }
}
