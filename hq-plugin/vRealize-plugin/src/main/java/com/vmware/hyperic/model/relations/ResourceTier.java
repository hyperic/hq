/**
 * 
 */
package com.vmware.hyperic.model.relations;

/**
 * @author imakhlin
 * 
 */
public enum ResourceTier {
    PLATFORM, SERVER, SERVICE, LOGICAL;

    public ResourceTier getResourceTier(final String tier) {
        return valueOf(tier.toUpperCase());
    }
}
