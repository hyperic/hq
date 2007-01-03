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

package org.hyperic.hq.common.server.session;

import java.util.Collection;
import java.util.Map;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.hyperic.dao.DAOFactory;
import org.hyperic.util.config.ConfigResponse;

import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.server.session.Crispo;
import org.hyperic.hq.common.shared.CrispoManagerLocal;
import org.hyperic.hq.common.shared.CrispoManagerUtil;

/**
 * The CRISPO (Config Response Is Sweetly Persisted ... Oy!) Manager deals
 * with storing configuration data typically associated with 
 * {@link ConfigResponse} objects; 
 * 
 * @ejb:bean name="CrispoManager"
 *      jndi-name="ejb/common/CrispoManager"
 *      local-jndi-name="LocalCrispoManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 * @ejb:transaction type="REQUIRED"
 */
public class CrispoManagerEJBImpl implements SessionBean {
    private CrispoDAO getDAO() {
        return DAOFactory.getDAOFactory().getCrispoDAO();
    }
    
    /**
     * Create a new {@link Crispo} from a {@link Map} of {@link String}
     * key/value pairs
     * 
     * @ejb:interface-method
     */
    public Crispo createCrispo(Map keyVals) {
        Crispo c = Crispo.create(keyVals);
        
        getDAO().save(c);
        return c;
    }
    
    /** 
     * @return all the {@link Crispo}s in the system
     * @ejb:interface-method
     */
    public Collection findAll() {
        return getDAO().findAll();
    }

    /**
     * @ejb:interface-method
     */
    public Crispo findById(Integer id) {
        return getDAO().findById(id);
    }
    
    /**
     * Delete a {@link Crispo} and all the options contained within.
     * 
     * @ejb:interface-method
     */
    public void deleteCrispo(Crispo c) {
        getDAO().remove(c);
    }

    /**
     * Create a new Crispo, filled out with the values from a 
     * {@link ConfigResponse}
     * 
     * @ejb:interface-method
     */
    public Crispo create(ConfigResponse cfg) {
        Crispo res = Crispo.create(cfg);
        getDAO().save(res);
        return res;
    }
    
    public static CrispoManagerLocal getOne() {
        try {
            return CrispoManagerUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
    
    public void ejbCreate() { }
    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
    public void setSessionContext(SessionContext c) {}
}
