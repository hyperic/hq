package org.hyperic.hq.control.server.session;

import java.util.Date;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.control.GroupControlActionResult;

/**
 * Executes control actions against a group of resources by submitting them to
 * the proper agents and waiting for asynchronous results
 * @author jhickey
 * 
 */
public interface GroupControlActionExecutor {

    /**
     * Executes a control action against a group of resources. This method DOES
     * wait for the agent to asynchronously return results
     * @param id The resource against which to execute the action
     * @param subjectName The Name of the AuthzSubject performing the action
     * @param dateScheduled The Date scheduled
     * @param scheduled true if this was scheduled, false if "quick control"
     * @param description The description of the action
     * @param action The action to execute
     * @param args Arguments to the action or null if action has no args
     * @param order An array of group member ids if the action is to be executed
     *        against resources in a certain order. Using order will cause
     *        actions to be invoked synchronously for each group member, and
     *        action execution will stop on first failure. If array is empty,
     *        the action is run against each group member asynchronously and in
     *        parallel, with a final wait at the end for all results to be
     *        reported.
     * @param defaultResourceTimeout The default per-resource timeout used if
     *        timeout is not in the resource's control config. If the operation
     *        is ordered, this timeout will be used waiting for each synchronous
     *        action. If unordered, the largest of the resource timeouts will be
     *        used to wait for all results.
     * @return The result of the group action
     */
    GroupControlActionResult executeGroupControlAction(AppdefEntityID id, String subjectName,
                                                       Date dateScheduled, boolean scheduled,
                                                       String description, String action,
                                                       String args, int[] order,
                                                       int defaultResourceTimeout);
}
