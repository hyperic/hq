/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.common.shared;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.common.server.session.Crispo;
import org.hyperic.hq.common.server.session.CrispoOption;
import org.hyperic.util.config.ConfigResponse;

/**
 * Local interface for CrispoManager.
 */
public interface CrispoManager {
    /**
     * Create a new {@link Crispo} from a {@link Map} of {@link String}
     * key/value pairs
     */
    public Crispo createCrispo(Map<String, String> keyVals);

    /**
     * Create a new {@link Crispo} from a {@link Map} of {@link String}
     * key/value pairs
     * @param override will insert the key/vals even if the val == null or val.length() == 0
     * <p>XXX not sure why the original design decision was to completely ignore key/vals
     * where the val == null || val.length() == 0, but didn't want to completely change it
     */
    public Crispo createCrispo(Map<String, String> keyVals, boolean override);

    public Collection<Crispo> findAll();

    public Crispo findById(Integer id);

    /**
     * Delete a {@link Crispo} and all the options contained within.
     */
    public void deleteCrispo(Crispo c);

    /**
     * Create a new Crispo, filled out with the values from a
     * {@link ConfigResponse}
     */
    public Crispo create(ConfigResponse cfg);

    /**
     * Update a crispo, matching the saved crispo to the values in the config
     * repsonse.
     */
    public void update(Crispo c, ConfigResponse cfg);

    /**
     * Find a List of CrispoOptions given the search key.
     * @param key The key to search for
     * @return A list of CrispoOptions that have a key that matches in whole or
     *         part the given key parameter.
     */
    public List<CrispoOption> findOptionByKey(String key);

    /**
     * Find a List of CrispoOptions given the search value.
     * @param val The value to search for
     * @return A list of CrispoOptions that have a value (in the array) that
     *         matches
     */
    public List<CrispoOption> findOptionByValue(String val);

    /**
     * Update the given CrispoOption with the given value.
     * @param o The CrispoOption to update
     * @param val The new value for this option
     */
    public void updateOption(CrispoOption o, String val);

}
