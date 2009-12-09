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

package org.hyperic.hq.ha.server.session;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.application.StartupListener;
import org.hyperic.hq.ha.HAUtil;
import org.hyperic.hq.measurement.server.session.MeasurementStartupListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HAStartupListener
    implements StartupListener, org.hyperic.hq.ha.HAService
{
    private final Log log = LogFactory.getLog(HAStartupListener.class);
    private org.hyperic.hq.ha.server.session.HAService haService;
    
    
    @Autowired
    public HAStartupListener(org.hyperic.hq.ha.server.session.HAService haService) {
        this.haService = haService;
    }

    @PostConstruct
    public void hqStarted() {
        log.info("Starting services");

        startHAService();
        MeasurementStartupListener.startDataPurgeWorker();
        HAUtil.setHAService(this);
    }
    
    public boolean isMasterNode() {
        // HA not implemented in .org
        return true;
    }

    private void startHAService()
    {
        try {
            haService.startSingleton();
        } catch (Exception e) {
            log.info("Unable to start service: "+e);
        }
    }
}
