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

package org.hyperic.hq.appdef;

import org.hyperic.hibernate.PersistedObject;

public class ConfigResponseDB extends PersistedObject
{
    private byte[] _productResponse;
    private byte[] _controlResponse;
    private byte[] _measurementResponse;
    private byte[] _autoInventoryResponse;
    private byte[] _responseTimeResponse;
    private boolean _userManaged;
    private String _validationError;

    public ConfigResponseDB() {
        super();
    }

    public byte[] getProductResponse() {
        return _productResponse;
    }

    public void setProductResponse(byte[] productResponse) {
        _productResponse = productResponse;
    }

    public byte[] getControlResponse() {
        return _controlResponse;
    }

    public void setControlResponse(byte[] controlResponse) {
        _controlResponse = controlResponse;
    }

    public byte[] getMeasurementResponse() {
        return _measurementResponse;
    }

    public void setMeasurementResponse(byte[] measurementResponse) {
        _measurementResponse = measurementResponse;
    }

    public byte[] getAutoInventoryResponse() {
        return _autoInventoryResponse;
    }

    public void setAutoInventoryResponse(byte[] autoInventoryResponse) {
        _autoInventoryResponse = autoInventoryResponse;
    }

    public byte[] getResponseTimeResponse() {
        return _responseTimeResponse;
    }

    public void setResponseTimeResponse(byte[] responseTimeResponse) {
        _responseTimeResponse = responseTimeResponse;
    }

    public boolean isUserManaged() {
        return _userManaged;
    }

    public boolean getUserManaged() {
        return isUserManaged();
    }

    public void setUserManaged(boolean userManaged) {
        _userManaged = userManaged;
    }

    public String getValidationError() {
        return _validationError;
    }

    public void setValidationError(String validationErr) {
        _validationError = validationErr;
    }

    public boolean equals(Object obj) {
        return (obj instanceof ConfigResponseDB) && super.equals(obj);
    }
}