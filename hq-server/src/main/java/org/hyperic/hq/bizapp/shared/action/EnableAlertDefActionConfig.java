package org.hyperic.hq.bizapp.shared.action;

import org.hyperic.hq.events.ActionConfigInterface;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.IntegerConfigOption;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;

/**
 * Shared configs for enable alert definition action
 */
public class EnableAlertDefActionConfig implements ActionConfigInterface {
    public static final String CFG_ID  = "id";

    /** Holds value of property alertDefId. */
    private Integer alertDefId;
    
    private String implementor =
        "com.hyperic.hq.bizapp.server.action.alert.EnableAlertDefAction";

    /** Creates a new instance of SharedEmailAction */
    public EnableAlertDefActionConfig() {
    }
       
    public ConfigSchema getConfigSchema() {
        IntegerConfigOption id;
        ConfigSchema res = new ConfigSchema();

        final int MIN_ID = 10001;
        
        // Determine the ID of the alert definition
        id = new IntegerConfigOption(
            CFG_ID, "Alert Definition ID", new Integer(MIN_ID));
        id.setMinValue(MIN_ID);
        res.addOption(id);

        return res;
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.events.ext.ActionInterface#getConfigResponse()
     */
    public ConfigResponse getConfigResponse()
        throws InvalidOptionException, InvalidOptionValueException {
        ConfigResponse response = new ConfigResponse();
        response.setValue(CFG_ID, String.valueOf(this.getAlertDefId()));
        return response;
    }

    /** Initialize the action instance
     * @param props configuration properties
     *
     */
    public void init(ConfigResponse config) throws InvalidActionDataException {
        // First, let's set the type
        String sId = config.getValue(CFG_ID);
        alertDefId = Integer.valueOf(sId);
    }
    
    /**
     * Returns the alertDefId.
     * @return int
     */
    public int getAlertDefId() {
        return alertDefId.intValue();
    }

    /**
     * Sets the alertDefId.
     * @param alertDefId The type to alertDefId
     */
    public void setAlertDefId(int alertDefId) {
        this.alertDefId = new Integer(alertDefId);
    }

    /**
     * Get the name of the action class
     * @return the name of the implementing class
     */
    public String getImplementor() {
        return implementor;
    }

    public void setImplementor(String implementor) {
        this.implementor = implementor; 
    }
}
