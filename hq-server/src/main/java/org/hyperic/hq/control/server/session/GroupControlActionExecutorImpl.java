package org.hyperic.hq.control.server.session;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.util.MessagePublisher;
import org.hyperic.hq.control.ControlActionResult;
import org.hyperic.hq.control.ControlEvent;
import org.hyperic.hq.control.GroupControlActionResult;
import org.hyperic.hq.control.shared.ControlActionTimeoutException;
import org.hyperic.hq.control.shared.ControlConstants;
import org.hyperic.hq.control.shared.ControlScheduleManager;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.grouping.server.session.GroupUtil;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link GroupControlActionExecutor}
 * @author jhickey
 * 
 */
@Component
public class GroupControlActionExecutorImpl implements GroupControlActionExecutor {
    private final Log log = LogFactory.getLog(GroupControlActionExecutorImpl.class);
    private ControlScheduleManager controlScheduleManager;
    private AuthzSubjectManager authzSubjectManager;
    private ControlActionResultsCollector controlActionResultsCollector;
    private MessagePublisher sender;
    private ControlActionExecutor controlActionExecutor;

    @Autowired
    public GroupControlActionExecutorImpl(
                                          ControlScheduleManager controlScheduleManager,
                                          AuthzSubjectManager authzSubjectManager,
                                          ControlActionResultsCollector controlActionResultsCollector,
                                          MessagePublisher sender,
                                          ControlActionExecutor controlActionExecutor) {
        this.controlScheduleManager = controlScheduleManager;
        this.authzSubjectManager = authzSubjectManager;
        this.controlActionResultsCollector = controlActionResultsCollector;
        this.sender = sender;
        this.controlActionExecutor = controlActionExecutor;
    }

    public GroupControlActionResult executeGroupControlAction(AppdefEntityID id,
                                                              String subjectName,
                                                              Date dateScheduled,
                                                              boolean scheduled,
                                                              String description, String action,
                                                              String args, int[] order,
                                                              int defaultResourceTimeout) {
        AuthzSubject subject = authzSubjectManager.findSubjectByName(subjectName);
        Integer jobId = null;
        Integer groupJobId = null;
        String status = ControlConstants.STATUS_COMPLETED;
        String errMsg = null;
        GroupControlActionResult result = null;

        try {
            groupJobId = controlScheduleManager.createHistory(id, null, null, subjectName, action,
                args, false, System.currentTimeMillis(), System.currentTimeMillis(), dateScheduled
                    .getTime(), ControlConstants.STATUS_INPROGRESS, "", null);

            int longestTimeout = 0;

            // get the group members and iterate over them
            List<AppdefEntityID> groupMembers = GroupUtil.getCompatGroupMembers(subject, id, order,
                PageControl.PAGE_ALL);

            Set<ControlActionResult> individualResults = new HashSet<ControlActionResult>();
            if (groupMembers.isEmpty()) {
                errMsg = "Group contains no resources";
                return new GroupControlActionResult(individualResults, id,
                    ControlConstants.STATUS_FAILED, errMsg);
            }

            ArrayList<Integer> jobIds = new ArrayList<Integer>();

            for (Iterator<AppdefEntityID> i = groupMembers.iterator(); i.hasNext();) {
                AppdefEntityID entity = (AppdefEntityID) i.next();

                int timeout = controlActionResultsCollector.getTimeout(subject, entity,
                    defaultResourceTimeout);
                if (timeout > longestTimeout) {
                    longestTimeout = timeout;
                }

                jobId = controlActionExecutor.executeControlAction(entity, id, groupJobId,
                    subjectName, dateScheduled, scheduled, description, action, args);

                // Keep a reference to all the job ids in case we need to
                // verify later they have completed successfully.

                jobIds.add(jobId);

                // If the job is ordered, synchronize the commands
                if (order!=null && order.length > 0) {
                    ControlActionResult memberResult = controlActionResultsCollector.waitForResult(
                        jobId, timeout);
                    individualResults.add(memberResult);
                    if (memberResult.getStatus().equals(ControlConstants.STATUS_FAILED)) {
                        errMsg = "Job id " + jobId + " failed: " + memberResult.getMessage();
                        // Ordered jobs bail out on first failure
                        result = new GroupControlActionResult(individualResults, id,
                            ControlConstants.STATUS_FAILED, errMsg);
                        break;
                    }
                }
            }

            if (order == null || order.length == 0) {
                result = controlActionResultsCollector.waitForGroupResults(id, jobIds,
                    longestTimeout);
                if (result.getStatus().equals(ControlConstants.STATUS_FAILED)) {
                    errMsg = result.getMessage();
                }
            } else if (result == null) {
                result = new GroupControlActionResult(individualResults, id,
                    ControlConstants.STATUS_COMPLETED, null);
            }

        } catch (GroupNotCompatibleException e) {
            errMsg = e.getMessage();
        } catch (ControlActionTimeoutException e) {
            errMsg = e.getMessage();
        } catch (PluginException e) {
            errMsg = e.getMessage();
        } catch (PermissionException e) {
            // This will only happen if the permisions on a resource change
            // after the job was scheudled.
            errMsg = "Permission denied: " + e.getMessage();
        } catch (AppdefEntityNotFoundException e) {
            // Shouldnt happen
            errMsg = "System error, resource not found: " + e.getMessage();
        } catch (ApplicationException e) {
            // Shouldnt happen
            errMsg = "Application error: " + e.getMessage();
        } finally {

            if (groupJobId != null) {

                if (errMsg != null) {
                    status = ControlConstants.STATUS_FAILED;
                }

                try {
                    // Update group history entry
                    controlScheduleManager.updateHistory(groupJobId, System.currentTimeMillis(),
                        status, errMsg);

                    ControlHistory cv = controlScheduleManager.getJobHistoryValue(groupJobId);

                    // Send a control event
                    ControlEvent event = new ControlEvent(cv.getSubject(), cv.getEntityType()
                        .intValue(), cv.getEntityId(), cv.getAction(), cv.getScheduled()
                        .booleanValue(), cv.getDateScheduled(), status);
                    event.setMessage(errMsg);

                    sender.publishMessage(EventConstants.EVENTS_TOPIC, event);
                } catch (Exception e) {
                    this.log.error("Unable to update control history: " + e.getMessage());
                }
            }
        }
        return result;
    }

}
