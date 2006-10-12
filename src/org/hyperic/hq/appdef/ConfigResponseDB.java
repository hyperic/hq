package org.hyperic.hq.appdef;

import org.hyperic.hq.appdef.shared.ConfigResponseValue;
import org.hyperic.hq.appdef.shared.ConfigResponsePK;

import java.util.Collection;
import java.io.Serializable;

/**
 *
 */
public class ConfigResponseDB implements Serializable
{
    private Integer id;
    private long _version_;
    private byte[] productResponse;
    private byte[] controlResponse;
    private byte[] measurementResponse;
    private byte[] autoInventoryResponse;
    private byte[] responseTimeResponse;
    private boolean userManaged;
    private String validationError;
    private Collection platforms;
    private Collection servers;
    private Collection services;

    // Constructors

    /**
     * default constructor
     */
    public ConfigResponseDB()
    {
        super();
    }

    // Property accessors
    public Integer getId()
    {
        return this.id;
    }

    private void setId(Integer id)
    {
        this.id = id;
    }

    private void set_version_(long _version_)
    {
        this._version_ = _version_;
    }

    public long get_version_()
    {
        return this._version_;
    }

    public byte[] getProductResponse()
    {
        return this.productResponse;
    }

    public void setProductResponse(byte[] productResponse)
    {
        this.productResponse = productResponse;
    }

    public byte[] getControlResponse()
    {
        return this.controlResponse;
    }

    public void setControlResponse(byte[] controlResponse)
    {
        this.controlResponse = controlResponse;
    }

    public byte[] getMeasurementResponse()
    {
        return this.measurementResponse;
    }

    public void setMeasurementResponse(byte[] measurementResponse)
    {
        this.measurementResponse = measurementResponse;
    }

    public byte[] getAutoInventoryResponse()
    {
        return this.autoInventoryResponse;
    }

    public void setAutoInventoryResponse(byte[] autoInventoryResponse)
    {
        this.autoInventoryResponse = autoInventoryResponse;
    }

    public byte[] getResponseTimeResponse()
    {
        return this.responseTimeResponse;
    }

    public void setResponseTimeResponse(byte[] responseTimeResponse)
    {
        this.responseTimeResponse = responseTimeResponse;
    }

    public boolean isUserManaged()
    {
        return this.userManaged;
    }

    /**
     * added for legacy EJB Entity Bean compatibility
     * @return
     */
    public boolean getUserManaged()
    {
        return isUserManaged();
    }

    public void setUserManaged(boolean userManaged)
    {
        this.userManaged = userManaged;
    }

    public String getValidationError()
    {
        return this.validationError;
    }

    public void setValidationError(String validationErr)
    {
        this.validationError = validationErr;
    }

    public Collection getPlatforms()
    {
        return this.platforms;
    }

    public void setPlatforms(Collection platforms)
    {
        this.platforms = platforms;
    }

    public Collection getServers()
    {
        return this.servers;
    }

    public void setServers(Collection servers)
    {
        this.servers = servers;
    }

    public Collection getServices()
    {
        return this.services;
    }

    public void setServices(Collection services)
    {
        this.services = services;
    }

    private ConfigResponseValue _value = new ConfigResponseValue();
    /**
     * for legacy EJB Entity Bean compatibility
     * @return
     */
    public ConfigResponseValue getConfigResponseValue()
    {
        _value.setId(getId());
        _value.setProductResponse(getProductResponse());
        _value.setControlResponse(getControlResponse());
        _value.setMeasurementResponse(getMeasurementResponse());
        _value.setAutoinventoryResponse(getAutoInventoryResponse());
        _value.setResponseTimeResponse(getResponseTimeResponse());
        _value.setUserManaged(getUserManaged());
        _value.setValidationError(
            (getValidationError() == null) ? "" : getValidationError());
        return _value;
    }

    public void setConfigResponseValue(ConfigResponseValue valueHolder)
    {
        setProductResponse( valueHolder.getProductResponse() );
        setControlResponse( valueHolder.getControlResponse() );
        setMeasurementResponse( valueHolder.getMeasurementResponse() );
        setAutoInventoryResponse( valueHolder.getAutoinventoryResponse() );
        setResponseTimeResponse( valueHolder.getResponseTimeResponse() );
        setUserManaged( valueHolder.getUserManaged() );
        setValidationError( valueHolder.getValidationError() );
    }

    private ConfigResponsePK _pkey = new ConfigResponsePK();
    /**
     * for legacy EJB Entity Bean compatibility
     * @return
     */
    public ConfigResponsePK getPrimaryKey()
    {
        _pkey.setId(id);
        return _pkey;
    }

    // TODO: add equals and hashCode()
}