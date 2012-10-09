/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], Hyperic, Inc.
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

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.tools.db.TypeMap;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.jdbc.JDBC;
import org.hyperic.util.security.MarkedStringEncryptor;
import org.hyperic.util.security.SecurityUtil;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.properties.PropertyValueEncryptionUtils;

public class DBUpgrader extends Task {

    public static final String ctx = DBUpgrader.class.getName();
    private static final String INITIAL_SCHEMA_VERSION = "@@@CAM_SCHEMA_VERSION@@@" ; 
    

    private final List   _schemaSpecs = new ArrayList();
    private String encryptionKey;
    private String _jdbcDriver;
    private String _jdbcUrl;
    private String _jdbcUser;
    private String _jdbcPassword;
    private int    _dbtype; // this is one of the JDBC.XXX_TYPE constants
    private int    _dbutilType; // this is one of the DBUtil.DATABASE_XXX constants

    // The query to find the existing schema version uses these.
    // It is of the form:
    // SELECT valueColumn FROM tableName WHERE keyColumn = keyMatch
    private String _valueColumn;
    private String _tableName;
    private String _keyColumn;
    private String _keyMatch;

    private File       _typeMapFile;
    private Collection _typeMaps;

    private String        _startSchemaVersionStr;
    private SchemaVersion _startSchemaVersion;
    private String        _targetSchemaVersionStr;
    private SchemaVersion _targetSchemaVersion;
    
    private PBEStringEncryptor encryptor ; 

    public DBUpgrader () {}

    public void setJdbcUrl ( String jdbcUrl ) {
        _jdbcUrl = jdbcUrl;
    }

    public void setJdbcUser ( String jdbcUser ) {
        _jdbcUser = jdbcUser;
    }

    public void setJdbcPassword ( String jdbcPassword ) {
        _jdbcPassword = jdbcPassword;
    }

    public void setValueColumn (String v) {
        _valueColumn = v;
    }

    public void setTable (String t) {
        _tableName = t;
    }

    public void setKeyColumn (String k) {
        _keyColumn = k;
    }

    public void setKeyMatch (String m) {
        _keyMatch = m;
    }

    public void setTypeMap ( File f ) {
        _typeMapFile = f;
    }

    public void setTargetSchemaVersion (String v) {
        _targetSchemaVersionStr = v;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }
    
    public final PBEStringEncryptor newEncryptor() { 
        return new MarkedStringEncryptor(
                SecurityUtil.DEFAULT_ENCRYPTION_ALGORITHM, this.encryptionKey) ;
    }//EOM 
    
    PBEStringEncryptor getEncryptor() { 
        return this.encryptor ; 
    }//EOM

    public SchemaSpec createSchemaSpec () {
        SchemaSpec ss = new SchemaSpec(this);
        _schemaSpecs.add(ss);
        return ss;
    }

    public Collection getTypeMaps () { return _typeMaps; }
    public int getDBType () { return _dbtype; }
    public int getDBUtilType () { return _dbutilType; }

