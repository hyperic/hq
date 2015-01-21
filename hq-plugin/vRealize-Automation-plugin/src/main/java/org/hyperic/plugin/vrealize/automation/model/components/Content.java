
package org.hyperic.plugin.vrealize.automation.model.components;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

@JsonPropertyOrder({
    "@type",
    "serviceId",
    "serviceName",
    "serviceTypeId",
    "notAvailable",
    "lastUpdated",
    "statusEndPointUrl",
    "serviceStatus"
})
public class Content {

    @JsonProperty("@type")
    private String Type;
    @JsonProperty("serviceId")
    private String serviceId;
    @JsonProperty("serviceName")
    private String serviceName;
    @JsonProperty("serviceTypeId")
    private String serviceTypeId;
    @JsonProperty("notAvailable")
    private boolean notAvailable;
    @JsonProperty("lastUpdated")
    private String lastUpdated;
    @JsonProperty("statusEndPointUrl")
    private String statusEndPointUrl;
    @JsonProperty("serviceStatus")
    private ServiceStatus serviceStatus;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     *
     * @return
     *     The Type
     */
    @JsonProperty("@type")
    public String getType() {
        return Type;
    }

    /**
     *
     * @param Type
     *     The @type
     */
    @JsonProperty("@type")
    public void setType(String Type) {
        this.Type = Type;
    }

    /**
     *
     * @return
     *     The serviceId
     */
    @JsonProperty("serviceId")
    public String getServiceId() {
        return serviceId;
    }

    /**
     *
     * @param serviceId
     *     The serviceId
     */
    @JsonProperty("serviceId")
    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    /**
     *
     * @return
     *     The serviceName
     */
    @JsonProperty("serviceName")
    public String getServiceName() {
        return serviceName;
    }

    /**
     *
     * @param serviceName
     *     The serviceName
     */
    @JsonProperty("serviceName")
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     *
     * @return
     *     The serviceTypeId
     */
    @JsonProperty("serviceTypeId")
    public String getServiceTypeId() {
        return serviceTypeId;
    }

    /**
     *
     * @param serviceTypeId
     *     The serviceTypeId
     */
    @JsonProperty("serviceTypeId")
    public void setServiceTypeId(String serviceTypeId) {
        this.serviceTypeId = serviceTypeId;
    }

    /**
     *
     * @return
     *     The notAvailable
     */
    @JsonProperty("notAvailable")
    public boolean isNotAvailable() {
        return notAvailable;
    }

    /**
     *
     * @param notAvailable
     *     The notAvailable
     */
    @JsonProperty("notAvailable")
    public void setNotAvailable(boolean notAvailable) {
        this.notAvailable = notAvailable;
    }

    /**
     *
     * @return
     *     The lastUpdated
     */
    @JsonProperty("lastUpdated")
    public String getLastUpdated() {
        return lastUpdated;
    }

    /**
     *
     * @param lastUpdated
     *     The lastUpdated
     */
    @JsonProperty("lastUpdated")
    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     *
     * @return
     *     The statusEndPointUrl
     */
    @JsonProperty("statusEndPointUrl")
    public String getStatusEndPointUrl() {
        return statusEndPointUrl;
    }

    /**
     *
     * @param statusEndPointUrl
     *     The statusEndPointUrl
     */
    @JsonProperty("statusEndPointUrl")
    public void setStatusEndPointUrl(String statusEndPointUrl) {
        this.statusEndPointUrl = statusEndPointUrl;
    }

    /**
     *
     * @return
     *     The serviceStatus
     */
    @JsonProperty("serviceStatus")
    public ServiceStatus getServiceStatus() {
        return serviceStatus;
    }

    /**
     *
     * @param serviceStatus
     *     The serviceStatus
     */
    @JsonProperty("serviceStatus")
    public void setServiceStatus(ServiceStatus serviceStatus) {
        this.serviceStatus = serviceStatus;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(Type).append(serviceId).append(serviceName).append(serviceTypeId).append(notAvailable).append(lastUpdated).append(statusEndPointUrl).append(serviceStatus).append(additionalProperties).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Content) == false) {
            return false;
        }
        Content rhs = ((Content) other);
        return new EqualsBuilder().append(Type, rhs.Type).append(serviceId, rhs.serviceId).append(serviceName, rhs.serviceName).append(serviceTypeId, rhs.serviceTypeId).append(notAvailable, rhs.notAvailable).append(lastUpdated, rhs.lastUpdated).append(statusEndPointUrl, rhs.statusEndPointUrl).append(serviceStatus, rhs.serviceStatus).append(additionalProperties, rhs.additionalProperties).isEquals();
    }

}
