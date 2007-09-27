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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.hyperic.util.StringUtil;
import org.hyperic.util.jdbc.DBUtil;

public class SST_DirectSQL extends SchemaSpecTask {

    private VerifySchema verifySchema;
    private List statements;

    public SST_DirectSQL () {
        verifySchema = null;
        statements = null;
    }

    public void execute () throws BuildException {

        validateAttributes();

        Connection c = getConnection();

        if (verifySchema != null) {
            verifySchema.init(c);
            verifySchema.execute();
        }

        if (statements != null) {
            Iterator i = statements.iterator();
            while (i.hasNext()) {
                Statement s = (Statement)i.next();
                s.init(c);
                s.execute();
            }
        }
    }

    public VerifySchema createVerifySchema () {
        verifySchema = new VerifySchema();
        return verifySchema;
    }

    public Statement createStatement() {
        Statement statement = new Statement();
        if (statements == null) {
            statements = new ArrayList();
        }
        statements.add(statement);
        return statement;
    }

    private void validateAttributes () throws BuildException {
        if ( statements == null )
            throw new BuildException("SchemaSpec: update: No 'statement' " +
                                     "attribute specified.");
    }

    public class Statement extends Task {

        private String sqlStmt  = null;
        private Connection conn = null;
        private String desc = null;
        private String targetDB = null;
        private boolean fail = true;

        public Statement () {}

        public void init (Connection conn) {
            this.conn = conn;
        }
        public void setDesc(String s) {
            desc = s;
        }

        public String getDesc() {
            return desc;
        }

        public boolean isFail() {
            return fail;
        }

        public void setFail(boolean fail) {
            this.fail = fail;
        }

        public void setTargetDB (String t) { targetDB = t; }

        public void addText(String msg) {
            sqlStmt = msg;
        }

        public void execute() throws BuildException {
            PreparedStatement ps;
            ps = null;

            if ( sqlStmt == null ) {
                return;
            }
            
            int dbType = getDBUtilType();
            log("dbType="+dbType+", targetDB='"+targetDB+"'");
            if (targetDB != null) {
                if (targetDB.equalsIgnoreCase("oracle")) {
                    if (!DBUtil.isOracle(dbType)) {
                        log("target was oracle, but this is not oracle, returning.");
                        return;
                    }
                } else if (targetDB.equalsIgnoreCase("postgresql")) {
                    if (!DBUtil.isPostgreSQL(dbType)) {
                        log("target was postgresql, but this is not pgsql, returning.");
                        return;
                    }
                } else if (targetDB.equalsIgnoreCase("mysql")) {
                    if (!DBUtil.isMySQL(dbType)) {
                        log("target was mysql, but this is not mysql, returning.");
                        return;
                    }
                } else {
                    throw new BuildException("dbtype attribute must be 'oracle'"
                                             + " or 'postgresql'");
                }
            }

            try {
                // Replace %%TRUE%% and %%FALSE%%
                sqlStmt
                    = StringUtil.replace(sqlStmt, "%%TRUE%%", 
                                         DBUtil.getBooleanValue(true, conn));
                sqlStmt
                    = StringUtil.replace(sqlStmt, "%%FALSE%%", 
                                         DBUtil.getBooleanValue(false, conn));

                ps = conn.prepareStatement(sqlStmt);
                log(">>>>> Processing statement desc=["+desc+"] " +
                    "SQL=["+sqlStmt+"]");
                
                try {
                    ps.execute();
                } catch (SQLException e) {
                    if (!isFail() ||
                        sqlStmt.trim().toLowerCase().startsWith("drop")) {
                        log(">>>>> SQL failed: " + e);
                        conn.rollback();
                        conn.commit();
                    } else {
                        throw e;
                    }
                } 
            } catch ( Exception e ) {
                throw new BuildException("Error executing statement " +
                                         "desc=["+desc+"] SQL=["+sqlStmt+"] "
                                         + e);
            } finally {
                DBUtil.closeStatement("Statement", ps);
            }
        }
    }

    public class VerifySchema extends Task {

        private String table = null;
        private String column = null;
        private Connection conn = null;

        public void init (Connection conn) { this.conn = conn; }
        public void setTable (String t) { table = t; }
        public void setColumn (String c) { column = c; }

        public void execute () throws BuildException {

            validateAttributes();
            String ctx = "VerifySchema";
            try {
                boolean foundColumn = DBUtil.checkColumnExists(ctx, conn, 
                                                               table, column);
                if ( !foundColumn ) {
                    throw new BuildException("Cannot update: column " + column
                                             + " does not exist in table " + table);
                }
            } catch ( Exception e ) {
                throw new BuildException("Error updating " + table + "." + column 
                                         + ": " + e, e);
            }
        }

        private void validateAttributes () throws BuildException {
            if ( table == null )
                throw new BuildException("SchemaSpec: update: No 'table' " +
                                         "attribute specified.");
            if ( column == null )
                throw new BuildException("SchemaSpec: update: No 'column' " +
                                         "attribute specified.");
        }
    }
}
