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
    protected static final String CFG_TRAP_OID = "snmpTrapOID";
    protected static final String CFG_NOTIFICATION_MECHANISM = "snmpNotificationMechanism";
    protected static final String CFG_VARIABLE_BINDINGS = "variableBindings";
    
    protected String oid;
    protected String address;
    protected String snmpTrapOID;
    protected String snmpNotificationMechanism;
    protected String variableBindings;  // in JSONArray format
    
    private static String implementor =
        "com.hyperic.hq.bizapp.server.action.alert.SnmpAction";

    public SnmpActionConfig() {
    }
    
    public SnmpActionConfig(String snmpNotificationMechanism,
                            String address, String snmpTrapOID, String oid,
                            String variableBindings) {
        this.snmpNotificationMechanism = snmpNotificationMechanism;
        this.address = address;
        this.snmpTrapOID = snmpTrapOID;
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

    public void setSnmpTrapOID(String snmpTrapOID) {
        this.snmpTrapOID = snmpTrapOID;
    }

    public String getSnmpTrapOID() {
        return snmpTrapOID;
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
            if ((variableBindings != null)
                    && (variableBindings.length() > 0)) {

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
    
        StringConfigOption snmpTrapOID = new StringConfigOption(CFG_TRAP_OID, "SNMP Trap OID", "");
        snmpTrapOID.setMinLength(1);
        snmpTrapOID.setOptional(true);
        res.addOption(snmpTrapOID);
        
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
        response.setValue(CFG_TRAP_OID, this.getSnmpTrapOID());
        response.setValue(CFG_NOTIFICATION_MECHANISM, this.getSnmpNotificationMechanism());
        response.setValue(CFG_VARIABLE_BINDINGS, this.getVariableBindings());
        
        return response;
    }

    public void init(ConfigResponse config) throws InvalidActionDataException {
        try {
            setAddress(config.getValue(CFG_ADDRESS));
            setOid(config.getValue(CFG_OID));
            setSnmpTrapOID(config.getValue(CFG_TRAP_OID));
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
