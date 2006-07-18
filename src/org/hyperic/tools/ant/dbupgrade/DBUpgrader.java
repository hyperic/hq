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

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.jdbc.JDBC;
import org.hyperic.tools.db.TypeMap;

public class DBUpgrader extends Task {

    public static final String ctx = DBUpgrader.class.getName();

    public static final String SCHEMA_MOD_IN_PROGRESS
        = " *** UPGRADE IN PROGRESS: migrating to version ";

    private List   schemaSpecs = new ArrayList();

    private String jdbcDriver   = null;
    private String jdbcUrl      = null;
    private String jdbcUser     = null;
    private String jdbcPassword = null;
    private int    dbtype; // this is one of the JDBC.XXX_TYPE constants
    private int    dbutilType; // this is one of the DBUtil.DATABASE_XXX constants

    // The query to find the existing schema version uses these.
    // It is of the form: 
    // SELECT valueColumn FROM tableName WHERE keyColumn = keyMatch
    private String valueColumn  = null;
    private String tableName    = null;
    private String keyColumn    = null;
    private String keyMatch     = null;

    private File   typeMapFile = null;
    private Collection typeMaps = null;

    private String startSchemaVersionString = null;
    private SchemaVersion startSchemaVersion = null;
    private String targetSchemaVersionString = null;
    private SchemaVersion targetSchemaVersion = null;

    public DBUpgrader () {}

    public void setJdbcUrl ( String jdbcUrl ) {
        this.jdbcUrl = jdbcUrl;
    }

    public void setJdbcUser ( String jdbcUser ) {
        this.jdbcUser = jdbcUser;
    }

    public void setJdbcPassword ( String jdbcPassword ) {
        this.jdbcPassword = jdbcPassword;
    }

    public void setValueColumn (String v) {
        valueColumn = v;
    }
    public void setTable (String t) {
        tableName = t;
    }
    public void setKeyColumn (String k) {
        keyColumn = k;
    }
    public void setKeyMatch (String m) {
        keyMatch = m;
    }

    public void setTypeMap ( File f ) {
        this.typeMapFile = f;
    }

    public void setTargetSchemaVersion (String v) {
        this.targetSchemaVersionString = v;
    }

    public SchemaSpec createSchemaSpec () {
        SchemaSpec ss = new SchemaSpec(this);
        schemaSpecs.add(ss);
        return ss;
    }

    public Collection getTypeMaps () { return typeMaps; }
    public int getDBType () { return dbtype; }
    public int getDBUtilType () { return dbutilType; }

