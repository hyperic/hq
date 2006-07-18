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

package org.hyperic.hq.agent.server;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AgentJdbcProvider
    implements AgentStorageProvider
{
    private static final String PROP_TABLE_NAME =
        "CAM_AGENT_PROP";        
    private static final String PROP_INDEX_NAME =
        PROP_TABLE_NAME + "_INDEX";

    private static final String PROP_TABLE_CREATE =
        "CREATE TABLE " + PROP_TABLE_NAME + 
        " (ID INT IDENTITY PRIMARY KEY, " +
        "NAME VARCHAR(128) NOT NULL, " +
        "VALUE VARCHAR(15000) NOT NULL)";
    private static final String PROP_INDEX_CREATE =
        "CREATE INDEX " + PROP_INDEX_NAME + " ON " + PROP_TABLE_NAME +
        " (NAME)";
        
    private static final String PROP_INSERT = 
        "INSERT INTO " + PROP_TABLE_NAME + " (ID, NAME, VALUE) " +
        "VALUES (NULL, ?, ?)";
    private static final String PROP_DELETE_BY_KEY =
        "DELETE FROM " + PROP_TABLE_NAME + " WHERE NAME = ?";
    private static final String PROP_DELETE_BY_ID =
        "DELETE FROM " + PROP_TABLE_NAME + " WHERE ID = ?";
    private static final String PROP_QUERY_KEYS =
        "SELECT NAME FROM " + PROP_TABLE_NAME;
    private static final String PROP_QUERY =        
        "SELECT VALUE, ID FROM " + PROP_TABLE_NAME + " WHERE NAME = ?";
    private static final String PROP_UPDATE =
        "UPDATE " + PROP_TABLE_NAME + " SET VALUE = ? WHERE NAME = ?";
            
    private static final String POINTBASE_JDBC_DRIVER =
        "com.pointbase.me.jdbc.jdbcDriver";
    //private static final String POINTBASE_JDBC_CONNECT =
    //    "jdbc:pointbase:micro:" + POINTBASE_DATA_DIR + "/cam-agent-db";

    private static final String POINTBASE_JDBC_CONNECT =
        "jdbc:pointbase:micro:";
    private static final String DATABASE_NAME = "cam-agent-db";

    // Only one db connection and set of statements for all instances of 
    // the class                
    private static Connection        conn = null;    

    private static PreparedStatement stmtPropDeleteId  = null;
    private static PreparedStatement stmtPropDeleteKey = null;
    private static PreparedStatement stmtPropInsert    = null;
    private static PreparedStatement stmtPropQuery     = null;
    private static PreparedStatement stmtPropQueryKeys = null;
    private static PreparedStatement stmtPropUpdate    = null;

    private Log logger = LogFactory.getLog(AgentJdbcProvider.class);

    /**
     * Get information about the storage provider.
     *
     * @return A short description about the provider.
     */

    public String getDescription() {
        return "Agent JDBC provider. Entire contents of data " +
            "kept in a database";
    }

    /**
     * Sets a value within the storage object.  The key may be any
     * String, but should probably be in a Java Properties stylee.
     * If 'value' is null, the key will be deleted from storage.
     *
     * @param key    Key for the attribute
     * @param value  Value of the key
     */

    public void setValue(String key, String value) {
        try {
            if(value != null) {
                // Insert if the key doesn't exist
                if(this.getValue(key) == null) {
                    this.insert(key, value);
                } else {  // Update if the key does exist
                    this.update(key, value);
                }
                    
                // The flush method will do the commit
            }
            else {
                this.deleteByKey(key);
                // The flush method will do the commit
            }
        }
        catch(SQLException e) {
            // Log the exception
            this.logger.error("SQL Exception inserting or deleting key: " +
                key + ", value: " + value);
        }
    }
    
    /**
     * Gets a value from the storage object.
     * 
     * @param key   Key for which to retrieve the value
     *
     * @return The value previously specified via setValue, or null if the
     *          key does not exist.
     */

    public String getValue(String key) {
        String      result;
        ResultSet   rset = null;
        
        try {
            rset = this.query(key);
            
            // There should only be one row.
            if(rset.next() == true) {
                result = rset.getString(1);
            } else {
                this.logger.debug("Didn't find a value for key: " + key);                
                result = null;
            }
        }
        catch(SQLException e) {
            this.logger.error("SQL Exception finding key: " + key);
            result = null;
        }
        finally {
            if(rset != null) {
                try { rset.close(); } catch(Exception e) {}
            }
        }
        
        return result;
    }

    public Set getKeys () {
        ResultSet rset = null;
        HashSet set = new HashSet();
        synchronized (stmtPropQueryKeys) {
            try {
                rset = stmtPropQueryKeys.executeQuery();
                while (rset.next()) {
                    set.add(rset.getString(1));
                }
                return set;

            } catch (SQLException e) {
                // Log the exception
                String msg = "SQL Exception querying keys";
                this.logger.error(msg);
                IllegalStateException ise = new IllegalStateException(msg);
                ise.initCause(e);
                throw ise;

            } finally {
                if (rset != null) {
                    try { rset.close(); } catch(Exception e) {}
                }
            }
        }
    }

    /**
     * Flush values to permanent storage.  Implementers of this interface may
     * cache properties internally -- this method gives them a chance to
     * store it to permanent storage before it gets lost.
     */

    public void flush() throws AgentStorageException {
        this.logger.debug("Flushing storage");
        
        try {
            conn.commit();  // Committing setValue calls
        }
        catch(SQLException e) {
            String msg = "Couldn't flush. Commit failed: " + e.getMessage();
            this.logger.error(msg);
            throw new AgentStorageException(msg);
        }
    }

    /**
     * Initialize the storage provider with simple bootstrap information.
     * This string is unique to the storage provider and may contain a
     * filename for further configuration, database DSN, etc.
     *
     * @param info  Information for the StorageProvider to use to initialize
     */

    public void init(String info) throws AgentStorageException
    {
        // Skip the init if we have been called before
        if(conn != null)
            return;

        Statement stmtExec = null;
        
        try {
            Class.forName(AgentJdbcProvider.POINTBASE_JDBC_DRIVER);
            
            // Create the data directory if necessary
            this.logger.debug("init info=\'" + info + '\'');

            // Process the info string if it's not empty
            if(info.length() > 0) {                    
                // Create the data directory
                File dir = new File(info);
                if(dir.exists() == false) {
                    this.logger.debug("No agent data directory. Creating it: "+
                        dir.getAbsolutePath());
                    
                    if(dir.mkdir() == false) {
                        throw new IOException("Cannot create data directory: "+
                            dir.getAbsolutePath());
                    }
                }
                
                // Add a directory seperator if we don't see one to prep for
                // concatenation below
                if(info.charAt( info.length() - 1 ) != File.separatorChar)
                    info = info.concat(File.separator);
            }

            // Open a connection to the database
            String connstr = AgentJdbcProvider.POINTBASE_JDBC_CONNECT + info +
                AgentJdbcProvider.DATABASE_NAME;
            this.logger.debug("openning connection: \'" + connstr + '\'');

            AgentJdbcProvider.conn = DriverManager.getConnection(connstr);
            conn.setAutoCommit(false);

            this.createSchema();
            
            // Prepare the sql commands
            stmtPropInsert = 
                conn.prepareStatement(AgentJdbcProvider.PROP_INSERT); 
            stmtPropDeleteId = 
                conn.prepareStatement(AgentJdbcProvider.PROP_DELETE_BY_ID);
            stmtPropDeleteKey = 
                conn.prepareStatement(AgentJdbcProvider.PROP_DELETE_BY_KEY);
            stmtPropQuery = 
                conn.prepareStatement(AgentJdbcProvider.PROP_QUERY);
            stmtPropQueryKeys = 
                conn.prepareStatement(AgentJdbcProvider.PROP_QUERY_KEYS);
            stmtPropUpdate = 
                conn.prepareStatement(AgentJdbcProvider.PROP_UPDATE);
        }
        catch(Exception e) {
            // Cannot initialize, most likely because we can't find the JDBC
            // driver or less likely, because a SQL command failed.
            this.logger.error(e);
            throw new AgentStorageException(e.getMessage());
        }
        finally {
            if(stmtExec != null)
                try { stmtExec.close(); } catch(SQLException e) {}
        }
    }

    /**
     * Perform any cleanup that the storage provider requires.
     */

    public void dispose() {
        try {
            if(conn != null)
               conn.close();    
        }
        catch(Exception e) {
            this.logger.error("Couldn't close agent jdbc connection");
        }
    }
    
    /**
     * Add a value to a storage column.  If the column does not yet
     * exist, it will be created.
     *
     * @param listName Name of the column to add to
     * @param value    Value to add to the column
     */

    public void addToList(String listName, String value)
        throws AgentStorageException
    {
        try {
            this.insert(listName, value);
        }
        catch(SQLException e) {
            // Log the exception
            String msg = "SQL Exception adding to list: " + listName +
                ", value: " + value + ": " + e.getMessage();
            this.logger.error(msg);
            throw new AgentStorageException(msg);
        }
    }

    /**
     * Get an iterator for a named list.  If there is no list currently
     * in storage, or the list contains 0 elements, null will be returned.
     *
     * @param listName name of the list to get an iterator for.
     */
    public Iterator getListIterator(String listName) {
        try {
            ResultSet rset = this.query(listName);
            
            if(rset.next() == true) {            
                return ( new JdbcProviderIterator(rset, true, this) );
            } else {
                rset.close();
                return null;                        
            }
        }
        catch(SQLException e) {
            this.logger.error("SQL Exception querying list: " + listName +
                ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Delete an entire list from storage.  This is basically a shortcut
     * for deleting all elements as returned by getListIterator()
     */
    public void deleteList(String listName) {
        try {
            this.deleteByKey(listName);
            conn.commit();
        }
        catch(SQLException e) {
            // Log the exception
            this.logger.error("SQL Exception deleting list: " + listName +
                ": " + e.getMessage());
        }
    }

    public void createList(String listName, int recSize) {
        // no-op
    }
    
    protected void deleteMetric(long id) {
        try {
            this.deleteById((int)id);
        }
        catch(SQLException e) {
            // Log the exception
            this.logger.error("SQL Exception deleting value: " + id + ": " +
                e.getMessage());
        }
    }

    private void createSchema() {
        // Bootstrap our two tables. Look ma, no installer.
        // Let see if our tables exist, if they do not create them
        Statement stmt = null;
                    
        try {                    
            DatabaseMetaData meta = conn.getMetaData();
            stmt = conn.createStatement();
        
            // Metric Table
            if(meta.getTables(null, null,
                AgentJdbcProvider.PROP_TABLE_NAME, null).next() == false) {
                    stmt.executeUpdate(AgentJdbcProvider.PROP_TABLE_CREATE);
            }
        
            // Metric Table Index
            if(meta.getIndexInfo(null, null,
                AgentJdbcProvider.PROP_TABLE_NAME,
                false, false).next() == false) {
                    stmt.executeUpdate(AgentJdbcProvider.PROP_INDEX_CREATE);
            }
        }
        catch(SQLException e) {
            // Things have gone very, very wrong
            this.logger.error("Cannot create schema: " + e);
        }
        finally {
            if(stmt != null)
                try { stmt.close(); } catch(SQLException e) {}
        }
    }

    private void deleteById(int id) throws SQLException
    {
        this.logger.debug("Removing '" + id + "' from storage");
        synchronized(this){
            stmtPropDeleteId.setInt(1, id);
            stmtPropDeleteId.execute();
        }
    }
    
    private void deleteByKey(String key) throws SQLException
    {
        this.logger.debug("Removing '" + key + "' from storage");
        synchronized(this){
            stmtPropDeleteKey.setString(1, key);
            stmtPropDeleteKey.execute();
        }
    }
    
    private void insert(String key, String value) throws SQLException
    {
        this.logger.debug("Inserting '" + value + "' into '" + key + '\'');
        synchronized(this){
            stmtPropInsert.setString(1, key);
            stmtPropInsert.setString(2, value);
            stmtPropInsert.execute();
        }
    }

    private ResultSet query(String key) throws SQLException
    {
        //this.logger.trace("Querying '" + key + "' from storage");
        synchronized(this){
            stmtPropQuery.setString(1, key);
            return stmtPropQuery.executeQuery();
        }
    }
    
    private void update(String key, String value) throws SQLException
    {
        this.logger.debug("Updating '" + value + "' in '" + key + '\'');
        synchronized(this){
            stmtPropUpdate.setString(1, value);
            stmtPropUpdate.setString(2, key);
            stmtPropUpdate.execute();
        }
    }
}


