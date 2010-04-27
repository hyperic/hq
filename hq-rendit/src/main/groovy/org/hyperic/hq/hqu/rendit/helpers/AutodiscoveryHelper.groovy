package org.hyperic.hq.hqu.rendit.helpers


import org.hyperic.hq.appdef.shared.AIPlatformValue
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.appdef.shared.AIQueueConstants
import org.hyperic.hq.appdef.shared.AIQueueManager;
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.util.pager.PageControl

/**
 * The AutodiscoveryHelper can be used to list and approve resources into
 * the inventory.
 */
class AutodiscoveryHelper extends BaseHelper {

    private aiqMan = Bootstrap.getBean(AIQueueManager.class);
    AutodiscoveryHelper(AuthzSubject user) {
        super(user)
    }

    /**
      * Return a List of {@link AIPlatformValue}s in the queue.
      */
    public List getQueue() {
        aiqMan.getQueue(user, true, true, false, PageControl.PAGE_ALL)
    }

    /**
     * Find an AIPlatformValue by fqdn.
     *
     * @param fqdn The platform fqdn to find.
     * @return A {@link AIPlatformValue} with the given fqdn or null if a
     * queued platform with the given fqdn does not exist.
     *
     */
    public AIPlatformValue findByFqdn(String fqdn) {
        aiqMan.findAIPlatformByFqdn(user, fqdn)
    }

    /**
     * Find an AIPlatformValue by id.
     * @param id The id to look up.
     * @return A {@link AIPlatformValue} with the given id or null if a
     * queued platform with the given id does not exist.
     */
    public AIPlatformValue findById(int id) {
        aiqMan.findAIPlatformById(user, id)
    }
    
    /**
     * Approve the platform with the given fqdn.
     * @param fqdn The platform fqdn to approve
     * @return A List of {@link org.hyperic.hq.appdef.server.session.AppdefResource}s
     * that were created as a result of processing the queue.
     */
    public List approve(AIPlatformValue platform) {
        // If a platform is a placeholder, don't attempt to approve it.
        List platformIds = []
        if (platform.queueStatus != AIQueueConstants.Q_STATUS_PLACEHOLDER) {
            platformIds.add(platform.id)
        }

        // Only approve servers that are not marked ignored
        List serverIds = platform.AIServerValues.findAll { !it.ignored }.id

        // All IP changes get auto-approved
        List ipIds = platform.AIIpValues.id

        aiqMan.processQueue(user, platformIds, serverIds, ipIds,
                            AIQueueConstants.Q_DECISION_APPROVE)
    }
}
