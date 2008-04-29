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
import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.util.StringUtil;
import org.hyperic.util.StringifiedException;

/**
 * This class just encapsulates the "raw data" part of the scan
 * state, without any utility methods or other flim-flam.  It exists
 * to make it easy to move scan states across SOAP and other network
 * transports.
 */
public class ScanStateCore implements Serializable {
    
    private AIPlatformValue _platform = null;
    private ScanMethodState[] _scanMethodStates;

    private boolean _isInterrupted = false;
    private boolean _isDone = false;

    private long _startTime = 0;
    private long _endTime = 0;
    private String _certDN;

    private boolean _areServersIncluded = true;

    private StringifiedException _globalException = null;

    public ScanStateCore () {}
    
    // -------------------- Getters and setters   --------------------
    
    public ScanMethodState[] getScanMethodStates() {
        return _scanMethodStates;
    }
    public void setScanMethodStates(ScanMethodState[] scanMethodStates) {
        this._scanMethodStates = scanMethodStates;
    }

    public long getStartTime() { return _startTime; }
    public void setStartTime(long _startTime) { this._startTime = _startTime; }

    public long getEndTime() { return _endTime; }
    public void setEndTime(long _endTime) { this._endTime = _endTime; }

    public StringifiedException getGlobalException() {
        return _globalException;
    }

    public void setGlobalException(StringifiedException _globalException) {
        this._globalException = _globalException;
    }

    /**
     * @return true if this scan includes server information, false otherwise.
     */
    public boolean getAreServersIncluded () {
        return _areServersIncluded; 
    }
    public void setAreServersIncluded (boolean b) {
        _areServersIncluded = b; 
    }

    public boolean getIsDone () { return _isDone; }
    public void setIsDone (boolean b) { 
        _isDone = b;
    }

    public boolean getIsInterrupted () { return _isInterrupted; }
    public void setIsInterrupted (boolean b) { 
        _isInterrupted = b;
    }
    
    public AIPlatformValue getPlatform () { return _platform; }
    public void setPlatform ( AIPlatformValue platform ) {
        if ( _platform != null ) {
            AIIpValue[] ips = getIps();            
            for ( int i=0; i<ips.length; i++ ) {
                platform.addAIIpValue(ips[i]);
            }
        }
        _platform = platform;
        _platform.setCertdn( _certDN );
    }

    // These are only needed for SOAP...
    public AIIpValue[] getIps () { 
        return _platform.getAIIpValues();
    }
    public void setIps ( AIIpValue[] ips ) {
        if ( _platform == null ) {
            _platform = new AIPlatformValue();
        } else {
            _platform.removeAllAIIpValues();
        }
        if ( ips == null ) return;
        for ( int i=0; i<ips.length; i++ ) {
            _platform.addAIIpValue(ips[i]);
        }
    }

    public String getCertDN () { return _platform.getCertdn(); }
    public void setCertDN ( String certDN ) {
        this._certDN=certDN;
        if( _platform != null )
            _platform.setCertdn(certDN);
    }

    /**
     * Write the contents of this scan state to
     * an AgentRemoteValue object.
     * @param keyName The key name to use when populating the 
     * AgentRemoteValue with data.
     * @param arv The AgentRemoteValue to write.
     * @exception AutoinventoryException If a problem occurs populating
     * the AgentRemoteValue with data.
     */
    public void toAgentRemoteValue(String keyName,
                                   AgentRemoteValue arv) 
        throws AutoinventoryException {

        arv.setValue(keyName, LatherUtil.encode(this));
    }

    /**
     * Read the contents of an AgentRemoteValue object and
     * create a scan state.
     * @param keyName The key name to use when reading data from the 
     * AgentRemoteValue.
     * @param arv The AgentRemoteValue to read.
     * @return A ScanState object read from the AgentRemoteValue.
     * @exception AutoinventoryException If a problem occurs reading
     * data from the AgentRemoteValue.
     */
    public static ScanStateCore fromAgentRemoteValue(String keyName,
                                                     AgentRemoteValue arv)
        throws AutoinventoryException {

        return LatherUtil.decodeScanStateCore(arv.getValue(keyName));
    }

    public String toString () {
        if( _platform == null ) return "[ScanState]";
        return "[ScanState" 
            + " platform=" + _platform
            + " ips=" + StringUtil.arrayToString(_platform.getAIIpValues())
            + " serversIncluded=" + _areServersIncluded
            + "]";
    }
}
