package org.hyperic.hq.autoinventory.server.session;

import java.util.List;

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.autoinventory.server.session.RuntimeReportProcessor.ServiceMergeInfo;
import org.hyperic.hq.common.ApplicationException;

public interface ServiceMerger {

    /**
     * Merge a list of {@link ServiceMergeInfo}s in HQ's appdef model
     */
    void mergeServices(List<ServiceMergeInfo> mergeInfos) throws PermissionException, ApplicationException;

    /**
     * Enqueues a list of {@link ServiceMergeInfo}s, indicating services to be
     * merged into appdef.
     */
    void scheduleServiceMerges(final String agentToken, final List<ServiceMergeInfo> serviceMerges);

    void markServiceClean(String agentToken);

    void markServiceClean(Agent agent, boolean serviceClean);

    boolean currentlyWorkingOn(Agent a);
}
