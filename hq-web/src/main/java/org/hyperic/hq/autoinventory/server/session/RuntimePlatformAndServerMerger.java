package org.hyperic.hq.autoinventory.server.session;

import java.util.List;

import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.CompositeRuntimeResourceReport;
import org.hyperic.hq.autoinventory.server.session.RuntimeReportProcessor.ServiceMergeInfo;
import org.hyperic.hq.common.ApplicationException;

public interface RuntimePlatformAndServerMerger {
    List<ServiceMergeInfo> mergePlatformsAndServers(String agentToken, CompositeRuntimeResourceReport crrr)
        throws ApplicationException, AutoinventoryException;

    void reportAIRuntimeReport(String agentToken, CompositeRuntimeResourceReport crrr) throws AutoinventoryException,
        PermissionException, ValidationException, ApplicationException;

    void schedulePlatformAndServerMerges(String agentToken, CompositeRuntimeResourceReport crrr);

}
