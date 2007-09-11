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
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.hyperic.util.jdbc.DBUtil;

public class SST_AlterColumn extends SchemaSpecTask {
    private String      _table;
    private String      _column;
    private String      _columnType;
    private String      _precision;
    private String      _nullable;
    private String      _defval;
    private boolean     _quoteDefault = true; // Defaults to true
    private Initializer _initializer;
    private ForeignKey  _foreignKey;

    public SST_AlterColumn () {}

    public void setTable (String t) {
        _table = t;
    }
    
    public void setColumn (String c) {
        _column = c;
    }
    
    public void setColumnType (String ct) {
        _columnType = ct;
    }
    
    public void setPrecision (String p) {
        _precision = p;
    }
    
    public void setNullable (String n) {
        _nullable = n;
    }
    
    public void setDefault (String d) {
        _defval = d;
    }
    
    public void setQuoteDefault (String d) {
        _quoteDefault = d.equalsIgnoreCase("t") || d.equalsIgnoreCase("true") ||
                        d.equalsIgnoreCase("y") || d.equalsIgnoreCase("yes");
    }
    
    public Initializer createInitializer () {
        if ( _initializer != null ) {
            throw new IllegalStateException("Multiple initializers "
                                            + "not permitted");
        }
        _initializer = new Initializer();
        return _initializer;
    }
    
    public ForeignKey createForeignKey () {
        if ( _foreignKey != null ) {
            throw new IllegalStateException("Multiple foreignKeys "
                                            + "not permitted");
        }
        _foreignKey = new ForeignKey();
        return _foreignKey;
    }

    public void execute () throws BuildException {
        validateAttributes();

        Connection c = getConnection();
        try {
            if (DBUtil.isOracle(c))
                alter_oracle(c);
            else if (DBUtil.isPostgreSQL(c))
                alter_pgsql(c);
            else {
                int dbtype = DBUtil.getDBType(c);
                throw new BuildException("Unsupported database: " + dbtype);
            }
        } catch (SQLException e) {
            throw new BuildException("Error determining dbtype: " + e, e);
        }
    }

    private void alter_oracle (Connection c) throws BuildException {
        String columnTypeName = null;
        String alterSql =
            "ALTER TABLE " + _table + " MODIFY (" + _column;

        if (_columnType != null) {
            columnTypeName =  getDBSpecificTypeName(_columnType);
            alterSql += " " + columnTypeName;
        }

        if (_defval != null) {
            alterSql += " DEFAULT '" + _defval + "'";
        }
        
        if ( _precision != null ) { 
            alterSql += " (" + _precision + ")";
        }

        if (_nullable != null) {
            alterSql += " " + _nullable;
        }
        alterSql += ")";

        List sql = new ArrayList();
        sql.add(alterSql);
        doAlter(c, sql);
    }

    private void alter_pgsql (Connection c) throws BuildException {
        String columnTypeName = null;
        List sqlList = new ArrayList();

        if (_columnType != null) {
            columnTypeName =  getDBSpecificTypeName(_columnType);
            if ( _precision != null ) { 
                columnTypeName += " (" + _precision + ")";
            }
            // In PostgreSQL you are not allowed to change a column type.
            // So instead, we rename the column, create a new column with the
            // desired datatype, copy the data over from the renamed/old column,
            // and then drop the renamed/old column.
            sqlList.add("ALTER TABLE " + _table
                        + " RENAME " + _column + " TO tmp_" + _column);
            sqlList.add("ALTER TABLE " + _table 
                        + " ADD " + _column + " " + columnTypeName);
            sqlList.add("UPDATE " + _table 
                        + " SET " + _column + " = tmp_" + _column);
            sqlList.add("ALTER TABLE " + _table
                        + " DROP COLUMN tmp_" + _column);
        }

        if (_defval != null) {
            if (_quoteDefault) {
                sqlList.add("ALTER TABLE " + _table + " ALTER " + _column +
                            " SET DEFAULT '" + _defval + "'");
            } else {
                sqlList.add("ALTER TABLE " + _table + " ALTER " + _column +
                            " SET DEFAULT " + _defval);
            }
        }
        
        if (_nullable != null) {
            if (_nullable.equalsIgnoreCase("NOT NULL")) {
                sqlList.add("ALTER TABLE " + _table 
                            + " ALTER " + _column + " SET NOT NULL");
            } else if (_nullable.equalsIgnoreCase("NULL")) {
                sqlList.add("ALTER TABLE " + _table
                            + " ALTER " + _column + " DROP NOT NULL");
            } else {
                throw new BuildException("Invalid nullable attribute: " + _nullable);
            }
        }

        doAlter(c, sqlList);
    }

