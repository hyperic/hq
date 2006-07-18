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

package org.hyperic.hq.authz.server.entity;

import java.sql.SQLException;
import javax.ejb.CreateException;
import javax.ejb.EntityContext;
import javax.naming.NamingException;
import org.hyperic.hq.authz.shared.AuthzSubjectLocal;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.jdbc.IDGeneratorFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AuthzEntity {
    public static final String ctx = AuthzEntity.class.getName();
    protected Log log = LogFactory.getLog(AuthzEntity.class);
    public AuthzEntity() {}
    private AuthzSubjectLocal whoami = null;
    protected EntityContext entityContext = null;

    /**
     * Id of this EJB
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @ejb:pk
     * @ejb:pk-field
     * @ejb:transaction type="NOTSUPPORTED"
     * @jboss:read-only true
     */
    public abstract Integer getId();
    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setId(Integer id);

    protected void ejbCreate(String ctx, String sequence) throws CreateException {
        try {
            // first we get a new id from the id generator
            Integer id = new Integer((int) IDGeneratorFactory.getNextId(ctx, 
                sequence, AuthzConstants.authzDS));
            this.setId(id);
            if (log.isDebugEnabled()) {
                log.debug("Completed base ejbCreate");
            }
        } catch (NamingException e) {
            log.error("Naming Exception occured in " +
                      "AutzhEntityBean.ejbCreate(): " + e.getMessage());
            throw new CreateException("Naming Exception occured in " +
                                      "AuthzEntity.ejbCreate(): " +
                                      e.getMessage());
        } catch (SQLException e) {
            log.error("SQL Exception occured in " +
                      "AutzhEntityBean.ejbCreate(): " + e.getMessage());
            throw new CreateException("SQL Exception occured in " +
                                      "AuthzEntity.ejbCreate(): " +
                                      e.getMessage());
        } catch (ConfigPropertyException e) {
            log.error("Config Property Exception occured in " +
                      "AutzhEntityBean.ejbCreate(): " + e.getMessage());
            throw new CreateException("Config Property Exception occured in " +
                                      "AuthzEntity.ejbCreate(): " +
                                      e.getMessage());
        }

    }

    /**
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public AuthzSubjectLocal getWhoami() {
        return whoami;
    }    

    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void setWhoami(AuthzSubjectLocal newWhoami) {
        whoami = newWhoami;
    }

    protected EntityContext getEntityContext() {
        return entityContext;
    }

    public void setEntityContext(EntityContext ctx) {
        entityContext = ctx;
    }

    public void unsetEntityContext() {
        entityContext = null;
    }

}
