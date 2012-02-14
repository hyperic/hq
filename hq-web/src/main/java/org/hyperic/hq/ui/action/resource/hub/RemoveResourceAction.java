/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.ui.action.resource.hub;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.common.shared.TransactionRetry;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.util.Reference;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Removes resources in ResourceHub
 */
public class RemoveResourceAction
    extends BaseAction {

    private final Log log = LogFactory.getLog(RemoveResourceAction.class.getName());

    private final EventsBoss eventsBoss;

    private final AppdefBoss appdefBoss;

    private final TransactionRetry transactionRetry;

    @Autowired
    public RemoveResourceAction(EventsBoss eventsBoss, AppdefBoss appdefBoss, TransactionRetry transactionRetry) {
        super();
        this.eventsBoss = eventsBoss;
        this.appdefBoss = appdefBoss;
        this.transactionRetry = transactionRetry;
    }

    @Override
	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        ResourceHubForm hubForm = (ResourceHubForm) form;
        if (hubForm.isGroupClicked()) {
            HttpSession session = request.getSession();
            session.setAttribute(Constants.ENTITY_IDS_ATTR, hubForm.getResources());

            session.setAttribute(Constants.RESOURCE_TYPE_ATTR, hubForm.getFf());
            return mapping.findForward("newgroup");
        } else if (hubForm.isDeleteClicked()) {
            removeResources(request, hubForm.getResources());
        } else if (hubForm.getEnableAlerts().isSelected()) {
            activateAlerts(request, hubForm.getResources(), true);
        } else if (hubForm.getDisableAlerts().isSelected()) {
            activateAlerts(request, hubForm.getResources(), false);
        }
        return returnSuccess(request, mapping);
    }

    private void activateAlerts(HttpServletRequest request, String[] resourceItems, boolean enabled) throws Exception {

        Integer sessionId = RequestUtils.getSessionId(request);

        List<String> resourceList = new ArrayList<String>();
        CollectionUtils.addAll(resourceList, resourceItems);
        List<AppdefEntityID> entities = BizappUtils.buildAppdefEntityIds(resourceList);

        eventsBoss.activateAlertDefinitions(sessionId.intValue(),
            entities.toArray(new AppdefEntityID[entities.size()]), enabled);

        RequestUtils.setConfirmation(request, enabled ? "resource.common.confirm.AlertsEnabled"
                                                     : "resource.common.confirm.AlertsDisabled");
    }

    private void removeResources(HttpServletRequest request, String[] resourceItems) throws SessionNotFoundException,
        ApplicationException, VetoException, RemoteException, ServletException {

        final Integer sessionId = RequestUtils.getSessionId(request);

        List<String> resourceList = new ArrayList<String>();
        CollectionUtils.addAll(resourceList, resourceItems);
        List<AppdefEntityID> entities = BizappUtils.buildAppdefEntityIds(resourceList);
        final Reference<SessionNotFoundException> snfeToThrow = new Reference<SessionNotFoundException>();
        final Reference<ApplicationException> aeToThrow = new Reference<ApplicationException>();
        final Reference<SessionTimeoutException> stoeToThrow = new Reference<SessionTimeoutException>();
        if (resourceItems != null && resourceItems.length > 0) {
            final Reference<Integer> numDeleted = new Reference<Integer>();
            final Reference<String> vetoMessage = new Reference<String>();
            //Fix for [HHQ-5417] - numDeleted.get() returned null
            numDeleted.set(0);
            // about the exception handling:
            // if someone either deleted the entity out from under our user
            // or the user hit the back button, a derivative of
            // AppdefEntityNotFoundException gets thrown... we can still
            // keep going on, trying to delete the other things in our list
            // (which is why the whole shebang isn't in one big
            // try / catch) but we only confirm that something was deleted
            // if something actually, um, was
            for (final AppdefEntityID resourceId : entities) {
                final Runnable runner = new Runnable() {
                    public void run() {
                        try {
                            List<AppdefEntityID> list =
                                Arrays.asList(appdefBoss.removeAppdefEntity(sessionId.intValue(), resourceId, false));
                            Integer num = numDeleted.get();
                            numDeleted.set(num + list.size());
                        } catch (AppdefEntityNotFoundException e) {
                            log.error("Removing resource " + resourceId + "failed.", e);
                        } catch (VetoException v) {
                            vetoMessage.set(v.getMessage());
                            log.info(vetoMessage);
                        } catch (SessionNotFoundException e) {
                            snfeToThrow.set(e);
                        } catch (SessionTimeoutException e) {
                            stoeToThrow.set(e);
                        } catch (ApplicationException e) {
                            aeToThrow.set(e);
                        }
                    }
                };
                transactionRetry.runTransaction(runner, 3, 1000);
                // this is ugly, may need to rethink the TransactionRetry api to avoid this type of code
                if (snfeToThrow.get() != null) {
                    throw snfeToThrow.get();
                } else if (stoeToThrow.get() != null) {
                    throw stoeToThrow.get();
                } else if (aeToThrow.get() != null) {
                    throw aeToThrow.get();
                }
            }
            if (vetoMessage.get() != null) {
                RequestUtils.setErrorObject(request,"resource.common.inventory.groups.error.RemoveVetoed", vetoMessage.get());
            } else if (numDeleted.get() > 0) {
                RequestUtils.setConfirmation(request, "resource.common.confirm.ResourcesRemoved");
            }
        }
    }
}
