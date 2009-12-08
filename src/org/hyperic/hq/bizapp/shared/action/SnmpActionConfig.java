package org.hyperic.hq.bizapp.shared.action;

import org.hyperic.hq.events.ActionConfigInterface;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;
import org.hyperic.util.config.StringConfigOption;

public class SnmpActionConfig implements ActionConfigInterface {

    protected static final String CFG_OID = "oid";
    protected static final String CFG_ADDRESS = "address";
    protected static final String CFG_NOTIFICATION_MECHANISM = "snmpNotificationMechanism";
    
    protected String oid;
    protected String address;
    protected String snmpNotificationMechanism;
    
    private String implementor =
        "com.hyperic.hq.bizapp.server.action.alert.SnmpAction";

    public SnmpActionConfig() {
    }
    
    public SnmpActionConfig(String address, String oid, String snmpNotificationMechanism) {
        this.address = address;
        this.oid = oid;
        this.snmpNotificationMechanism = snmpNotificationMechanism;
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

    /* (non-Javadoc)
     * @see org.hyperic.hq.events.ActionConfigInterface#getConfigSchema()
     */
    public ConfigSchema getConfigSchema() {
        StringConfigOption address, oid, snmpNotificationMechanism;
        ConfigSchema res = new ConfigSchema();
    
        address = new StringConfigOption(CFG_ADDRESS,
                "Transport Address ([transport:]address)", "");
        address.setMinLength(1);
        res.addOption(address);
    
        oid = new StringConfigOption(CFG_OID, "OID", "1.3.6");
        oid.setMinLength(1);
        res.addOption(oid);
        
        snmpNotificationMechanism = new StringConfigOption(CFG_NOTIFICATION_MECHANISM, "SNMP Notification Mechanism", "v1 Trap");
        snmpNotificationMechanism.setMinLength(1);
        res.addOption(snmpNotificationMechanism);
        
        return res;
    }

    public ConfigResponse getConfigResponse()
    throws InvalidOptionException, InvalidOptionValueException {
        ConfigResponse response = new ConfigResponse();
        
        response.setValue(CFG_ADDRESS, this.getAddress());
        response.setValue(CFG_OID, this.getOid());
        response.setValue(CFG_NOTIFICATION_MECHANISM, this.getSnmpNotificationMechanism());
        
        return response;
    }

    public void init(ConfigResponse config) throws InvalidActionDataException {
        try {
            setAddress(config.getValue(CFG_ADDRESS));
            setOid(config.getValue(CFG_OID));
            setSnmpNotificationMechanism(config.getValue(CFG_NOTIFICATION_MECHANISM));
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

    public String getSnmpNotificationMechanism() {
        return snmpNotificationMechanism;
    }

    public void setSnmpNotificationMechanism(String snmpNotificationMechanism) {
        this.snmpNotificationMechanism = snmpNotificationMechanism;
    }
}
