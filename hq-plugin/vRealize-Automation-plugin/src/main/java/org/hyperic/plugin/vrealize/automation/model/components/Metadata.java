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
            "size",
            "totalElements",
            "totalPages",
            "number",
            "offset"
})
public class Metadata {

    @JsonProperty("size")
    private long size;
    @JsonProperty("totalElements")
    private long totalElements;
    @JsonProperty("totalPages")
    private long totalPages;
    @JsonProperty("number")
    private long number;
    @JsonProperty("offset")
    private long offset;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     *
     * @return The size
     */
    @JsonProperty("size")
    public long getSize() {
        return size;
    }

    /**
     *
     * @param size The size
     */
    @JsonProperty("size")
    public void setSize(long size) {
        this.size = size;
    }

    /**
     *
     * @return The totalElements
     */
    @JsonProperty("totalElements")
    public long getTotalElements() {
        return totalElements;
    }

    /**
     *
     * @param totalElements The totalElements
     */
    @JsonProperty("totalElements")
    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    /**
     *
     * @return The totalPages
     */
    @JsonProperty("totalPages")
    public long getTotalPages() {
        return totalPages;
    }

    /**
     *
     * @param totalPages The totalPages
     */
    @JsonProperty("totalPages")
    public void setTotalPages(long totalPages) {
        this.totalPages = totalPages;
    }

    /**
     *
     * @return The number
     */
    @JsonProperty("number")
    public long getNumber() {
        return number;
    }

    /**
     *
     * @param number The number
     */
    @JsonProperty("number")
    public void setNumber(long number) {
        this.number = number;
    }

    /**
     *
     * @return The offset
     */
    @JsonProperty("offset")
    public long getOffset() {
        return offset;
    }

    /**
     *
     * @param offset The offset
     */
    @JsonProperty("offset")
    public void setOffset(long offset) {
        this.offset = offset;
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
        return new HashCodeBuilder().append(size).append(totalElements).append(totalPages).append(number).append(offset).append(
                    additionalProperties).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Metadata) == false) {
            return false;
        }
        Metadata rhs = ((Metadata) other);
        return new EqualsBuilder().append(size, rhs.size).append(totalElements, rhs.totalElements).append(totalPages,
                    rhs.totalPages).append(number, rhs.number).append(offset, rhs.offset).append(additionalProperties,
                    rhs.additionalProperties).isEquals();
    }

}