    private void doAlter (Connection c, List sqlList) {
        PreparedStatement ps = null;
        String sql;
        try {
            // Check to see if the column exists.  If it doesn't exist
            // then can't alter it
            boolean foundColumn = DBUtil.checkColumnExists(_ctx, c, 
                                                           _table, _column);
            if ( !foundColumn ) {
                log(">>>>> Not altering column: " + _column
                    + " because it does not exist in table " + _table);
                return;
            }

            // Alter the column.
            for (int i=0; i<sqlList.size(); i++) {
                sql = (String) sqlList.get(i);
                log(">>>>> Altering with statement: " + sql);
                ps = c.prepareStatement(sql);
                ps.executeUpdate();
            }

            // Initialize the column
            if ( _initializer != null ) {
                _initializer.init(c);
                _initializer.execute();
            }
            if ( _foreignKey != null ) {
                _foreignKey.init(c);
                _foreignKey.execute();
            }

        } catch ( Exception e ) {
            throw new BuildException("Error updating " + _table + "." + _column 
                                     + ": " + e, e);
        } finally {
            DBUtil.closeStatement(_ctx, ps);
        }
    }

    private void validateAttributes () throws BuildException {
        if ( _table == null )
            throw new BuildException(
                    "SchemaSpec: update: No 'table' attribute specified.");
        if ( _column == null )
            throw new BuildException(
                    "SchemaSpec: update: No 'column' attribute specified.");
        if ( _columnType == null && _nullable == null && _defval == null)
            throw new BuildException(
                    "SchemaSpec: update: No 'columnType', 'default, or " +
                    "'nullable' attribute specified.");
    }

    public class Initializer extends Task {

        private String     _initSql;
        private Connection _conn;

        public Initializer () {}

        public void init (Connection conn) {
            _conn = conn;
        }

        public void addText(String msg) {
            if ( _initSql == null ) _initSql = "";
            _initSql += project.replaceProperties(msg);
        }

        public void execute() throws BuildException {

            if ( _initSql == null ) return;

            PreparedStatement ps = null;
            try {
                ps = _conn.prepareStatement(_initSql);
                log(">>>>> Initializing " + _table + "." + _column 
                    + " with " + _initSql);
                ps.executeUpdate();

            } catch ( Exception e ) {
                throw new BuildException("Error initializing " 
                                         + _table + "." + _column 
                                         + " (sql=" + _initSql + ")");
            } finally {
                DBUtil.closeStatement(_ctx, ps);
            }
        }
    }

    public class ForeignKey extends Task {
        private String     _constraintName;
        private String     _refs;
        private Connection _conn;

        public ForeignKey () {}

        public void init (Connection conn) {
            _conn = conn;
        }

        public void setConstraintName (String constraintName) {
            _constraintName = constraintName;
        }

        public void setReferences (String refs) {
            _refs = refs;
        }

        public void execute () throws BuildException {
            String fkSql
                = "ALTER TABLE " + _table + " "
                + "ADD CONSTRAINT " + _constraintName + " "
                + "FOREIGN KEY (" + _column + ") REFERENCES " + _refs;
            PreparedStatement ps = null;
            try {
                ps = _conn.prepareStatement(fkSql);
                log(">>>>> Adding foreign key constraint " + _constraintName 
                    + " on " + _table + "." + _column + "->" + _refs);
                ps.executeUpdate();

            } catch ( Exception e ) {
                throw new BuildException("Error adding foreign key for "
                                         + _table + "." + _column 
                                         + " (sql=" + fkSql + ")");
            } finally {
                DBUtil.closeStatement(_ctx, ps);
            }
        }
    }
}