    public void execute () throws BuildException {

        validateAttributes();

        Project p = getProject();
        List newSpecs = new ArrayList();
        int i;
        newSpecs.addAll(schemaSpecs);

        // Sort the schema specs - if any reordering occurred, consider that
        // an error.  Also, if there are any duplicate versions, that's an error
        Collections.sort(newSpecs);
        int size = schemaSpecs.size();
        for ( i=0; i<size; i++ ) {
            if ( !newSpecs.get(i).equals(schemaSpecs.get(i)) ) {
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
            dbutilType = DBUtil.getDBType(c);
            startSchemaVersionString = loadStartSchemaVersion(c);
            if (startSchemaVersionString.indexOf(SCHEMA_MOD_IN_PROGRESS) != -1) {
                throw new BuildException("DBUpgrader: Database schema is in "
                                         + "an inconsistent state: version="
                                         + startSchemaVersionString);
            }
            try {
                startSchemaVersion
                    = new SchemaVersion(startSchemaVersionString);
            } catch (IllegalArgumentException e) {
                throw new BuildException("DBUpgrader: " + e.getMessage(), e);
            }
            log("Starting schema migration: " + startSchemaVersion 
                + " -> " + targetSchemaVersion);

            // If the target version is LATEST, then figure out the "real"
            // target version.
            String realTargetSchemaVersion = targetSchemaVersion.toString();
            if ( targetSchemaVersion.getIsLatest() ) {
                SchemaSpec latestSpec = (SchemaSpec) schemaSpecs.get(size-1);
                 realTargetSchemaVersion = latestSpec.getVersion().toString();
            }

            // Ensure that we're not trying to "downgrade" - that is,
            // ensure that the target version is not earlier than the 
            // existing server version.  In particular, if the target
            // version is LATEST but the actual latest SchemaSpec is
            // earlier than the current database's schema version, we 
            // consider that a downgrade as well.
            if ( targetSchemaVersion.compareTo(startSchemaVersion) < 0 ) {
                throw new BuildException("SchemaSpec: cannot downgrade from "
                                         + startSchemaVersion + " -> " 
                                         + realTargetSchemaVersion);
            }
        
            size = schemaSpecs.size();
            SchemaSpec ss;
            c.setAutoCommit(false);
            SchemaVersion fromVersion = startSchemaVersion;
            SchemaVersion toVersion;
            for ( i=0; i<size; i++ ) {
                ss = (SchemaSpec) schemaSpecs.get(i);
                toVersion = ss.getVersion();
                if ( !shouldExecSpecVersion(toVersion) ) continue;
                log("Upgrading " + fromVersion + " -> " + toVersion);
                try {
                    markSchemaModificationInProgress(c, fromVersion, toVersion);
                    ss.initialize(c, this);
                    ss.execute();
                    updateSchemaVersion(c, toVersion.toString());
                    c.commit();
                    fromVersion = toVersion;
                    log("Upgraded " + fromVersion + " -> " + toVersion + " OK");

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
            if ( targetSchemaVersion.getIsLatest() ) {
                updateSchemaVersion(c, realTargetSchemaVersion);
                c.commit();
            }

            log("DATABASE SUCCESSFULLY UPGRADED TO "
                + realTargetSchemaVersion);

        } catch (SQLException e) {
            throw new BuildException("DBUpgrader: sql error: " + e, e);

        } finally {
            DBUtil.closeConnection(ctx, c);
        }
    }

    protected boolean shouldExecSpecVersion (SchemaVersion version) {
        return version.getIsLatest() ||
            version.between(startSchemaVersion, targetSchemaVersion);
    }

    private void validateAttributes () throws BuildException {
        if ( jdbcUrl == null )
            throw new BuildException("DBUpgrader: No 'jdbcUrl' attribute specified.");

        jdbcDriver = JDBC.getDriverString(jdbcUrl);
        try {
            Class.forName(jdbcDriver).newInstance();
        } catch (Exception e) {
            throw new BuildException("Error loading jdbc driver: "
                                     + jdbcDriver + ": " + e, e);
        }
        dbtype = JDBC.toType(jdbcUrl);

        if ( typeMapFile == null )
            throw new BuildException("DBUpgrader: No 'typeMap' attribute specified.");
        try {
            typeMaps = TypeMap.loadTypeMapFromFile(typeMapFile);
        } catch ( Exception e ) {
            throw new BuildException("DBUpgrader: Error loading typemap from: "
                                     + typeMapFile.getAbsolutePath()
                                     + ": " + e, e);
        }

        if ( targetSchemaVersionString == null )
            throw new BuildException("DBUpgrader: No 'targetSchemaVersion' attribute specified.");
        try {
            targetSchemaVersion = new SchemaVersion(targetSchemaVersionString);
        } catch (IllegalArgumentException e) {
            throw new BuildException("SchemaSpec: " + e.getMessage(), e);
        }
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
            tableName = tableName.toUpperCase();
            String sql = "SELECT * FROM " + tableName;
            stmt = c.createStatement();
            
            rs = stmt.executeQuery(sql);
            ResultSetMetaData rsmd = rs.getMetaData();

            for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                String column = rsmd.getColumnName(i);
                if (column.equalsIgnoreCase(valueColumn))
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
            = "SELECT " + valueColumn + " "
            + "FROM " + tableName + " "
            + "WHERE " + keyColumn + " = ? ";
        // tihs because we changed the key/value columns
        // to support mysqls reserved words
        String alternSql
            = "SELECT PROP" + valueColumn + " " 
            + "FROM " + tableName + " "
            + "WHERE PROP" + keyColumn + " = ?"; 

        try {
            if(usePrefix(c)) {
                ps = c.prepareStatement(alternSql);
            } else {
                ps = c.prepareStatement(origSql);
            }
            ps.setString(1, keyMatch);
            rs = ps.executeQuery();
            if ( rs.next() ) {
                versionString = rs.getString(1);
            } else {
                throw new BuildException("Schema version not found!");
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
            + SCHEMA_MOD_IN_PROGRESS
            + toVersion.toString();
        updateSchemaVersion(c, versionString);
    }

    private void updateSchemaVersion(Connection c, String v) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sql
            = "UPDATE " + tableName + " "
            + "SET " + valueColumn + " = ? "
            + "WHERE " + keyColumn + " = ? ";
        // mysql support changed these columsn to prefix with prop
        String alternSql
            = "UPDATE " + tableName + " "
            + "SET PROP" + valueColumn + " = ? "
            + "WHERE PROP" + keyColumn + " = ? ";
        try {
            if(usePrefix(c)) {
                ps = c.prepareStatement(alternSql);
            } else {
                ps = c.prepareStatement(sql);
            }
            ps.setString(1, v);
            ps.setString(2, keyMatch);
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
        if ( jdbcUser == null && jdbcPassword == null ) {
            return DriverManager.getConnection(jdbcUrl);
        } else {
            return DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
        }
    }
}
