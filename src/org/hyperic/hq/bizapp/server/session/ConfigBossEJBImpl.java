/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.bizapp.server.session;

import java.util.Properties;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.bizapp.shared.ConfigBossLocal;
import org.hyperic.hq.bizapp.shared.ConfigBossUtil;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.server.session.ServerConfigManagerEJBImpl;
import org.hyperic.hq.common.shared.ServerConfigManagerLocal;
import org.hyperic.hq.product.server.MBeanUtil;
import org.hyperic.util.ConfigPropertyException;

/**
 * A boss to provide bizapp client access to the configuration settings
 * @ejb:bean name="ConfigBoss"
 *      jndi-name="ejb/bizapp/ConfigBoss"
 *      local-jndi-name="LocalConfigBoss"
 *      view-type="both"
 *      type="Stateless"
 * @ejb:transaction type="Required"
 */
public class ConfigBossEJBImpl 
    extends BizappSessionEJB
    implements SessionBean 
{
    private SessionManager sessionManager = SessionManager.getInstance();

    private ServerConfigManagerLocal getCfgMan() {
        return ServerConfigManagerEJBImpl.getOne();
    }

    /**
     * Get the top-level configuration properties
     * @ejb:interface-method
     */
    public Properties getConfig() throws ConfigPropertyException {
        return getCfgMan().getConfig();
    }

    /**
     * Get the configuration properties for a specified prefix
     * @ejb:interface-method
     */
    public Properties getConfig(String prefix) throws ConfigPropertyException
    {
        return getCfgMan().getConfig(prefix);
    }

    /**
     * Set the top-level configuration properties
     * @ejb:interface-method
     */
    public void setConfig(int sessId, Properties props) 
        throws ApplicationException, ConfigPropertyException  
    {
        AuthzSubject subject = 
            SessionManager.getInstance().getSubject(sessId);
        getCfgMan().setConfig(subject, props);
    }

    /**
     * Set the configuration properties for a prefix
     * @ejb:interface-method
     */
    public void setConfig(int sessId, String prefix, Properties props) 
        throws ApplicationException, ConfigPropertyException 
    {
        AuthzSubject subject = 
            SessionManager.getInstance().getSubject(sessId);
        getCfgMan().setConfig(subject, prefix, props);
    }

    /**
     * Restart the config Service
     * @ejb:interface-method
     */
    public void restartConfig() {
        try {
            MBeanServer server = MBeanUtil.getMBeanServer();

            ObjectName objName =
                new ObjectName("hyperic.jmx:type=Service,name=ProductConfig");
            server.invoke(objName, "restart", new Object[] {},
                          new String[] {});
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    /**
     * Perform routine database maintenance.  Must have admin permissions.
     * @return The time it took to vaccum, in milliseconds, or -1 if the 
     *         database is not PostgreSQL.
     * @ejb:interface-method
     */
    public long vacuum (int sessionId)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        PermissionManager pm = PermissionManagerFactory.getInstance();
        if (!pm.hasAdminPermission(subject.getId())) {
            throw new PermissionException("Only admins can vacuum the DB");
        }
        return getServerConfigManager().vacuum();
    }
    
    public static ConfigBossLocal getOne() {
        try {
            return ConfigBossUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public void ejbCreate() { }
    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
    public void setSessionContext(SessionContext c) {}
}