    @Override
    public void execute () throws BuildException {
        validateAttributes();

        Project p = getProject();
        List newSpecs = new ArrayList();
        int i;
        newSpecs.addAll(_schemaSpecs);

        // Sort the schema specs - if any reordering occurred, consider that
        // an error.  Also, if there are any duplicate versions, that's an error
        Collections.sort(newSpecs);
        int size = _schemaSpecs.size();
        for ( i=0; i<size; i++ ) {
            if ( !newSpecs.get(i).equals(_schemaSpecs.get(i)) ) {
                throw new BuildException("DBUpgrader: SchemaSpecs specified "
                                         + "out of proper version ordering.");
            }
            if ( i>0 && newSpecs.get(i).equals(newSpecs.get(i-1)) ) {
                throw new BuildException("DBUpgrader: duplicate SchemaSpec "
                                         + "version specified.");
            }
        }

        Connection c = null;
        try {
            // Connect to the database to grab the starting schema version
            c = getConnection();
            _dbutilType = DBUtil.getDBType(c);
            _startSchemaVersionStr = loadStartSchemaVersion(c);
            if (_startSchemaVersionStr.indexOf(HQConstants.SCHEMA_MOD_IN_PROGRESS) != -1) {
                throw new BuildException("DBUpgrader: Database schema is in "
                                         + "an inconsistent state: version="
                                         + _startSchemaVersionStr);
            }
            try {
                _startSchemaVersion = new SchemaVersion(_startSchemaVersionStr);
            } catch (IllegalArgumentException e) {
                throw new BuildException("DBUpgrader: " + e.getMessage(), e);
            }
            log("Starting schema migration: " + _startSchemaVersion
                + " -> " + _targetSchemaVersion);

            // If the target version is LATEST, then figure out the "real"
            // target version.
            String realTargetSchemaVersion = _targetSchemaVersion.toString();
            if ( _targetSchemaVersion.getIsLatest() ) {
                SchemaSpec latestSpec = (SchemaSpec) _schemaSpecs.get(size-1);
                 realTargetSchemaVersion = latestSpec.getVersion().toString();
            }

            // Ensure that we're not trying to "downgrade" - that is,
            // ensure that the target version is not earlier than the
            // existing server version.  In particular, if the target
            // version is LATEST but the actual latest SchemaSpec is
            // earlier than the current database's schema version, we
            // consider that a downgrade as well.
            SchemaVersion realTargetSchemaVersionSchemaSpec =
                    new SchemaVersion(realTargetSchemaVersion);
            // _startSchemaVersion == LATEST on fresh DBsetup
            if (!_startSchemaVersion.getIsLatest() &&
                realTargetSchemaVersionSchemaSpec.compareTo(_startSchemaVersion) < 0 )
            {
                throw new BuildException("SchemaSpec: cannot downgrade from "
                                         + _startSchemaVersion + " -> "
                                         + realTargetSchemaVersion);
            }

            size = _schemaSpecs.size();
            SchemaSpec ss;
            c.setAutoCommit(false);
            SchemaVersion fromVersion = _startSchemaVersion;
            SchemaVersion toVersion;
            for ( i=0; i<size; i++ ) {
                ss = (SchemaSpec) _schemaSpecs.get(i);
                toVersion = ss.getVersion();
                if ( !shouldExecSpecVersion(toVersion, ss) ) continue;
                
                try {
                    markSchemaModificationInProgress(c, fromVersion, toVersion);
                    ss.initialize(c, this);
                    ss.execute();
                    
                    //only update the database if the current schema spec's version is bigger than the start version
                    //(could happen when the alwaysExecute flag is set to true for a given schemaScpec element) 
                    if(_startSchemaVersion.compareTo(toVersion) < 0) {
                        log("Upgrading " + fromVersion + " -> " + toVersion);
                        updateSchemaVersion(c, toVersion.toString());
                        c.commit();
                        fromVersion = toVersion;
                        log("Upgraded " + fromVersion + " -> " + toVersion + " OK");
                    }//EO if the current schema spec was smaller than the target's one   

                } catch ( Exception e ) {
                    try {
                        c.rollback();
                    } catch ( Exception e2 ) {
                        log("Error rolling back: " + e2);
                    }
                    throw new BuildException("DBUpgrader: Error running "
                                             + "SchemaSpec: " + ss.getVersion()
                                             + ": " + e, e);
                }
            }

            // If this was a "upgrade to latest", then ensure that
            // the schema version gets set correctly.
            if ((this._targetSchemaVersion.getIsLatest()) || (this._startSchemaVersionStr.equals(INITIAL_SCHEMA_VERSION))) {
                updateSchemaVersion(c, realTargetSchemaVersion);
                c.commit();
            }

            log("DATABASE SUCCESSFULLY UPGRADED TO " + realTargetSchemaVersion);

        } catch (SQLException e) {
            throw new BuildException("DBUpgrader: sql error: " + e, e);

        } finally {
            DBUtil.closeConnection(ctx, c);
        }
    }

    protected boolean shouldExecSpecVersion (SchemaVersion version, final SchemaSpec schemaSpec) {
        return (version.getIsLatest() || version.between(_startSchemaVersion, _targetSchemaVersion) || 
                schemaSpec.shouldAlwaysExecute()) ; 
    }

    void validateAttributes () throws BuildException {
        if ( _jdbcUrl == null )
            throw new BuildException("DBUpgrader: No 'jdbcUrl' attribute specified.");

        _jdbcDriver = JDBC.getDriverString(_jdbcUrl);
        try {
            Class.forName(_jdbcDriver).newInstance();
        } catch (Exception e) {
            throw new BuildException("Error loading jdbc driver: "
                                     + _jdbcDriver + ": " + e, e);
        }
        _dbtype = JDBC.toType(_jdbcUrl);

        if ( _typeMapFile == null )
            throw new BuildException("DBUpgrader: No 'typeMap' attribute specified.");
        try {
            _typeMaps = TypeMap.loadTypeMapFromFile(_typeMapFile);
        } catch ( Exception e ) {
            throw new BuildException("DBUpgrader: Error loading typemap from: "
                                     + _typeMapFile.getAbsolutePath()
                                     + ": " + e, e);
        }
        
        if ( _targetSchemaVersionStr == null )
            throw new BuildException("DBUpgrader: No 'targetSchemaVersion' attribute specified.");
        try {
            _targetSchemaVersion = new SchemaVersion(_targetSchemaVersionStr);
        } catch (IllegalArgumentException e) {
            throw new BuildException("SchemaSpec: " + e.getMessage(), e);
        }
        
        this.encryptor = this.newEncryptor() ; 
    }

