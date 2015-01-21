
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
    "initialized",
    "serviceName",
    "solutionUser",
    "startedTime",
    "serviceInitializationStatus",
    "errorMessage",
    "identityCertificateInfo",
    "serviceRegistrationId",
    "sslCertificateInfo",
    "defaultServiceEndpointType"
})
public class ServiceStatus {

    @JsonProperty("initialized")
    private boolean initialized;
    @JsonProperty("serviceName")
    private String serviceName;
    @JsonProperty("solutionUser")
    private String solutionUser;
    @JsonProperty("startedTime")
    private String startedTime;
    @JsonProperty("serviceInitializationStatus")
    private String serviceInitializationStatus;
    @JsonProperty("errorMessage")
    private Object errorMessage;
    @JsonProperty("identityCertificateInfo")

    private IdentityCertificateInfo identityCertificateInfo;
    @JsonProperty("serviceRegistrationId")
    private String serviceRegistrationId;
    @JsonProperty("sslCertificateInfo")

    private SslCertificateInfo sslCertificateInfo;
    @JsonProperty("defaultServiceEndpointType")
    private String defaultServiceEndpointType;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     *
     * @return
     *     The initialized
     */
    @JsonProperty("initialized")
    public boolean isInitialized() {
        return initialized;
    }

    /**
     *
     * @param initialized
     *     The initialized
     */
    @JsonProperty("initialized")
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
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
     *     The solutionUser
     */
    @JsonProperty("solutionUser")
    public String getSolutionUser() {
        return solutionUser;
    }

    /**
     *
     * @param solutionUser
     *     The solutionUser
     */
    @JsonProperty("solutionUser")
    public void setSolutionUser(String solutionUser) {
        this.solutionUser = solutionUser;
    }

    /**
     *
     * @return
     *     The startedTime
     */
    @JsonProperty("startedTime")
    public String getStartedTime() {
        return startedTime;
    }

    /**
     *
     * @param startedTime
     *     The startedTime
     */
    @JsonProperty("startedTime")
    public void setStartedTime(String startedTime) {
        this.startedTime = startedTime;
    }

    /**
     *
     * @return
     *     The serviceInitializationStatus
     */
    @JsonProperty("serviceInitializationStatus")
    public String getServiceInitializationStatus() {
        return serviceInitializationStatus;
    }

    /**
     *
     * @param serviceInitializationStatus
     *     The serviceInitializationStatus
     */
    @JsonProperty("serviceInitializationStatus")
    public void setServiceInitializationStatus(String serviceInitializationStatus) {
        this.serviceInitializationStatus = serviceInitializationStatus;
    }

    /**
     *
     * @return
     *     The errorMessage
     */
    @JsonProperty("errorMessage")
    public Object getErrorMessage() {
        return errorMessage;
    }

    /**
     *
     * @param errorMessage
     *     The errorMessage
     */
    @JsonProperty("errorMessage")
    public void setErrorMessage(Object errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     *
     * @return
     *     The identityCertificateInfo
     */
    @JsonProperty("identityCertificateInfo")
    public IdentityCertificateInfo getIdentityCertificateInfo() {
        return identityCertificateInfo;
    }

    /**
     *
     * @param identityCertificateInfo
     *     The identityCertificateInfo
     */
    @JsonProperty("identityCertificateInfo")
    public void setIdentityCertificateInfo(IdentityCertificateInfo identityCertificateInfo) {
        this.identityCertificateInfo = identityCertificateInfo;
    }

    /**
     *
     * @return
     *     The serviceRegistrationId
     */
    @JsonProperty("serviceRegistrationId")
    public String getServiceRegistrationId() {
        return serviceRegistrationId;
    }

    /**
     *
     * @param serviceRegistrationId
     *     The serviceRegistrationId
     */
    @JsonProperty("serviceRegistrationId")
    public void setServiceRegistrationId(String serviceRegistrationId) {
        this.serviceRegistrationId = serviceRegistrationId;
    }

    /**
     *
     * @return
     *     The sslCertificateInfo
     */
    @JsonProperty("sslCertificateInfo")
    public SslCertificateInfo getSslCertificateInfo() {
        return sslCertificateInfo;
    }

    /**
     *
     * @param sslCertificateInfo
     *     The sslCertificateInfo
     */
    @JsonProperty("sslCertificateInfo")
    public void setSslCertificateInfo(SslCertificateInfo sslCertificateInfo) {
        this.sslCertificateInfo = sslCertificateInfo;
    }

    /**
     *
     * @return
     *     The defaultServiceEndpointType
     */
    @JsonProperty("defaultServiceEndpointType")
    public String getDefaultServiceEndpointType() {
        return defaultServiceEndpointType;
    }

    /**
     *
     * @param defaultServiceEndpointType
     *     The defaultServiceEndpointType
     */
    @JsonProperty("defaultServiceEndpointType")
    public void setDefaultServiceEndpointType(String defaultServiceEndpointType) {
        this.defaultServiceEndpointType = defaultServiceEndpointType;
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
        return new HashCodeBuilder().append(initialized).append(serviceName).append(solutionUser).append(startedTime).append(serviceInitializationStatus).append(errorMessage).append(identityCertificateInfo).append(serviceRegistrationId).append(sslCertificateInfo).append(defaultServiceEndpointType).append(additionalProperties).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ServiceStatus) == false) {
            return false;
        }
        ServiceStatus rhs = ((ServiceStatus) other);
        return new EqualsBuilder().append(initialized, rhs.initialized).append(serviceName, rhs.serviceName).append(solutionUser, rhs.solutionUser).append(startedTime, rhs.startedTime).append(serviceInitializationStatus, rhs.serviceInitializationStatus).append(errorMessage, rhs.errorMessage).append(identityCertificateInfo, rhs.identityCertificateInfo).append(serviceRegistrationId, rhs.serviceRegistrationId).append(sslCertificateInfo, rhs.sslCertificateInfo).append(defaultServiceEndpointType, rhs.defaultServiceEndpointType).append(additionalProperties, rhs.additionalProperties).isEquals();
    }

}
