/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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
import java.util.List;
import java.util.Map;

import org.hyperic.hq.common.shared.CrispoManager;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The CRISPO (Config Response Is Sweetly Persisted ... Oy!) Manager deals
 * with storing configuration data typically associated with
 * {@link ConfigResponse} objects;
 * 
 */
@Service
@Transactional
public class CrispoManagerImpl implements CrispoManager {
    private CrispoDAO crispoDao;
    private CrispoOptionDAO crispoOptionDao;

    @Autowired
    public CrispoManagerImpl(CrispoDAO crispoDao, CrispoOptionDAO crispoOptionDao) {
        this.crispoDao = crispoDao;
        this.crispoOptionDao = crispoOptionDao;
    }

    /**
     * Create a new {@link Crispo} from a {@link Map} of {@link String}
     * key/value pairs
     */
    public Crispo createCrispo(Map<String, String> keyVals, boolean override) {
        Crispo c = Crispo.create(keyVals, override);
        crispoDao.save(c);
        return c;
    }

    /**
     * Create a new {@link Crispo} from a {@link Map} of {@link String}
     * key/value pairs
     */
    public Crispo createCrispo(Map<String, String> keyVals) {
        Crispo c = Crispo.create(keyVals, false);
        crispoDao.save(c);
        return c;
    }

    /**
     * @return all the {@link Crispo}s in the system
     */
    @Transactional(readOnly=true)
    public Collection<Crispo> findAll() {
        return crispoDao.findAll();
    }

    /**
     */
    @Transactional(readOnly=true)
    public Crispo findById(Integer id) {
        return crispoDao.findById(id);
    }

    /**
     * Delete a {@link Crispo} and all the options contained within.
     */
    public void deleteCrispo(Crispo c) {
        crispoDao.remove(c);
    }

    /**
     * Create a new Crispo, filled out with the values from a
     * {@link ConfigResponse}
     */
    public Crispo create(ConfigResponse cfg) {
        Crispo res = Crispo.create(cfg, false);
        crispoDao.save(res);
        return res;
    }

    /**
     * Update a crispo, matching the saved crispo to the values in the
     * config repsonse.
     */
    public void update(Crispo c, ConfigResponse cfg) {
        c.updateWith(cfg);
        crispoDao.save(c);
    }

    /**
     * Find a List of CrispoOptions given the search key.
     * 
     * @param key The key to search for
     * @return A list of CrispoOptions that have a key that matches in whole
     *         or part the given key parameter.
     */
    @Transactional(readOnly=true)
    public List<CrispoOption> findOptionByKey(String key) {
        return crispoOptionDao.findOptionsByKey(key);
    }

    /**
     * Find a List of CrispoOptions given the search value.
     * 
     * @param val The value to search for
     * @return A list of CrispoOptions that have a value (in the array) that
     *         matches
     */
    @Transactional(readOnly=true)
    public List<CrispoOption> findOptionByValue(String val) {
        return crispoOptionDao.findOptionsByValue(val);
    }

    /**
     * Update the given CrispoOption with the given value.
     * 
     * @param o The CrispoOption to update
     * @param val The new value for this option
     */
    public void updateOption(CrispoOption o, String val) {
        if (val == null || val.matches("^\\s*$")) {
            crispoOptionDao.remove(o);
            Collection<CrispoOption> opts = o.getCrispo().getOptsSet();
            opts.remove(o);
        } else {
            o.setValue(val);
            crispoOptionDao.save(o);
        }
    }
}
