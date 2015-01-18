
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
    "identityCertificateExists",
    "issuerName",
    "principalName",
    "notValidBefore",
    "notValidAfter",
    "thumbprint"
})
public class IdentityCertificateInfo {

    @JsonProperty("identityCertificateExists")
    private boolean identityCertificateExists;
    @JsonProperty("issuerName")
    private String issuerName;
    @JsonProperty("principalName")
    private String principalName;
    @JsonProperty("notValidBefore")
    private String notValidBefore;
    @JsonProperty("notValidAfter")
    private String notValidAfter;
    @JsonProperty("thumbprint")
    private String thumbprint;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     *
     * @return
     *     The identityCertificateExists
     */
    @JsonProperty("identityCertificateExists")
    public boolean isIdentityCertificateExists() {
        return identityCertificateExists;
    }

    /**
     *
     * @param identityCertificateExists
     *     The identityCertificateExists
     */
    @JsonProperty("identityCertificateExists")
    public void setIdentityCertificateExists(boolean identityCertificateExists) {
        this.identityCertificateExists = identityCertificateExists;
    }

    /**
     *
     * @return
     *     The issuerName
     */
    @JsonProperty("issuerName")
    public String getIssuerName() {
        return issuerName;
    }

    /**
     *
     * @param issuerName
     *     The issuerName
     */
    @JsonProperty("issuerName")
    public void setIssuerName(String issuerName) {
        this.issuerName = issuerName;
    }

    /**
     *
     * @return
     *     The principalName
     */
    @JsonProperty("principalName")
    public String getPrincipalName() {
        return principalName;
    }

    /**
     *
     * @param principalName
     *     The principalName
     */
    @JsonProperty("principalName")
    public void setPrincipalName(String principalName) {
        this.principalName = principalName;
    }

    /**
     *
     * @return
     *     The notValidBefore
     */
    @JsonProperty("notValidBefore")
    public String getNotValidBefore() {
        return notValidBefore;
    }

    /**
     *
     * @param notValidBefore
     *     The notValidBefore
     */
    @JsonProperty("notValidBefore")
    public void setNotValidBefore(String notValidBefore) {
        this.notValidBefore = notValidBefore;
    }

    /**
     *
     * @return
     *     The notValidAfter
     */
    @JsonProperty("notValidAfter")
    public String getNotValidAfter() {
        return notValidAfter;
    }

    /**
     *
     * @param notValidAfter
     *     The notValidAfter
     */
    @JsonProperty("notValidAfter")
    public void setNotValidAfter(String notValidAfter) {
        this.notValidAfter = notValidAfter;
    }

    /**
     *
     * @return
     *     The thumbprint
     */
    @JsonProperty("thumbprint")
    public String getThumbprint() {
        return thumbprint;
    }

    /**
     *
     * @param thumbprint
     *     The thumbprint
     */
    @JsonProperty("thumbprint")
    public void setThumbprint(String thumbprint) {
        this.thumbprint = thumbprint;
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
        return new HashCodeBuilder().append(identityCertificateExists).append(issuerName).append(principalName).append(notValidBefore).append(notValidAfter).append(thumbprint).append(additionalProperties).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof IdentityCertificateInfo) == false) {
            return false;
        }
        IdentityCertificateInfo rhs = ((IdentityCertificateInfo) other);
        return new EqualsBuilder().append(identityCertificateExists, rhs.identityCertificateExists).append(issuerName, rhs.issuerName).append(principalName, rhs.principalName).append(notValidBefore, rhs.notValidBefore).append(notValidAfter, rhs.notValidAfter).append(thumbprint, rhs.thumbprint).append(additionalProperties, rhs.additionalProperties).isEquals();
    }

}
