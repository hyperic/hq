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
    protected String _oid;
    protected String _address;
    private String implementor =
        "com.hyperic.hq.bizapp.server.action.alert.SnmpAction";

    public SnmpActionConfig() {
    }
    
    public SnmpActionConfig(String address, String oid) {
        _address = address;
        _oid = oid;
    }

    public void setOid(String oid) {
        _oid = oid;
    }

    public String getOid() {
        return _oid;
    }

    public void setAddress(String address) {
        _address = address;
    }

    public String getAddress() {
        return _address;
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.events.ActionConfigInterface#getConfigSchema()
     */
    public ConfigSchema getConfigSchema() {
        StringConfigOption address, oid;
        ConfigSchema res = new ConfigSchema();
    
        address = new StringConfigOption(CFG_ADDRESS,
                "Transport Address ([transport:]address)", "");
        address.setMinLength(1);
        res.addOption(address);
    
        oid = new StringConfigOption(CFG_OID, "OID", "1.3.6");
        oid.setMinLength(1);
        res.addOption(oid);
        
        return res;
    }

    public ConfigResponse getConfigResponse()
        throws InvalidOptionException, InvalidOptionValueException {
        ConfigResponse response = new ConfigResponse();
        response.setValue(CFG_ADDRESS, this.getAddress());
        response.setValue(CFG_OID, this.getOid());
        return response;
    }

    public void init(ConfigResponse config) throws InvalidActionDataException {
        try {
            setAddress(config.getValue(CFG_ADDRESS));
            setOid(config.getValue(CFG_OID));
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
