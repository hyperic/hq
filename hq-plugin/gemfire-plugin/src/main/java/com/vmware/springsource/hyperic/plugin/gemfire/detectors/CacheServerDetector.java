package com.vmware.springsource.hyperic.plugin.gemfire.detectors;

import java.util.Map;
import org.hyperic.util.config.ConfigResponse;

public class CacheServerDetector extends MemberDetector {

    boolean isValidMember(Map memberDetails) {
        return "true".equalsIgnoreCase(memberDetails.get("gemfire.member.isserver.boolean").toString())
                && !"true".equalsIgnoreCase(memberDetails.get("gemfire.member.isgateway.boolean").toString());
    }

    final ConfigResponse getAtributtes(Map memberDetails) {
        ConfigResponse res = new ConfigResponse();
        res.setValue("id", (String) memberDetails.get("gemfire.member.id.string"));
        res.setValue("type", (String) memberDetails.get("gemfire.member.type.string"));
        res.setValue("host", (String) memberDetails.get("gemfire.member.host.string"));
        res.setValue("port", (Integer) memberDetails.get("gemfire.member.port.int"));
        return res;
    }
}
