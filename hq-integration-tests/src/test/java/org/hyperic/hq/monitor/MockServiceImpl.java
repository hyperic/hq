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
package org.hyperic.hq.monitor;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;


/**
 * MockService - some services will implement at least one interface
 * for JDK Dynamic Proxying, and some will not (as of 05/2010)
 *
 * @author Helena Edelson
 */
@Service
@Transactional
public class MockServiceImpl implements MockService {

    private final Log logger = LogFactory.getLog(this.getClass().getName());

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void foo(long duration, Pojo obj) {
        try {

            Thread.sleep(duration);

        } 
        catch (InterruptedException e) {
            logger.warn("Thread interrupted, shutting down.");

        }
        catch (Exception e) {
            logger.warn(e);
        }

        logger.debug("completed " + this.getClass().getName() + ".foo()");
    }

    
}
