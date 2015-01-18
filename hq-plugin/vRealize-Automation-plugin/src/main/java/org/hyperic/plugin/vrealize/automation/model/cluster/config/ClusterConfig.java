
package org.hyperic.plugin.vrealize.automation.model.cluster.config;

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
    "nodeId",
    "nodeHost",
    "nodeCert",
    "nodeType",
    "updatedOn",
    "createdOn",
    "lastUpdateRelative"
})
public class ClusterConfig {

    @JsonProperty("nodeId")
    private String nodeId;
    @JsonProperty("nodeHost")
    private String nodeHost;
    @JsonProperty("nodeCert")
    private String nodeCert;
    @JsonProperty("nodeType")
    private String nodeType;
    @JsonProperty("updatedOn")
    private Long updatedOn;
    @JsonProperty("createdOn")
    private Long createdOn;
    @JsonProperty("lastUpdateRelative")
    private String lastUpdateRelative;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     *
     * @return
     *     The nodeId
     */
    @JsonProperty("nodeId")
    public String getNodeId() {
        return nodeId;
    }

    /**
     *
     * @param nodeId
     *     The nodeId
     */
    @JsonProperty("nodeId")
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     *
     * @return
     *     The nodeHost
     */
    @JsonProperty("nodeHost")
    public String getNodeHost() {
        return nodeHost;
    }

    /**
     *
     * @param nodeHost
     *     The nodeHost
     */
    @JsonProperty("nodeHost")
    public void setNodeHost(String nodeHost) {
        this.nodeHost = nodeHost;
    }

    /**
     *
     * @return
     *     The nodeCert
     */
    @JsonProperty("nodeCert")
    public String getNodeCert() {
        return nodeCert;
    }

    /**
     *
     * @param nodeCert
     *     The nodeCert
     */
    @JsonProperty("nodeCert")
    public void setNodeCert(String nodeCert) {
        this.nodeCert = nodeCert;
    }

    /**
     *
     * @return
     *     The nodeType
     */
    @JsonProperty("nodeType")
    public String getNodeType() {
        return nodeType;
    }

    /**
     *
     * @param nodeType
     *     The nodeType
     */
    @JsonProperty("nodeType")
    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    /**
     *
     * @return
     *     The updatedOn
     */
    @JsonProperty("updatedOn")
    public Long getUpdatedOn() {
        return updatedOn;
    }

    /**
     *
     * @param updatedOn
     *     The updatedOn
     */
    @JsonProperty("updatedOn")
    public void setUpdatedOn(Long updatedOn) {
        this.updatedOn = updatedOn;
    }

    /**
     *
     * @return
     *     The createdOn
     */
    @JsonProperty("createdOn")
    public Long getCreatedOn() {
        return createdOn;
    }

    /**
     *
     * @param createdOn
     *     The createdOn
     */
    @JsonProperty("createdOn")
    public void setCreatedOn(Long createdOn) {
        this.createdOn = createdOn;
    }

    /**
     *
     * @return
     *     The lastUpdateRelative
     */
    @JsonProperty("lastUpdateRelative")
    public String getLastUpdateRelative() {
        return lastUpdateRelative;
    }

    /**
     *
     * @param lastUpdateRelative
     *     The lastUpdateRelative
     */
    @JsonProperty("lastUpdateRelative")
    public void setLastUpdateRelative(String lastUpdateRelative) {
        this.lastUpdateRelative = lastUpdateRelative;
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
        return new HashCodeBuilder().append(nodeId).append(nodeHost).append(nodeCert).append(nodeType).append(updatedOn).append(createdOn).append(lastUpdateRelative).append(additionalProperties).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ClusterConfig) == false) {
            return false;
        }
        ClusterConfig rhs = ((ClusterConfig) other);
        return new EqualsBuilder().append(nodeId, rhs.nodeId).append(nodeHost, rhs.nodeHost).append(nodeCert, rhs.nodeCert).append(nodeType, rhs.nodeType).append(updatedOn, rhs.updatedOn).append(createdOn, rhs.createdOn).append(lastUpdateRelative, rhs.lastUpdateRelative).append(additionalProperties, rhs.additionalProperties).isEquals();
    }

}
