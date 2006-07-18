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

package org.hyperic.hq.auth.server.entity;

import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.CreateException;

import org.hyperic.util.jdbc.IDGenerator;
import org.hyperic.hq.auth.shared.PrincipalsPK;
import org.hyperic.hq.common.shared.HQConstants;

import org.jboss.security.Util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the PrincipalsBean Implementation.  This is only used to
 * manange principals in our own authentication schema
 * @ejb:bean name="Principals"
 *           jndi-name="ejb/auth/Principals"
 *           local-jndi-name="LocalPrincipals"
 *           view-type="local"
 *           type="CMP"
 *           cmp-version="2.x"
 *
 * @ejb:finder signature="org.hyperic.hq.auth.shared.PrincipalsLocal findByUsername(java.lang.String s)"
 *             query="SELECT OBJECT(m) FROM Principals AS m WHERE m.principal = ?1"
 *             unchecked="true"
 *             result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findAllUsers()"
 *             query="SELECT OBJECT(m) FROM Principals as m"
 *             unchecked="true"
 *             result-type-mapping="Local"
 *
 * @ejb:value-object name="Principals" match="*" instantiation="eager"
 * @ejb:transaction type="SUPPORTS"
 *
 * @jboss:table-name table-name="EAM_PRINCIPAL"
 * @jboss:create-table false
 * @jboss:remove-table false
 */

public abstract class PrincipalsEJBImpl implements EntityBean {

    private final String SEQUENCE_NAME = "EAM_PRINCIPAL_ID_SEQ";

    private final int SEQUENCE_INTERVAL = 10;
    private final String DATASOURCE_NAME = HQConstants.DATASOURCE;

    protected Log log = LogFactory.getLog(
        "org.hyperic.hq.auth.server.entity.PrincipalsEJBImpl");

    private IDGenerator idGen = 
        new IDGenerator(PrincipalsEJBImpl.class.getName(),
                        SEQUENCE_NAME,
                        SEQUENCE_INTERVAL,
                        DATASOURCE_NAME);
    
    public PrincipalsEJBImpl() {}
    
    /**
     * Id of this EJB
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @ejb:pk
     * @ejb:pk-field
     * @jboss:read-only true
     */
    public abstract Integer getId();
    
    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setId(Integer id);

    /**
     * Get the principal name
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:read-only true
     */
    public abstract String getPrincipal();

    /**
     * Set the principal name
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setPrincipal(String principal);

    /**
     * Get the password
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:read-only true
     */
    public abstract String getPassword();
    
    /**
     * Set the password
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setPassword(String password);
    
    /**
     * The create method
     * @param String The principal
     * @param String The password
     * @return PrincipalPK
     * @ejb:create-method
     */
    public PrincipalsPK ejbCreate(String principal, String password) 
        throws CreateException
    {
        // All passwords are stored encrypted
        String passwordHash = Util.createPasswordHash("MD5", "base64",
                                                      null, null, password);

        try {
            Integer id = new Integer((int) idGen.getNewID());

            this.setId(id);
            this.setPrincipal(principal);
            this.setPassword(passwordHash);

        } catch (Exception e) {
            log.error("Exception occured in ejbCreate()" + e);
            throw new CreateException("Exception found in ejbCreate()" + e);
        }

        return null;
    }
    
    public void ejbPostCreate() {}

    public void ejbActivate() {}

    public void ejbPassivate() {}

    public void ejbLoad() {}

    public void ejbStore() {}

    public void setEntityContext(EntityContext ctx) {}
}
