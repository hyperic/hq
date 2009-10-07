package org.hyperic.tools.ant.dbupgrade;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.tools.ant.BuildException;
import org.hyperic.util.jdbc.DBUtil;



/**
 * Schema upgrade to new 4.2 alerting subsystem. AlertFiredEvents representing
 * the last recoverable alert stored by old "MultiConditionTriggers" must be updated with INSTANCE_ID in the EAM_EVENT_LOG table.
 * This must be done so they will be properly obtained by RecoveryConditionEvaluator on server startup
 * @author jhickey
 *
 */
public class AlertEventUpdateSchemaSpecTask
    extends SchemaSpecTask
{
    
    public void execute() throws BuildException {
        log("Updating alert events");
        int eventsUpdated = updateEvents();
        log("Finished updating alert events.  " + eventsUpdated + " events were updated with instance IDs.");
    }
    
    private int updateEvents() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            //Update all events with the alert definition ID.  We match by alert def name, resource id, and match of event timestamp with creation of an alert from that definition.
            //This is done to ensure we are matching the proper alert def, since we are allowed to define an alert against the same resource with the same name.
            String updateSQL = "UPDATE EAM_EVENT_LOG el, EAM_ALERT_DEFINITION ad, EAM_ALERT al SET el.INSTANCE_ID=ad.ID WHERE TYPE='org.hyperic.hq.events.AlertFiredEvent' AND " + 
                "el.SUBJECT=ad.NAME AND ad.RESOURCE_ID=el.RESOURCE_ID AND al.ALERT_DEFINITION_ID=ad.ID AND el.TIMESTAMP=al.CTIME";
            stmt = conn.createStatement();
            int rowsUpdated = stmt.executeUpdate(updateSQL);
            return rowsUpdated;
        } catch (Exception e) {
            throw new BuildException(e.getMessage(), e);
        } finally {
            DBUtil.closeJDBCObjects(AlertEventUpdateSchemaSpecTask.class.getName(), null, stmt, rs);
        }
    }

}
