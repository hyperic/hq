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

package org.hyperic.hq.events.server.session;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.dialect.Dialect;
import org.hyperic.hibernate.dialect.HQDialectUtil;
import org.hyperic.hq.application.Scheduler;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.bizapp.server.action.email.EmailRecipient;
import org.hyperic.hq.bizapp.shared.EmailManager;
import org.hyperic.hq.common.SystemException;
import org.hyperic.util.jdbc.DBUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Schedules the HQ DB Health task
 */
@Service
public class HQDBHealthChecker {

    private static final String BUNDLE = "org.hyperic.hq.events.Resources";
    private static final Object HEALTH_CHECK_LOCK = new Object();
    private static final int HEALTH_CHECK_PERIOD_MILLIS = 15 * 1000;
    private static final int FAILURE_CHECK_PERIOD_MILLIS = 1000;
    private static final int MAX_NUM_OF_FAILURE_CHECKS = 10;

    private final Log log = LogFactory.getLog(HQDBHealthChecker.class);
    private final DBUtil dbUtil;
    private final EmailManager emailManager;
    private final Scheduler scheduler;
    private ScheduledFuture<?> healthCheckerTask ; 

    @Autowired
    public HQDBHealthChecker(DBUtil dbUtil, EmailManager emailManager, Scheduler scheduler) {
        this.dbUtil = dbUtil;
        this.emailManager = emailManager;
        this.scheduler = scheduler;
    }

    @PostConstruct
    public void scheduler() {
        // We want to start the health check only after all plugins
        // have been deployed since this is when the server starts accepting
        // metrics from agents.
        log.info("Scheduling HQ DB Health to perform a health check every " +
                 (HEALTH_CHECK_PERIOD_MILLIS / 1000) + " sec");

        this.healthCheckerTask = scheduler.scheduleAtFixedRate(new HQDBHealthTask(), Scheduler.NO_INITIAL_DELAY,
            HEALTH_CHECK_PERIOD_MILLIS);
    }
    
    @PreDestroy 
    public final void destroy() { 
        this.healthCheckerTask.cancel(true/*mayInterruptIfRunning*/) ; 
    }//EOM 

    private class HQDBHealthTask implements Runnable {

        private final Log _log = LogFactory.getLog(HQDBHealthTask.class);

        private long healthOkStartTime = 0; // time of first OK health check
        private long lastHealthOkTime = 0; // time of last OK health check
        private int numOfHealthCheckFailures = 0;
        private final String HQADMIN_EMAIL_SQL = "SELECT email_address FROM EAM_SUBJECT WHERE id = " +
                                                 AuthzConstants.rootSubjectId;
        private String hqadminEmail = null;

        public void run() {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;

            synchronized (HEALTH_CHECK_LOCK) {
                try {
                    conn = dbUtil.getConnection();
                    stmt = conn.createStatement();

                    if (healthOkStartTime == 0) {
                        healthOkStartTime = pingDatabase(conn, stmt, rs);
                    } else {
                        // get latest email address to send to in case of db
                        // failures
                        rs = stmt.executeQuery(HQADMIN_EMAIL_SQL);

                        if (rs.next()) {
                            hqadminEmail = rs.getString(1);
                        }
                    }
                    recordSuccess();
                } catch (Throwable t) {
                    recordFailure(t);
                    performFailureCheck();
                } finally {
                    DBUtil.closeJDBCObjects(HQDBHealthTask.class, conn, stmt, rs);
                }
            }
        }

        /**
         * Perform failure checks and shutdown HQ if necessary
         */
        private void performFailureCheck() {
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;

            while (healthOkStartTime == 0) {
                try {
                    // wait 1 second before trying
                    Thread.sleep(FAILURE_CHECK_PERIOD_MILLIS);

                    conn = dbUtil.getConnection();
                    stmt = conn.createStatement();

                    healthOkStartTime = pingDatabase(conn, stmt, rs);
                    recordSuccess();
                } catch(SQLException e) {
                    //The DataSource does not throw a unique exception if we timed out waiting for a connection, so this is a temp hack to avoid shutdown
                    //if a connection can't be obtained
                    if(e.getMessage() != null && e.getMessage().contains("Pool empty. Unable to fetch a connection")) {
                        log.warn("No connections available in the connection pool.  " + e.getMessage());
                    } else {
                        handleFailure(e);
                    }
                    
                } catch (Throwable t) {
                   handleFailure(t);
                } finally {
                    DBUtil.closeJDBCObjects(HQDBHealthTask.class, conn, stmt, rs);
                }
            }

        } // end
        
        private void handleFailure(Throwable t) {
            recordFailure(t);

            if (numOfHealthCheckFailures >= MAX_NUM_OF_FAILURE_CHECKS) {
                // shutdown HQ if the database health checks fails
                try {
                    shutdownNotify(t);
                } catch (Throwable t2) {
                    // catch all so that HQ can shutdown
                }
                System.exit(1);
            }
        }

        /**
         * Get database timestamp to check overall database health
         */
        private long pingDatabase(Connection conn, Statement stmt, ResultSet rs)
            throws SQLException {

            Dialect dialect = HQDialectUtil.getDialect(conn);
            rs = stmt.executeQuery(dialect.getCurrentTimestampSelectString());

            if (rs.next()) {
                rs.getString(1);
            }

            return System.currentTimeMillis();
        }

        /**
         * Set fields and log success status
         */
        private void recordSuccess() {
            lastHealthOkTime = System.currentTimeMillis();
            numOfHealthCheckFailures = 0;
            logStatus(null);
        }

        /**
         * Set fields and log failure status
         */
        private void recordFailure(Throwable t) {
            healthOkStartTime = 0;
            numOfHealthCheckFailures++;
            logStatus(t);
        }

        /**
         * Log the status of the current health check
         */
        private void logStatus(Throwable t) {
            if (numOfHealthCheckFailures == 0) {
                _log.debug("HQ DB Health: OK since " + new Date(healthOkStartTime));
            } else {
                String status = "HQ DB Health: Failed. Attempt #" + numOfHealthCheckFailures +
                                ". Last successful check at " + new Date(lastHealthOkTime);

                if (numOfHealthCheckFailures < MAX_NUM_OF_FAILURE_CHECKS) {
                    _log.error(status + ". Checking again in " +
                               (FAILURE_CHECK_PERIOD_MILLIS / 1000) + " sec.", t);
                } else {
                    _log.error(status + ". Shutting down HQ.", t);
                }
            }
        }

        /**
         * email notify of shutdown
         */
        private void shutdownNotify(Throwable t) {
            try {
                InternetAddress addr = new InternetAddress(hqadminEmail);
                EmailRecipient rec = new EmailRecipient(addr, false);

                EmailRecipient[] addresses = new EmailRecipient[] { rec };

                String[] body = new String[addresses.length];
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                t.printStackTrace(pw);

                StringBuffer sb = new StringBuffer();
                MessageFormat messageFormat = new MessageFormat((ResourceBundle.getBundle(BUNDLE)
                    .getString("event.hqdbhealth.email.message")));
                messageFormat.format(new String[] { new Date().toString(), sw.toString() }, sb,
                    null);

                Arrays.fill(body, sb.toString());

                emailManager.sendEmail(addresses, ResourceBundle.getBundle(BUNDLE).getString(
                    "event.hqdbhealth.email.subject"), body, null, null);
            } catch (AddressException e) {
                _log.error("Invalid email address: " + hqadminEmail);
            } catch (SystemException e) {
                _log.error("HQ services not available for sending emails");
            }
        }

    }

}
