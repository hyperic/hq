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

package org.hyperic.hq.livedata.shared;

import com.thoughtworks.xstream.XStream;

import java.io.Serializable;

import org.hyperic.hq.appdef.shared.AppdefEntityID;

/**
 * Result object from live data commands.
 */
public class LiveDataResult implements Serializable {

    private AppdefEntityID _id;
    private boolean _error;
    private Throwable _cause;
    private String _errorMsg;
    private String _xml;

    public LiveDataResult(AppdefEntityID id, String xml) {
        _error = false;
        _id = id;
        _xml = xml;
    }

    public LiveDataResult(AppdefEntityID id, Throwable t, String errorMsg) {
        _error = true;
        _id = id;
        _cause = t;
        _errorMsg = errorMsg;
    }

    /**
     * Get the appdef entity id for this result
     */
    public AppdefEntityID getAppdefEntityID() {
        return _id;
    }

    /**
     * Get the raw XML result for this request.
     */
    public String getXMLResult() {
        return _xml;
    }

    /**
     * Get the Object result for this request.
     */
    public Object getObjectResult() {
        XStream xstream = new XStream();
        return xstream.fromXML(_xml);
    }

    /**
     * Get the error (if it exists) for this command.  Not always guaranteed
     * to be non-null if hasError() is true. 
     */
    public Throwable getCause() {
        return _cause;
    }

    /**
     * Get the error string for this result.
     */
    public String getErrorMessage() {
        return _errorMsg;
    }

    /**
     * True if an error occured collecting the live data
     */
    public boolean hasError() {
        return _error;
    }
}
