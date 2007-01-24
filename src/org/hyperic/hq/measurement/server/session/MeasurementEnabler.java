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

package org.hyperic.hq.measurement.server.session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.util.LoggingThreadGroup;

import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;

class MeasurementEnabler {
    private static Log _log = LogFactory.getLog(MeasurementEnabler.class);
    private static final Object INIT_LOCK = new Object();
    private static MeasurementEnabler INSTANCE;

    private final ThreadGroup _tGroup   = new LoggingThreadGroup("Enabler");
    private BlockingQueue  _enableQueue = new LinkedBlockingQueue(); 
    private QueueReader    _reader      = new QueueReader();
    
    private MeasurementEnabler() {
    }

    public static MeasurementEnabler getInstance() {
        synchronized (INIT_LOCK) {
            if (INSTANCE == null) {
                Thread t;
                
                INSTANCE = new MeasurementEnabler();
                t = new Thread(INSTANCE._tGroup, INSTANCE._reader,
                               "MeasurementEnabler");
                t.start();
            }
        }
        return INSTANCE;
    }
    
    private static class ScheduleInfo {
        private AppdefEntityID     _id;
        private AuthzSubjectValue  _subject;
    }
    
    private class QueueReader implements Runnable {
        public void run() {
            while (true) {
                ScheduleInfo info;
                
                try {
                    info = (ScheduleInfo)_enableQueue.take();
                } catch(InterruptedException e) {
                    _log.warn("Unable to take item off queue", e);
                    continue;
                }
    
                _log.info("Enabling default metrics for [" + info._id + "]");
                try {
                    DerivedMeasurementManagerEJBImpl.getOne()
                        .enableDefaultMetrics(info._subject, info._id);
                } catch(Exception e) {
                    _log.warn("Unable to enable default metrics", e);
                }
            }
        }
    }

    static void enableDefaultMetrics(AuthzSubjectValue subject,
                                     AppdefEntityID id)
    {
        ScheduleInfo info = new ScheduleInfo();
        
        info._id      = id;
        info._subject = subject;
        
        try {
            getInstance()._enableQueue.put(info);
        } catch(InterruptedException e) {
            _log.warn("Unable to enqueue metrics enable for [" + id + "]", e);
        }
    }
}
