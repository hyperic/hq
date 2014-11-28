/**
 * 
 */
package com.vmware.hyperic.model.relations;

/**
 * @author imakhlin
 * 
 */
public enum ResourceSubType {
    APPLICATION, GROUP, TAG;

    public ResourceSubType getResourceSubType(final String key) {
        return valueOf(key.toUpperCase());
    }
}
