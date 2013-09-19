/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2004-2011], VMware, Inc.
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
 */

package org.hyperic.hq.common.shared;

import java.sql.BatchUpdateException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.StaleStateException;
import org.hibernate.exception.GenericJDBCException;
import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
public class TransactionRetry {
    
    private static final Log log = LogFactory.getLog(TransactionRetry.class);
    
    /**
     * Utility method for running transactions that will retry in case of a
     * {@link HibernateOptimisticLockingFailureException}.
     * This method should be run from outside a transaction context going into a transactional
     * context or else it is pretty useless
     * @param runner - {@link Runner} will execute runner.run()
     * @param maxRetries - max number of retries when
     * {@link HibernateOptimisticLockingFailureException} occurs
     * @param sleepTime - sleepTime in ms between retries
     */
    public void runTransaction(Runnable runner, int maxRetries, long sleepTime) {
        RuntimeException ex = null;
        int tries = 0;
        while (tries++ < maxRetries) {
            try {
                log.debug("running " + runner + " for try number " + tries);
                runner.run();
                ex = null;
                break;
            } catch (GenericJDBCException e) {
                Throwable cause = e.getCause();
                if (cause != null && cause instanceof BatchUpdateException) {
                    ex = e;
                    if (tries < maxRetries) {
                        log.warn("retrying operation thread=" + Thread.currentThread().getName()+
                                 ", tries=" + tries + " error: " + e);
                    }
                    log.debug(e,e);
                } else {
                    throw e;
                }
            } catch (StaleStateException e) {
                ex = e;
                if (tries < maxRetries) {
                    log.warn("retrying operation thread=" + Thread.currentThread().getName()+
                             ", tries=" + tries + " error: " + e);
                }
                log.debug(e, e);
            } catch (HibernateOptimisticLockingFailureException e) {
                ex = e;
                if (tries < maxRetries) {
                    log.warn("retrying operation thread=" + Thread.currentThread().getName()+
                             ", tries=" + tries + " error: " + e);
                }
                log.debug(e, e);
            }
            try {
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
            } catch (InterruptedException e) {
                log.debug(e,e);
            }
        }
        if (ex != null) {
            throw ex;
        }
    }

}
