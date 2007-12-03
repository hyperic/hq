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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.hyperic.dao.DAOFactory;

import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.server.session.Crispo;
import org.hyperic.hq.common.server.session.CrispoOption;
import org.hyperic.hq.common.shared.CrispoManagerLocal;
import org.hyperic.hq.common.shared.CrispoManagerUtil;
import org.hyperic.util.config.ConfigResponse;

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
    private CrispoDAO getCrispoDAO() {
        return DAOFactory.getDAOFactory().getCrispoDAO();
    }

    private CrispoOptionDAO getCrispoOptionDAO() {
        return DAOFactory.getDAOFactory().getCrispoOptionDAO();
    }
    /**
     * Create a new {@link Crispo} from a {@link Map} of {@link String}
     * key/value pairs
     * 
     * @ejb:interface-method
     */
    public Crispo createCrispo(Map keyVals) {
        Crispo c = Crispo.create(keyVals);
        
        getCrispoDAO().save(c);
        return c;
    }
    
    /** 
     * @return all the {@link Crispo}s in the system
     * @ejb:interface-method
     */
    public Collection findAll() {
        return getCrispoDAO().findAll();
    }

    /**
     * @ejb:interface-method
     */
    public Crispo findById(Integer id) {
        return getCrispoDAO().findById(id);
    }
    
    /**
     * Delete a {@link Crispo} and all the options contained within.
     * 
     * @ejb:interface-method
     */
    public void deleteCrispo(Crispo c) {
        getCrispoDAO().remove(c);
    }

    /**
     * Create a new Crispo, filled out with the values from a 
     * {@link ConfigResponse}
     * 
     * @ejb:interface-method
     */
    public Crispo create(ConfigResponse cfg) {
        Crispo res = Crispo.create(cfg);
        getCrispoDAO().save(res);
        return res;
    }
    
    /**
     * Update a crispo, matching the saved crispo to the values in the
     * config repsonse. 
     * 
     * @ejb:interface-method
     */
    public void update(Crispo c, ConfigResponse cfg) {
        c.updateWith(cfg);
        getCrispoDAO().save(c);
    }

    /**
     * Find a List of CrispoOptions given the search key.
     *
     * @param key The key to search for
     * @return A list of CrispoOptions that have a key that matches in whole
     * @ejb:interface-method
     * or part the given key parameter.
     */
    public List findOptionByKey(String key) {
        return getCrispoOptionDAO().findOptionsByKey(key);    
    }

    /**
     * Update the given CrispoOption with the given value.
     *
     * @param o The CrispoOption to update
     * @param val The new value for this option
     * @ejb:interface-method 
     */
    public void updateOption(CrispoOption o, String val) {
        if (val == null || val.matches("^\\s*$")) {
            getCrispoOptionDAO().remove(o);
            Collection opts = o.getCrispo().getOptsSet();
            opts.remove(o);
        } else {
            o.setValue(val);
            getCrispoOptionDAO().save(o);
        }
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
