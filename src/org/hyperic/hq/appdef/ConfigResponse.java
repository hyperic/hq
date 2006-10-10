package org.hyperic.hq.appdef;

import java.util.Collection;
import java.io.Serializable;

/**
 *
 */
public class ConfigResponse implements Serializable
{
    private Integer id;
    private long _version_;
    private byte[] productResponse;
    private byte[] controlResponse;
    private byte[] measurementResponse;
    private byte[] autoInventoryResponse;
    private byte[] reponseTimeResponse;
    private boolean userManaged;
    private String validationErr;
    private Collection platforms;
    private Collection servers;
    private Collection services;

    // Constructors

    /**
     * default constructor
     */
    public ConfigResponse()
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

    public byte[] getReponseTimeResponse()
    {
        return this.reponseTimeResponse;
    }

    public void setReponseTimeResponse(byte[] reponseTimeResponse)
    {
        this.reponseTimeResponse = reponseTimeResponse;
    }

    public boolean isUserManaged()
    {
        return this.userManaged;
    }

    public void setUserManaged(boolean userManaged)
    {
        this.userManaged = userManaged;
    }

    public String getValidationErr()
    {
        return this.validationErr;
    }

    public void setValidationErr(String validationErr)
    {
        this.validationErr = validationErr;
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
}
