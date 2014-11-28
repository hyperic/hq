/**
 * 
 */
package com.vmware.hyperic.model.relations;

/**
 * @author imakhlin
 * 
 */
public enum RelationType {
    PARENT, CHILD, SIBLING;

    public RelationType getResourceSubType(final String key) {
        return valueOf(key.toUpperCase());
    }
}
