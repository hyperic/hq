/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.autoinventory;

import java.io.Serializable;

import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.encoding.Base64;

/**
 * This class just encapsulates the "raw data" part of the scan
 * configuration, without any utility methods or other flim-flam.  It exists
 * to make it easy to move scan configurations across SOAP and other network
 * transports.
 */
public class ScanConfigurationCore implements Serializable {

    /**
     * The configurations for the scan methods to run.
     */
    private ScanMethodConfig[] _scanConfigs = new ScanMethodConfig[0];

    /**
     * An array of ServerSignature objects representing the servers
     * to scan for.
     */
    private ServerSignature[] _serverSigs = new ServerSignature[0];

    /**
     * ConfigResponse for the platform
     */
    private ConfigResponse configResponse;
    
    public ScanConfigurationCore () {}

    /**
     * Get the scan method configs to use in this scan.
     * @return An array of ScanMethodConfig objects.
     */
    public ScanMethodConfig[] getScanMethodConfigs () {
        return _scanConfigs;
    }

    /**
     * Set the scan method configs to use in this scan.
     * @param configs An array of ScanMethodConfig objects.
     */
    public void setScanMethodConfigs (ScanMethodConfig[] configs) {
        _scanConfigs = configs;
    }

    public void addScanMethodConfig (ScanMethod method, ConfigResponse configResponse) {

        ScanMethodConfig config = new ScanMethodConfig();
        config.setMethodClass(method.getClass().getName());
        config.setConfig(configResponse);

        if ( _scanConfigs != null ) {
            ScanMethodConfig[] newConfigs = new ScanMethodConfig[_scanConfigs.length+1];
            System.arraycopy(_scanConfigs, 0, newConfigs, 0, _scanConfigs.length);
            newConfigs[newConfigs.length-1] = config;
            _scanConfigs = newConfigs;
        } else {
            _scanConfigs = new ScanMethodConfig[] { config };
        }
    }

    /**
     * Set the scan method configurations for this scan.
     * @param serverTypes An array of ScanMethodConfig objects.
     */
    public void setServerSignatures ( ScanMethodConfig[] configs ) {
        _scanConfigs = configs;
    }

    /**
     * Get the server signatures to scan for in this scan.
     * @return An array of ServerSignature objects indicating
     * which servers to scan for.
     */
    public ServerSignature[] getServerSignatures () {
        return _serverSigs;
    }

    /**
     * Set the server signatures to scan for in this scan.
     * @param serverTypes An array of ServerSignature objects indicating
     * which server types to scan for.
     */
    public void setServerSignatures ( ServerSignature[] serverSigs ) {
        _serverSigs = serverSigs;
    }

    public ScanMethodConfig findScanMethodConfig(ScanMethod method) {
        return findScanMethodConfig(method.getClass().getName());
    }

    public ScanMethodConfig findScanMethodConfig(String methodClass) {
        if ( _scanConfigs != null ) {
            for ( int i=0; i<_scanConfigs.length; i++ ) {
                if ( _scanConfigs[i].getMethodClass().equals(methodClass) ) {
                    return _scanConfigs[i];
                }
            }
        }
        throw new IllegalArgumentException("Scan method not found: " + 
                                           methodClass);
    }

    public byte[] serialize() throws AutoinventoryException {
        return LatherUtil.serialize(this);
    }

    public String encode() throws AutoinventoryException {
        return Base64.encode(serialize());
    }
    
    public static ScanConfigurationCore deserialize(byte[] data) 
        throws AutoinventoryException {

        return LatherUtil.deserializeScanConfigurationCore(data);
    }

    public static ScanConfigurationCore decode(String data)
        throws AutoinventoryException {

        return deserialize(Base64.decode(data));
    }

    /**
     * Write the contents of this scan configuration to
     * an AgentRemoteValue object.
     * @param keyName The key name to use when populating the 
     * AgentRemoteValue with data.
     * @param arv The AgentRemoteValue to write.
     * @exception AutoinventoryException If a problem occurs populating
     * the AgentRemoteValue with data.
     */
    public void toAgentRemoteValue( String keyName,
                                    AgentRemoteValue arv ) 
        throws AutoinventoryException {

        arv.setValue(keyName, Base64.encode(serialize()));
    }

    /**
     * Read the contents of an AgentRemoteValue object and
     * create a scan configuration.
     * @param keyName The key name to use when reading data from the 
     * AgentRemoteValue.
     * @param arv The AgentRemoteValue to read.
     * @return A ScanConfiguration object read from the AgentRemoteValue.
     * @exception AutoinventoryException If a problem occurs reading
     * data from the AgentRemoteValue.
     */
    public static ScanConfigurationCore fromAgentRemoteValue( String keyName,
                                                              AgentRemoteValue arv )
        throws AutoinventoryException {

        return decode(arv.getValue(keyName));
    }

    public String toString () {
        String rstr = "ScanConfiguration: ";
        int i;
        String stypes = "";
        ServerSignature[] serverSigs = getServerSignatures();
        if ( serverSigs == null || serverSigs.length == 0 ) stypes = "NONE";
        else {
            for ( i=0; i<serverSigs.length; i++ ) {
                if ( i>0 ) stypes += ", ";
                stypes += serverSigs[i].getServerTypeName();
            }
        }

        String scans;
        ScanMethodConfig[] configs = getScanMethodConfigs();
        if ( configs == null || configs.length == 0 ) scans = "NONE";
        else {
            scans = "";
            String methodClass;
            ScanMethod method;
            String methodName;
            for ( i=0; i<configs.length; i++ ) {
                methodClass = configs[i].getMethodClass();
                try {
                    method
                        = (ScanMethod) Class.forName(methodClass).newInstance();
                    methodName = method.getName();
                    scans += "\n\t" + methodName + ": "
                        + configs[i].getConfig();
                } catch ( Exception e ) {
                    scans += "\n\t" + methodClass + ": "
                        + "Error reading config: " + e;
                }
            }
        }

        rstr += "\n  Global Config:"
            + "\n  Server Types: " + stypes
            + "\n  Scan Methods: " + scans
            + "\nConfigResponse: " + this.configResponse;

        return rstr;
    }

    public boolean equals ( Object o ) {
        if ( o instanceof ScanConfigurationCore ) {

            ScanConfigurationCore scc = (ScanConfigurationCore) o;
            ScanMethodConfig[] config1, config2;
            ServerSignature[] ss1, ss2;
            int i;

            config1 = getScanMethodConfigs();
            config2 = scc.getScanMethodConfigs();
            if ( config1.length != config2.length ) return false;
            for ( i=0; i<config1.length; i++ ) {
                if ( !config1[i].equals(config2[i]) ) return false;
            }

            ss1 = getServerSignatures();
            ss2 = scc.getServerSignatures();
            if ( ss1.length != ss2.length ) return false;
            for ( i=0; i<ss1.length; i++ ) {
                if ( !ss1[i].equals(ss2[i]) ) return false;
            }

            return true;
        }
        return false;
    }

    public ConfigResponse getConfigResponse() {
        return this.configResponse;
    }

    public void setConfigResponse(ConfigResponse configResponse) {
        this.configResponse = configResponse;
    }
}
