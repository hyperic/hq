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

package org.hyperic.hq.appdef.server.entity;

import java.sql.SQLException;
import java.util.Properties;

import javax.ejb.CreateException;
import javax.ejb.EJBLocalObject;
import javax.ejb.EntityContext;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.jdbc.IDGeneratorFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the parent abstract class of all appdef beans
 * Notice that it itself does not implement EntityBean, this
 * is not a coincidence :)
 */

public abstract class AppdefEntityBean {

    public static final String ctx = AppdefEntityBean.class.getName();
    public static final String DATASOURCE_NAME = HQConstants.DATASOURCE;
    protected Log log = LogFactory.getLog("org.hyperic.hq.appdef.server.entity.AppdefEntityBean");

    protected EntityContext entityContext = null;

    public AppdefEntityBean() {}

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
    public abstract java.lang.Integer getId();
    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     */
    public abstract void setId(java.lang.Integer id);

    /**
     * MTime of this EJB
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     * @ejb:transaction type="SUPPORTS"
     */
    public abstract java.lang.Long getMTime();
    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     */
    public abstract void setMTime(java.lang.Long mtime);

    /**
     * CTime of this EJB
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     * @ejb:transaction type="SUPPORTS"
     */
    public abstract java.lang.Long getCTime();
    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     */
    public abstract void setCTime(java.lang.Long ctime);

    /**
     * @ejb:interface-method
     */
    public abstract javax.ejb.EJBLocalObject getAppdefResourceType();
    
    /**
     * A private method which performs the default create operations
     * for all Appdef Entity Beans. Note that while its called ejbCreate
     * it does not conform to the standard convention for EJB's.
     * @param ctx - the logging context
     * @param sequence - the name of the sequence to be used for obtaining
     * the next id for this object
     */
    protected void ejbCreate(String ctx, String sequence) 
        throws CreateException {
        try {
            if(log.isDebugEnabled()) {
                log.debug("Begin base ejbCreate");
            }
            // first we get a new id from the id generator
            Integer id = new Integer((int) IDGeneratorFactory.getNextId(ctx, 
                sequence, DATASOURCE_NAME));
            this.setId(id);
            // now we set mtime and ctime
            this.setMTime(new Long(System.currentTimeMillis()));
            this.setCTime(new Long(System.currentTimeMillis()));
            if(log.isDebugEnabled()) {
                log.debug("Completed base ejbCreate");
            }
        } catch (NamingException e) {
            log.error(
                "Naming Exception occured in ejbCreate()" + e);
            throw new CreateException(
                "Naming Exception occured in ejbCreate()" + e);
        } catch (SQLException e) {
            log.error(
                "SQLException occured in ejbCreate()" + e);
            throw new CreateException(
                "SQLException occured in ejbCreate()" + e);
        } catch (ConfigPropertyException e) {
            log.error(
                "ConfigPropertyException occured in ejbCreate(): " + e);
            throw new CreateException(
                "ConfigPropertyException occured in ejbCreate(): " + e);
        }
    }

    /**
     * Get an initialized properties object which corresponds
     * to the custom properties defined for a particular class
     * @param Class - class for which you want the properties set
     * @return Properties - a Properties object with all the 
     * appropriate keys initialized.
     */
    public Properties getCustomProperties(Class aClass) {
        Properties props = new Properties();
        if(aClass.equals(org.hyperic.hq.appdef.server.entity.ApplicationEJBImpl.class)) {
            props.put("Owner", new String());
            props.put("Location", new String());
        }
        // etc. etc.
        return props;
    }    

    public boolean matchesValueObject(AppdefResourceValue obj) {
        boolean matches = true;
        if (obj.getId() != null) {
            matches = (obj.getId().intValue() == this.getId().intValue());
        } else {
            matches = (this.getId() == null);
        }
        if (obj.getCTime() != null) {
            matches = (obj.getCTime().floatValue() == this.getCTime().floatValue());
        } else {
            matches = (this.getCTime() == null);
        }
        return matches;
    }

    protected void rollback() {
        if(!this.getEntityContext().getRollbackOnly()) {
            this.getEntityContext().setRollbackOnly();
        }
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

    /**
     * utility method to get a reference to the *this* equivalent
     * of the entity bean. It needs to be cast to the appropriate 
     * Local Interface class of the corresponding bean.
     */
    protected EJBLocalObject getSelfLocal() {
        return getEntityContext().getEJBLocalObject();
    }

}
