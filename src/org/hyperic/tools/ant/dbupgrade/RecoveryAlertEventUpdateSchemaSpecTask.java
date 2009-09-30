package org.hyperic.tools.ant.dbupgrade;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.AlertFiredEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.util.jdbc.DBUtil;



/**
 * Schema upgrade to new 4.2 alerting subsystem. AlertFiredEvents representing
 * the last recoverable alert stored by old "MultiConditionTriggers" must be updated with INSTANCE_ID in the EAM_EVENT_LOG table.
 * This must be done so they will be properly obtained by RecoveryConditionEvaluator on server startup
 * @author jhickey
 *
 */
public class RecoveryAlertEventUpdateSchemaSpecTask
    extends SchemaSpecTask
{

  
    public void execute() throws BuildException {
        log("Updating recoverable alert events");
        List lastAlertTriggersFired = loadAlertTriggerFiredEventsFromExistingMultiConditions();
        for(Iterator iterator = lastAlertTriggersFired.iterator();iterator.hasNext();) {
            AlertFiredEvent event = (AlertFiredEvent)iterator.next();
            updateLogEventInstanceId(event);
        }
        log("Finished updating recoverable alert events.  " + lastAlertTriggersFired.size() +
            " recovery alert definitions should be initialized with active alert state.");
    }
    
    private String getResourceQuery(AppdefEntityID aeid) {
        StringBuffer selectSQL = new StringBuffer();
        final Integer id = aeid.getId();
        switch (aeid.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_SERVER :
                selectSQL.append("SELECT RESOURCE_ID from EAM_SERVER where ID=").append(id);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                selectSQL.append("SELECT RESOURCE_ID from EAM_PLATFORM where ID=").append(id);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE :
                selectSQL.append("SELECT RESOURCE_ID from EAM_SERVICE where ID=").append(id);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                selectSQL.append("SELECT ID from EAM_RESOURCE where INSTANCE_ID=").append(aeid.getAuthzTypeId()).
                append(" AND RESOURCE_TYPE_ID=").append(id);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                selectSQL.append("SELECT RESOURCE_ID from EAM_APPLICATION where ID=").append(id);
                break;
            default:
                selectSQL.append("SELECT ID from EAM_RESOURCE where INSTANCE_ID=").append(aeid.getAuthzTypeId()).
                append(" AND RESOURCE_TYPE_ID=").append(id);
        }
        return selectSQL.toString();
    }
    
    private void updateLogEventInstanceId(AlertFiredEvent lastAlertFired) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            String updateSQL = "UPDATE EAM_EVENT_LOG set INSTANCE_ID=" + lastAlertFired.getInstanceId() + 
            " WHERE TYPE='org.hyperic.hq.events.AlertFiredEvent' AND DETAIL='" + lastAlertFired.toString() + 
            "' AND SUBJECT='" + lastAlertFired.getSubject() + 
            "' AND RESOURCE_ID in (" + getResourceQuery(lastAlertFired.getResource()) + ")";
            stmt = conn.createStatement();
            int rowsUpdated = stmt.executeUpdate(updateSQL);
            if(rowsUpdated == 0) {
                log("Could not find log event corresponding to an unfixed alert using query '" + updateSQL +
                         "'.  This could be due to event log purging.  An initial recovery alert for this alert may not fire.");
            }
        } catch (Exception e) {
            throw new BuildException(e.getMessage(), e);
        } finally {
            DBUtil.closeJDBCObjects(RecoveryAlertEventUpdateSchemaSpecTask.class.getName(), null, stmt, rs);
        }   
    }

    private AbstractEvent getTriggerFiredEvent(byte[] eventObject) throws ClassNotFoundException, IOException {
        ByteArrayInputStream istream = new ByteArrayInputStream(eventObject);
        ObjectInput _eventObjectInput = new ObjectInputStream(istream);
        return (AbstractEvent) _eventObjectInput.readObject();
    }

    private List loadAlertTriggerFiredEventsFromExistingMultiConditions() throws BuildException {
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
            List lastAlerts = new ArrayList();
            while (rs.next()) {
                processNext(rs, lastAlerts);
            }
            return lastAlerts;
        } catch (Exception e) {
            throw new BuildException(e.getMessage(), e);
        } finally {
            DBUtil.closeJDBCObjects(RecoveryAlertEventUpdateSchemaSpecTask.class.getName(), null, stmt, rs);
        }
    }

    private void processNext(ResultSet rs, List lastAlerts) {
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
                        lastAlerts.add(multiConditionEvents[i]);
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