    /**
     * this method is here to determine if the database has key
     * and value columsn which are prefixed with 'PROP' or not
     * it was added as part of supporting MySQL5.
     * @return true if key & value columns should be prefixed with 'PROP'
     *         false otherwise
     */
    private boolean usePrefix(Connection c) throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;

        try {
            _tableName = _tableName.toUpperCase();
            String sql = "SELECT * FROM " + _tableName;
            stmt = c.createStatement();

            rs = stmt.executeQuery(sql);
            ResultSetMetaData rsmd = rs.getMetaData();

            for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                String column = rsmd.getColumnName(i);
                if (column.equalsIgnoreCase(_valueColumn))
                    return false;
            }

            return true;
        } finally {
            DBUtil.closeStatement(ctx, stmt);
            DBUtil.closeResultSet(ctx, rs);
        }
    }

    private String loadStartSchemaVersion (Connection c) throws BuildException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        String versionString;
        String origSql
            = "SELECT " + _valueColumn + " "
            + "FROM " + _tableName + " "
            + "WHERE " + _keyColumn + " = ? ";
        // tihs because we changed the key/value columns
        // to support mysqls reserved words
        String alternSql
            = "SELECT PROP" + _valueColumn + " "
            + "FROM " + _tableName + " "
            + "WHERE PROP" + _keyColumn + " = ?";

        try {
            if(usePrefix(c)) {
                ps = c.prepareStatement(alternSql);
            } else {
                ps = c.prepareStatement(origSql);
            }
            ps.setString(1, _keyMatch);
            rs = ps.executeQuery();
            if ( rs.next() ) {
                versionString = rs.getString(1);
            } else {
                //throw new BuildException("Schema version not found!");
                versionString = INITIAL_SCHEMA_VERSION  ;
            }
            if ( rs.next() ) {
                throw new BuildException("Multiple matches found for "
                                         + "schema version!");
            }
            return versionString;

        } catch ( SQLException e ) {
            throw new BuildException("Error loading starting schema version: "
                                     + e, e);
        } finally {
            DBUtil.closeStatement(ctx, ps);
            DBUtil.closeResultSet(ctx, rs);
        }
    }

    private void markSchemaModificationInProgress ( Connection c,
                                                    SchemaVersion fromVersion,
                                                    SchemaVersion toVersion)
        throws BuildException {

        String versionString
            = fromVersion.toString()
            + HQConstants.SCHEMA_MOD_IN_PROGRESS
            + toVersion.toString();
        updateSchemaVersion(c, versionString);
    }

    private void updateSchemaVersion(Connection c, String v) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sql
            = "UPDATE " + _tableName + " "
            + "SET " + _valueColumn + " = ? "
            + "WHERE " + _keyColumn + " = ? ";
        // mysql support changed these columsn to prefix with prop
        String alternSql
            = "UPDATE " + _tableName + " "
            + "SET PROP" + _valueColumn + " = ? "
            + "WHERE PROP" + _keyColumn + " = ? ";
        try {
            if(usePrefix(c)) {
                ps = c.prepareStatement(alternSql);
            } else {
                ps = c.prepareStatement(sql);
            }
            ps.setString(1, v);
            ps.setString(2, _keyMatch);
            ps.executeUpdate();
        } catch ( SQLException e ) {
            throw new BuildException("Error updating schema version to "
                                     + "'" + v + "': "
                                     + e, e);
        } finally {
            DBUtil.closeStatement(ctx, ps);
            DBUtil.closeResultSet(ctx, rs);
        }
    }

    public Connection getConnection () throws SQLException {
        
        if ( _jdbcUser == null && _jdbcPassword == null ) {
            return DriverManager.getConnection(_jdbcUrl);
        } else {
            String password = _jdbcPassword;
                        
            if (PropertyValueEncryptionUtils.isEncryptedValue(password)) {
                password = decryptPassword(password);
            }
            
            return DriverManager.getConnection(_jdbcUrl, _jdbcUser, password);
        }
    }
    
    private String decryptPassword(String clearTextPassword) {
        return this.encryptor.decrypt(clearTextPassword) ; 
    }
    
     
}
