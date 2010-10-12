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

package org.hyperic.tools.ant.dbupgrade;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.hyperic.hq.events.AlertConditionEvaluatorStateRepository;
import org.hyperic.hq.events.FileAlertConditionEvaluatorStateRepository;
import org.hyperic.util.jdbc.DBUtil;

/**
 * Schema upgrade to new 4.2 alerting subsystem. Expirations stored by old
 * "CounterTriggers" must now be written to ExecutionStrategyStates file for
 * initialization of new CounterExecutionStrategy objects. We will not bother
 * writing out expiration times that are already less than current time (though
 * server would handle old expirations properly if we did.
 * CounterExecutionStrategy objects clear expired dates from list before
 * processing)
 * @author jhickey
 *
 */
public class CounterExportSchemaSpecTask
    extends SchemaSpecTask
{

    private AlertConditionEvaluatorStateRepository stateRepository;

    public void setExportDir(String exportDir) {
        log("Setting Alert Counter exportDir to " + exportDir);
        this.stateRepository = new FileAlertConditionEvaluatorStateRepository(new File(exportDir));
    }

    public void execute() throws BuildException {
        log("Exporting Alert Counters");
        Map lastAlertExpirations = loadExpirationsFromExistingCounterTriggers();
        stateRepository.saveExecutionStrategyStates(lastAlertExpirations);
        log("Finished exporting Alert Counters.  " + lastAlertExpirations.size() +
            " alert definitions should be initialized with previous count.");
    }

    private Map loadExpirationsFromExistingCounterTriggers() throws BuildException {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            String selectSQL =
                "SELECT rt.alert_definition_id, te.expiration " +
                "FROM EAM_TRIGGER_EVENT te, EAM_REGISTERED_TRIGGER rt WHERE " +
                "rt.classname='org.hyperic.hq.bizapp.server.trigger.frequency.CounterTrigger' " +
                "AND te.expiration > " +
                System.currentTimeMillis() + " and rt.id=te.trigger_id";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(selectSQL);
            Map lastAlertExpirations = new HashMap();
            while (rs.next()) {
                processNext(rs, lastAlertExpirations);
            }
            return lastAlertExpirations;
        } catch (Exception e) {
            throw new BuildException(e.getMessage(), e);
        } finally {
            DBUtil.closeJDBCObjects(CounterExportSchemaSpecTask.class.getName(), null, stmt, rs);
        }
    }

    private void processNext(ResultSet rs, Map lastAlertExpirations) {
        int alertDefinitionId = -1;
        try {
            alertDefinitionId = rs.getInt(1);
        } catch (Exception e) {
            log("Error processing result set.  An alert counter may not be initialized.  Cause: " + e.getMessage());
            return;
        }
        try {
            long expiration = rs.getLong(2);
            List expirations = (List) lastAlertExpirations.get(Integer.valueOf(alertDefinitionId));
            if (expirations == null) {
                expirations = new ArrayList();
            }
            expirations.add(Long.valueOf(expiration));
            lastAlertExpirations.put(Integer.valueOf(alertDefinitionId), expirations);
        } catch (Exception e) {
            log("Error processing result set.  Counter for alert " + alertDefinitionId +
                " will be reset to 0.  Cause: " + e.getMessage());
        }
    }
}
