package org.hyperic.plugin.vrealize.automation.model.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
            "links",
            "content",
            "metadata"
})
public class ComponentsRegistry {

    @JsonProperty("links")
    private List<Link> links = new ArrayList<Link>();
    @JsonProperty("content")
    private List<Content> content = new ArrayList<Content>();
    @JsonProperty("metadata")
    private Metadata metadata;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     *
     * @return The links
     */
    @JsonProperty("links")
    public List<Link> getLinks() {
        return links;
    }

    /**
     *
     * @param links The links
     */
    @JsonProperty("links")
    public void setLinks(List<Link> links) {
        this.links = links;
    }

    /**
     *
     * @return The content
     */
    @JsonProperty("content")
    public List<Content> getContent() {
        return content;
    }

    /**
     *
     * @param content The content
     */
    @JsonProperty("content")
    public void setContent(List<Content> content) {
        this.content = content;
    }

    /**
     *
     * @return The metadata
     */
    @JsonProperty("metadata")
    public Metadata getMetadata() {
        return metadata;
    }

    /**
     *
     * @param metadata The metadata
     */
    @JsonProperty("metadata")
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
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
    public void setAdditionalProperty(String name,
                                      Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(links).append(content).append(metadata).append(additionalProperties).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ComponentsRegistry) == false) {
            return false;
        }
        ComponentsRegistry rhs = ((ComponentsRegistry) other);
        return new EqualsBuilder().append(links, rhs.links).append(content, rhs.content).append(metadata, rhs.metadata).append(
                    additionalProperties, rhs.additionalProperties).isEquals();
    }

}
