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

package org.hyperic.hq.auth.server;

import java.security.acl.Group;
import java.util.Map;
import java.util.Properties;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.login.FailedLoginException;

import org.hyperic.hq.common.shared.HQConstants;

import org.jboss.security.SimpleGroup;
import org.jboss.security.auth.spi.UsernamePasswordLoginModule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A JDBC login module that only supports authentication
 *
 * JDBC LoginModule options:
 *
 * principalsQuery
 *   Query used to extract the password for a given user.  By default
 *   this value is "SELECT password FROM principals WHERE principal=?"
 *
 * dsJndiName 
 *   JNDI name of the datasource to use.  Default value is java:/HypericDS
 *
 */

public class JDBCLoginModule extends UsernamePasswordLoginModule
{
    private String dsJndiName;
    private String principalsQuery = 
        "SELECT password FROM EAM_PRINCIPAL WHERE principal=?";
    protected Log log = LogFactory.getLog(JDBCLoginModule.class);

    public void initialize(Subject subject, CallbackHandler handler,
                          Map sharedState, Map options)
    {
        super.initialize(subject, handler, sharedState, options);
        
        dsJndiName = (String) options.get("dsJndiName");
        if (dsJndiName == null) {
            dsJndiName = HQConstants.DATASOURCE;
        }
        Object tmpQuery = options.get("principalsQuery");
        if (tmpQuery != null) {
            principalsQuery = tmpQuery.toString();
        }
        log.debug("dsJndiName=" + dsJndiName);
        log.debug("prinipalsQuery=" + principalsQuery);
    }

    private Properties getProperties()
    {
        Properties props = new Properties();

        props.put("java.naming.factory.initial",
                  "org.jnp.interfaces.NamingContextFactory");
        props.put("java.naming.provider.url",
                  "jnp://localhost:1099");
        props.put("java.naming.factory.url.pkgs",
                  "org.jboss.naming:org.jnp.interfaces");
        
        return props;
    }

    protected String getUsersPassword() throws LoginException
    {
        String username = getUsername();
        String password = null;
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            Properties props = getProperties();
            InitialContext ctx = new InitialContext(props);
            DataSource ds = (DataSource) ctx.lookup(dsJndiName);
            conn = ds.getConnection();
            
            ps = conn.prepareStatement(principalsQuery);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next() == false) {
                throw new FailedLoginException("No matching username found " +
                                               "in principals");
            }
            password = rs.getString(1);
            rs.close();
        } catch (NamingException ex) {
            throw new LoginException(ex.toString(true));
        } catch (SQLException ex) {
            throw new LoginException(ex.toString());
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                }
            }
        }
        return password;
    }

    // Return an emtpy set of roles
    protected Group[] getRoleSets() throws LoginException
    {
        SimpleGroup roles = new SimpleGroup("Roles");
        Group[] roleSets = {roles};
        return roleSets;
    }
}
