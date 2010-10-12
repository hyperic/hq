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

package org.hyperic.tools.ant;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;

import org.hyperic.util.jdbc.DBUtil;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.properties.PropertyValueEncryptionUtils;

public class ServerConfigPropsUpgrader extends Task {

    private static final String ctx = ServerConfigPropsUpgrader.class.getName();

    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;
    private String encryptionKey;
    private String tableName = "EAM_CONFIG_PROPS";
    private String keyColumn = "PROPKEY";
    private String valueColumn = "PROPVALUE";
    private String propKey;
    private String propValue;

    public void setJdbcUrl ( String jdbcUrl ) {
        this.jdbcUrl = jdbcUrl;
    }

    public void setJdbcUser ( String jdbcUser ) {
        this.jdbcUser = jdbcUser;
    }

    public void setJdbcPassword ( String jdbcPassword ) {
        this.jdbcPassword = jdbcPassword;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }
    
    public void setPropKey(String propKey) {
    	this.propKey = propKey;
    }
    
    public void setPropValue(String propValue) {
    	this.propValue = propValue;
    }

    public void execute () throws BuildException {
        validate();

        Connection c = null;
        
        try {
            c = getConnection();
            c.setAutoCommit(false);
            
            // TODO: Add functionality to update multiple config props
            updateConfigProp(c, propKey, propValue);
        	
            c.commit();
            log("Updated " + propKey + " to " + propValue);
        } catch (SQLException e) {
            try {
                c.rollback();
            } catch ( Exception e2 ) {
                log("Error rolling back: " + e2);
            }
            throw new BuildException("ServerConfigPropsUpgrader: Error updating "
                                     + propKey + " to " + propValue
                                     + ": " + e.getMessage(), e);

        } finally {
            DBUtil.closeConnection(ctx, c);
        }
    }

    private void validate() throws BuildException {
        if (jdbcUrl == null) {
            throw new BuildException("No jdbcUrl was set, can't continue");
        }
        if (propKey == null) {
            throw new BuildException("No config prop key was set");
        }
        if (propValue == null) {
            throw new BuildException("No config prop value was set");
        }
    }

    private void updateConfigProp(Connection c,
    							  String key,
    							  String value) 
    	throws SQLException {
    	
        PreparedStatement ps = null;
        String sql
            = "UPDATE " + this.tableName + " "
            + "SET " + this.valueColumn + " = ? "
            + "WHERE " + this.keyColumn + " = ? ";

        try {
            ps = c.prepareStatement(sql);
            ps.setString(1, value);
            ps.setString(2, key);
            ps.executeUpdate();
        } finally {
            DBUtil.closeStatement(ctx, ps);
        }
    }
    
    private Connection getConnection() throws SQLException {
        if (jdbcUser == null && jdbcPassword == null ) {
            return DriverManager.getConnection(jdbcUrl);
        } else {
            String password = jdbcPassword;
                        
            if (PropertyValueEncryptionUtils.isEncryptedValue(password)) {
                log("Encryption key is " + encryptionKey);
                password = decryptPassword(
                                "PBEWithMD5AndDES",
                                encryptionKey,
                                password);
            }
            
            return DriverManager.getConnection(jdbcUrl, jdbcUser, password);
        }
    }
    
    private String decryptPassword(String algorithm, 
    		                       String encryptionKey,
    		                       String clearTextPassword) {

    	// TODO: This needs to be refactored into a security utility class

    	StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
    	encryptor.setPassword(encryptionKey);
    	encryptor.setAlgorithm(algorithm);

    	return PropertyValueEncryptionUtils.decrypt(clearTextPassword, encryptor);
    }
}
