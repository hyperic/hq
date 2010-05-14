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

package org.hyperic.hq.galerts.server.session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.galerts.shared.GtriggerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 */
@Service
@Transactional
public class GtriggerManagerImpl implements GtriggerManager {
    private final Log log = LogFactory.getLog(GtriggerManagerImpl.class);

    private GtriggerTypeInfoDAO gtriggerTypeInfoDao = Bootstrap.getBean(GtriggerTypeInfoDAO.class);

    @Autowired
    public GtriggerManagerImpl(GtriggerTypeInfoDAO gtriggerTypeInfoDao) {
        this.gtriggerTypeInfoDao = gtriggerTypeInfoDao;
    }

    /**
     */
    @Transactional(readOnly=true)
    public GtriggerTypeInfo findTriggerType(GtriggerType type) {
        return gtriggerTypeInfoDao.find(type);
    }

    /**
     * Register a trigger type.
     * 
     * @param triggerType Trigger type to register
     * @return the persisted metadata about the trigger type
     */
    public GtriggerTypeInfo registerTriggerType(GtriggerType triggerType) {
        GtriggerTypeInfo res;

        res = gtriggerTypeInfoDao.find(triggerType);
        if (res != null) {
            log.warn("Attempted to register GtriggerType class [" +
                     triggerType.getClass() + "] but it was already " +
                     "registered");
            return res;
        }
        res = new GtriggerTypeInfo(triggerType.getClass());
        gtriggerTypeInfoDao.save(res);
        return res;
    }

    /**
     * Unregister a trigger type. This method will fail if any alert
     * definitions are using triggers of this type.
     * 
     * @param triggerType Trigger type to unregister
     */
    public void unregisterTriggerType(GtriggerType triggerType) {
        GtriggerTypeInfo info = gtriggerTypeInfoDao.find(triggerType);

        if (info == null) {
            log.warn("Tried to unregister a trigger type which was not registered");
            return;
        }

        gtriggerTypeInfoDao.remove(info);
    }
}
