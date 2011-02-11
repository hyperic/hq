package com.vmware.springsource.hyperic.plugin.gemfire.detectors;

import java.util.Map;

public class ApplicationServerDetector extends CacheServerDetector {

    @Override
    boolean isValidMember(Map memberDetails) {
        return (!"true".equalsIgnoreCase(memberDetails.get("gemfire.member.isserver.boolean").toString()))
                && (!"true".equalsIgnoreCase(memberDetails.get("gemfire.member.isgateway.boolean").toString()));
    }
}
