package org.hyperic.hq.bizapp.shared.action;

import org.hyperic.hq.events.ActionConfigInterface;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;
import org.hyperic.util.config.StringConfigOption;
import org.json.JSONArray;
import org.json.JSONException;

public class SnmpActionConfig implements ActionConfigInterface {

    protected static final String CFG_OID = "oid";
    protected static final String CFG_ADDRESS = "address";
    protected static final String CFG_NOTIFICATION_MECHANISM = "snmpNotificationMechanism";
    protected static final String CFG_VARIABLE_BINDINGS = "variableBindings";
    
    protected String oid;
    protected String address;
    protected String snmpNotificationMechanism;
    protected String variableBindings;  // in JSONArray format
    
    private static String implementor =
        "com.hyperic.hq.bizapp.server.action.alert.SnmpAction";

    public SnmpActionConfig() {
    }
    
    public SnmpActionConfig(String snmpNotificationMechanism,
                            String address, String oid,
                            String variableBindings) {
        this.snmpNotificationMechanism = snmpNotificationMechanism;
        this.address = address;
        this.oid = oid;
        this.variableBindings = variableBindings;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getOid() {
        return oid;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public String getSnmpNotificationMechanism() {
        return snmpNotificationMechanism;
    }

    public void setSnmpNotificationMechanism(String snmpNotificationMechanism) {
        this.snmpNotificationMechanism = snmpNotificationMechanism;
    }
    
    /**
     * Gets the variable bindings configuration in JSON format
     */
    public String getVariableBindings() {
        return variableBindings;
    }
    
    /**
     * Sets the variable bindings configuration in JSONArray format
     */
    public void setVariableBindings(String variableBindings) {
        try {
            if (variableBindings != null
                    && variableBindings.length() > 0) {

                // validate that the variable bindings is in JSONArray format
                JSONArray j = new JSONArray(variableBindings);
                this.variableBindings = j.toString();
            } else {
                this.variableBindings = variableBindings;
            }
        } catch (JSONException je) {
            throw new IllegalArgumentException(
                    "Variable bindings must be in a valid JSONArray format:" 
                        + je.getMessage());
        }
    }
    
    /* (non-Javadoc)
     * @see org.hyperic.hq.events.ActionConfigInterface#getConfigSchema()
     */
    public ConfigSchema getConfigSchema() {
        ConfigSchema res = new ConfigSchema();
    
        StringConfigOption address = new StringConfigOption(CFG_ADDRESS,
                "Transport Address ([transport:]address)", "");
        address.setMinLength(1);
        res.addOption(address);
    
        StringConfigOption oid = new StringConfigOption(CFG_OID, "OID", "1.3.6");
        oid.setMinLength(1);
        res.addOption(oid);
        
        StringConfigOption snmpNotificationMechanism = 
            new StringConfigOption(CFG_NOTIFICATION_MECHANISM, "SNMP Notification Mechanism", "v2c Trap");
        snmpNotificationMechanism.setMinLength(1);
        res.addOption(snmpNotificationMechanism);
        
        StringConfigOption variableBindings = 
            new StringConfigOption(CFG_VARIABLE_BINDINGS, "User Variable Bindings", "[]");
        variableBindings.setOptional(true);
        res.addOption(variableBindings);
        
        return res;
    }

    public ConfigResponse getConfigResponse()
    throws InvalidOptionException, InvalidOptionValueException {
        ConfigResponse response = new ConfigResponse();
        
        response.setValue(CFG_ADDRESS, this.getAddress());
        response.setValue(CFG_OID, this.getOid());
        response.setValue(CFG_NOTIFICATION_MECHANISM, this.getSnmpNotificationMechanism());
        response.setValue(CFG_VARIABLE_BINDINGS, this.getVariableBindings());
        
        return response;
    }

    public void init(ConfigResponse config) throws InvalidActionDataException {
        try {
            setAddress(config.getValue(CFG_ADDRESS));
            setOid(config.getValue(CFG_OID));
            setSnmpNotificationMechanism(config.getValue(CFG_NOTIFICATION_MECHANISM, "v2c Trap"));
            setVariableBindings(config.getValue(CFG_VARIABLE_BINDINGS, "[]"));
        } catch (IllegalArgumentException ex) {
            throw new InvalidActionDataException(ex);
        }
    }

    public String getImplementor() {
        return implementor;
    }

    public void setImplementor(String implementor) {
        this.implementor = implementor;
    }
}
