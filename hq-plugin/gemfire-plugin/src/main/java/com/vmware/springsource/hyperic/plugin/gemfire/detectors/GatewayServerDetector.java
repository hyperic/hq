package com.vmware.springsource.hyperic.plugin.gemfire.detectors;

import java.util.Map;
import org.hyperic.util.config.ConfigResponse;

public class GatewayServerDetector extends MemberDetector {

    boolean isValidMember(Map memberDetails) {
        boolean iserver = "true".equalsIgnoreCase(memberDetails.get("gemfire.member.isserver.boolean").toString());
        boolean isgateway = "true".equalsIgnoreCase(memberDetails.get("gemfire.member.isgateway.boolean").toString());
        boolean res = iserver && isgateway;
        getLog().debug("[isValidMember] iserver=" + iserver + " isgateway=" + isgateway + " res=" + res);
        return res;
    }

    @Override
    ConfigResponse getAtributtes(Map memberDetails) {
        ConfigResponse res = new ConfigResponse();
        return res;
    }
}
