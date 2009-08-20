package org.hyperic.tools.ant.dbupgrade;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.AlertFiredEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.server.session.AlertConditionEvaluatorStateRepository;
import org.hyperic.hq.events.server.session.FileAlertConditionEvaluatorStateRepository;
import org.hyperic.util.jdbc.DBUtil;

/**
 * Schema upgrade to new 4.2 alerting subsystem. TriggerFiredEvents representing
 * the last recoverable alert stored by old "MultiConditionTriggers" must now be
 * written to AlertConditionEvaluatorStates file for initialization of new
 * RecoveryConditionEvaluator objects.
 * @author jhickey
 *
 */
public class RecoveryConditionExportSchemaSpecTask
    extends SchemaSpecTask
{

    private AlertConditionEvaluatorStateRepository stateRepository;

    public void setExportDir(String exportDir) {
        log("Setting Recovery Condition exportDir to " + exportDir);
        this.stateRepository = new FileAlertConditionEvaluatorStateRepository(new File(exportDir));
    }

    public void execute() throws BuildException {
        log("Exporting Recovery Alert State");
        Map lastAlertTriggersFired = loadAlertTriggerFiredEventsFromExistingMultiConditions();
        stateRepository.saveAlertConditionEvaluatorStates(lastAlertTriggersFired);
        log("Finished exporting Recovery Alert state.  " + lastAlertTriggersFired.size() +
            " recovery alert definitions should be initialized with active alert state.");
    }

    private AbstractEvent getTriggerFiredEvent(byte[] eventObject) throws ClassNotFoundException, IOException {
        ByteArrayInputStream istream = new ByteArrayInputStream(eventObject);
        ObjectInput _eventObjectInput = new ObjectInputStream(istream);
        return (AbstractEvent) _eventObjectInput.readObject();
    }

    private Map loadAlertTriggerFiredEventsFromExistingMultiConditions() throws BuildException {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            String selectSQL =
                "SELECT rt.alert_definition_id, te.event_object " +
                "FROM EAM_TRIGGER_EVENT te, EAM_REGISTERED_TRIGGER rt WHERE " +
                "rt.classname='org.hyperic.hq.bizapp.server.trigger.conditional.MultiConditionTrigger' " +
                "AND rt.id=te.trigger_id";
            stmt = conn.createStatement();
            rs = stmt.executeQuery(selectSQL);
            Map lastAlerts = new HashMap();
            while (rs.next()) {
                processNext(rs, lastAlerts);
            }
            return lastAlerts;
        } catch (Exception e) {
            throw new BuildException(e.getMessage(), e);
        } finally {
            DBUtil.closeJDBCObjects(RecoveryConditionExportSchemaSpecTask.class.getName(), null, stmt, rs);
        }
    }

    private void processNext(ResultSet rs, Map lastAlerts) {
        int alertDefinitionId = -1;
        try {
            alertDefinitionId = rs.getInt(1);
        } catch (Exception e) {
            log("Error processing result set.  A recovery condition may not be " +
                "initialized with previous alert state.  Cause: " + e.getMessage());
            return;
        }
        try {
            byte[] eventObject = rs.getBytes(2);
            AbstractEvent event = getTriggerFiredEvent(eventObject);
            if (event instanceof TriggerFiredEvent) {
                TriggerFiredEvent triggerFired = (TriggerFiredEvent) event;
                AbstractEvent[] multiConditionEvents = triggerFired.getEvents();
                for (int i = 0; i < multiConditionEvents.length; i++) {
                    if (multiConditionEvents[i] instanceof AlertFiredEvent) {
                        lastAlerts.put(Integer.valueOf(alertDefinitionId), triggerFired);
                    }
                }
            }
        } catch (Exception e) {
            log("Error processing result set.  Recovery alert " + alertDefinitionId +
                " will not be initialized with previous alert state.  Cause: " + 
                e.getMessage());
        }
    }

}
