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
import java.sql.PreparedStatement;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.hyperic.util.StringUtil;
import org.hyperic.util.jdbc.DBUtil;

public class SST_AddColumn extends SchemaSpecTask {

    private String table = null;
    private String column = null;
    private String columnType = null;
    private String precision = null;
    private Initializer initializer = null;
    private ForeignKey foreignKey = null;

    public SST_AddColumn () {}

    public void setTable (String t) {
        table = t;
    }
    public void setColumn (String c) {
        column = c;
    }
    public void setColumnType (String ct) {
        columnType = ct;
    }
    public void setPrecision (String p) {
        precision = p;
    }
    public Initializer createInitializer () {
        if ( initializer != null ) {
            throw new IllegalStateException("Multiple initializers "
                                            + "not permitted");
        }
        initializer = new Initializer();
        return initializer;
    }
    public ForeignKey createForeignKey () {
        if ( foreignKey != null ) {
            throw new IllegalStateException("Multiple foreignKeys "
                                            + "not permitted");
        }
        foreignKey = new ForeignKey();
        return foreignKey;
    }

    public void execute () throws BuildException {

        validateAttributes();

        Connection c = getConnection();
        PreparedStatement ps = null;
        String columnTypeName = getDBSpecificTypeName(columnType);
        String alterSql
            = "ALTER TABLE " + table + " ADD " + column + " " + columnTypeName;
        if ( precision != null ) alterSql += "(" + precision + ")";

        try {
            // Check to see if the column exists.  If it's already there,
            // then don't re-add it.
            boolean foundColumn = DBUtil.checkColumnExists(ctx, c, 
                                                           table, column);
            if ( foundColumn ) {
                log(">>>>> Not adding column: " + column
                    + " because it already exists in table " + table);
                return;
            }

            // Add the column.
            ps = c.prepareStatement(alterSql);
            log(">>>>> Adding column " + column + " (type=" + columnTypeName + ") "
                + " to table " + table);
            ps.executeUpdate();

            // Initialize the column
            if ( initializer != null ) {
                initializer.init(c);
                initializer.execute();
            }
            if ( foreignKey != null ) {
                foreignKey.init(c);
                foreignKey.execute();
            }

        } catch ( Exception e ) {
            throw new BuildException("Error updating " + table + "." + column 
                                     + ": " + e, e);
        } finally {
            DBUtil.closeStatement(ctx, ps);
        }
        
    }

    private void validateAttributes () throws BuildException {
        if ( table == null )
            throw new BuildException("SchemaSpec: update: No 'table' attribute specified.");
        if ( column == null )
            throw new BuildException("SchemaSpec: update: No 'column' attribute specified.");
        if ( columnType == null )
            throw new BuildException("SchemaSpec: update: No 'columnType' attribute specified.");
    }

    public class Initializer extends Task {

        private String initSql = null;
        private Connection conn;

        public Initializer () {}

        public void init (Connection conn) {
            this.conn = conn;
        }

        public void addText(String msg) {
            if ( initSql == null ) initSql = "";
            initSql += project.replaceProperties(msg);
        }

        public void execute() throws BuildException {

            if ( initSql == null ) return;

            PreparedStatement ps = null;
            try {
                // Replace %%TRUE%% and %%FALSE%%
                initSql
                    = StringUtil.replace(initSql, "%%TRUE%%", 
                                         DBUtil.getBooleanValue(true, conn));
                initSql
                    = StringUtil.replace(initSql, "%%FALSE%%", 
                                         DBUtil.getBooleanValue(false, conn));
                ps = conn.prepareStatement(initSql);
                log(">>>>> Initializing " + table + "." + column 
                    + " with " + initSql);
                ps.executeUpdate();

            } catch ( Exception e ) {
                throw new BuildException("Error initializing " 
                                         + table + "." + column 
                                         + " (sql=" + initSql + ")");
            } finally {
                DBUtil.closeStatement(ctx, ps);
            }
        }
    }

    public class ForeignKey extends Task {

        private String constraintName = null;
        private String refs = null;
        private Connection conn;

        public ForeignKey () {}

        public void init (Connection conn) {
            this.conn = conn;
        }

        public void setConstraintName (String constraintName) {
            this.constraintName = constraintName;
        }
        public void setReferences (String refs) {
            this.refs = refs;
        }

        public void execute () throws BuildException {
            String fkSql
                = "ALTER TABLE " + table + " "
                + "ADD CONSTRAINT " + constraintName + " "
                + "FOREIGN KEY (" + column + ") REFERENCES " + refs;
            PreparedStatement ps = null;
            try {
                ps = conn.prepareStatement(fkSql);
                log(">>>>> Adding foreign key constraint " + constraintName 
                    + " on " + table + "." + column + "->" + refs);
                ps.executeUpdate();

            } catch ( Exception e ) {
                throw new BuildException("Error adding foreign key for "
                                         + table + "." + column 
                                         + " (sql=" + fkSql + ")");
            } finally {
                DBUtil.closeStatement(ctx, ps);
            }
        }
    }
}
