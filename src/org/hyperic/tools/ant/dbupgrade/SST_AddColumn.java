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

    private String      _table;
    private String      _column;
    private String      _columnType;
    private String      _precision;
    private Initializer _initializer;
    private ForeignKey  _foreignKey;
    private String _default;
    private String _nullable;

    
    public SST_AddColumn () {}

    public void setDefault (String v) {
        _default = v;
    }
    
    public void setTable (String t) {
        _table = t;
    }

    public void setNullable (String n) {
        _nullable = n;
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
        PreparedStatement ps = null;
        String columnTypeName = getDBSpecificTypeName(_columnType);
        String alterSql = "ALTER TABLE " + _table + " ADD " + _column + " " + 
                          columnTypeName;
        
        alterSql += ( _precision != null ) ? "(" + _precision + ")" : "";
        
        try {
            if (_default != null) {
                String buf = null;
                 if (_columnType.equalsIgnoreCase("boolean") && (!_default.equalsIgnoreCase("null"))) {
                     buf = " default " + DBUtil.getBooleanValue(Boolean.valueOf(_default).booleanValue(), c);
                 } else {
                     buf = " default " + _default;
                 }
                 alterSql += buf;
             }
            // set the database null constraing after setting the default
            alterSql += ( _nullable != null ) ? " " + _nullable : "";
            
            // Check to see if the column exists.  If it's already there,
            // then don't re-add it.
            boolean foundColumn = DBUtil.checkColumnExists(_ctx, c, 
                                                           _table, _column);
            if ( foundColumn ) {
                log(">>>>> Not adding column: " + _column
                    + " because it already exists in table " + _table);
                return;
            }

            // Add the column.
            ps = c.prepareStatement(alterSql);
            log(">>>>> Adding column " + _column + " (type=" + columnTypeName + 
                ")  to table " + _table);
                
            ps.executeUpdate();

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
            throw new BuildException("SchemaSpec: update: No 'table' " + 
                                     "attribute specified.");
        if ( _column == null )
            throw new BuildException("SchemaSpec: update: No 'column' " + 
                                     "attribute specified.");
        if ( _columnType == null )
            throw new BuildException("SchemaSpec: update: No 'columnType' " + 
                                     "attribute specified.");
    }

    public class Initializer extends Task {
        private String      _initSql;
        private Connection  _conn;

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
                // Replace %%TRUE%% and %%FALSE%%
                _initSql
                    = StringUtil.replace(_initSql, "%%TRUE%%", 
                                         DBUtil.getBooleanValue(true, _conn));
                _initSql
                    = StringUtil.replace(_initSql, "%%FALSE%%", 
                                         DBUtil.getBooleanValue(false, _conn));
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
