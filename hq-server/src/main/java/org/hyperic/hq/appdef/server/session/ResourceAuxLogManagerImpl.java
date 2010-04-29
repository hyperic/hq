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

package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.galerts.ResourceAuxLog;
import org.hyperic.hq.appdef.shared.ResourceAuxLogManager;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 */
@org.springframework.stereotype.Service
@Transactional
public class ResourceAuxLogManagerImpl implements ResourceAuxLogManager {
    private ResourceAuxLogDAO resourceAuxLogDao;

    @Autowired
    public ResourceAuxLogManagerImpl(ResourceAuxLogDAO resourceAuxLogDao) {
        this.resourceAuxLogDao = resourceAuxLogDao;
    }

    /**
     */
    public ResourceAuxLogPojo create(GalertAuxLog log, ResourceAuxLog logInfo) {
        ResourceAuxLogPojo resourceLog = new ResourceAuxLogPojo(log, logInfo, log.getAlert().getAlertDef());

        resourceAuxLogDao.save(resourceLog);
        return resourceLog;
    }

    /**
     */
    public void remove(GalertAuxLog log) {
        resourceAuxLogDao.remove(resourceAuxLogDao.find(log));
    }

    /**
     */
    @Transactional(readOnly=true)
    public ResourceAuxLogPojo find(GalertAuxLog log) {
        return resourceAuxLogDao.find(log);
    }

    /**
     */
    public void removeAll(GalertDef def) {
        resourceAuxLogDao.removeAll(def);
    }
}
