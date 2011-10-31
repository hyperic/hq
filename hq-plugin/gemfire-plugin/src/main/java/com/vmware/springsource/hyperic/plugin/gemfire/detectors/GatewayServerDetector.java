package com.vmware.springsource.hyperic.plugin.gemfire.detectors;

import java.util.Map;
import org.hyperic.util.config.ConfigResponse;

public class GatewayServerDetector extends MemberDetector {

    boolean isValidMember(Map memberDetails) {
        boolean isgateway = "true".equalsIgnoreCase(memberDetails.get("gemfire.member.isgateway.boolean").toString());
        boolean res = isgateway;
        getLog().debug("[isValidMember] isgateway=" + isgateway + " res=" + res);
        return res;
    }

    @Override
    ConfigResponse getAtributtes(Map memberDetails) {
        ConfigResponse res = new ConfigResponse();
        return res;
    }
}
