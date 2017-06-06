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

package org.hyperic.hq.ui.action.resource.common.monitor.alerts.config;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.servlet.ServletException;

import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * An Action that retrieves data from the BizApp to facilitate display of the
 * form to add users to AlertDefinition.
 * 
 */
@Component("addUsersFormPrepareActionNG")
public class AddUsersFormPrepareActionNG extends
		AddNotificationsFormPrepareActionNG implements ViewPreparer {

	@Autowired
	private AuthzBoss authzBoss;

	/**
	 * Retrieve this data and store it in the specified request parameters:
	 * 
	 * <ul>
	 * <li><code>AlertDefinitionValue</code> object identified by
	 * <code>Constants.ALERT_DEFS_ATTR</code></li>
	 * <li><code>List</code> of available <code>AuthzSubjectValue</code> objects
	 * (those not already associated with the alert def) in
	 * <code>Constants.AVAIL_USERS_ATTR</code></li>
	 * <li><code>Integer</code> number of available users in
	 * <code>Constants.NUM_AVAIL_USERS_ATTR</code></li>
	 * <li><code>List</code> of pending <code>AuthzSubjectValue</code> objects
	 * (those in queue to be associated with the definition) in
	 * <code>Constants.PENDING_USERS_ATTR</code></li>
	 * <li><code>Integer</code> number of pending users in
	 * <code>Constants.NUM_PENDING_USERS_ATTR</code></li>
	 * </ul>
	 */

	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		AddUsersFormNG addForm = new AddUsersFormNG();

		request = getServletRequest();

		if(request.getParameter("ad") != null){
			addForm.setAd(Integer.parseInt(request.getParameter("ad")));
		}
		Integer sessionId;
		try {
			sessionId = RequestUtils.getSessionId(request);

			AppdefEntityID aeid;
			try {
				aeid = RequestUtils.getEntityTypeId(request);
				addForm.setAetid(aeid.getAppdefKey());
			} catch (ParameterNotFoundException e) {
				aeid = RequestUtils.getEntityId(request);
				addForm.setType(new Integer(aeid.getType()));
				addForm.setRid(aeid.getId());
			}

			// pending users are those on the right side of the "add
			// to list" widget- awaiting association with the Alert
			// Definition when the form's "ok" button is clicked.
			Integer[] pendingUserIds = SessionUtils.getList(
					request.getSession(), Constants.PENDING_USERS_SES_ATTR);

			Integer[] userIds = getNotificationIds(request, addForm, aeid,
					EmailActionConfig.TYPE_USERS);

			PageControl pcp = RequestUtils.getPageControl(request, "psp",
					"pnp", "sop", "scp");
			PageList<AuthzSubjectValue> pendingUsers = authzBoss
					.getSubjectsById(sessionId, pendingUserIds, pcp);

			// available users are all users in the system that are
			// _not_ associated with the definition and are not
			// pending
			PageControl pca = RequestUtils.getPageControl(request, "psa",
					"pna", "soa", "sca");

			ArrayList<Integer> excludes = new ArrayList<Integer>(
					pendingUserIds.length + userIds.length);
			excludes.addAll(Arrays.asList(pendingUserIds));
			excludes.addAll(Arrays.asList(userIds));

			PageList<AuthzSubjectValue> availableUsers = authzBoss
					.getAllSubjects(sessionId, excludes, pca);

			request.setAttribute(Constants.PENDING_USERS_ATTR, pendingUsers);
			request.setAttribute(Constants.AVAIL_USERS_ATTR, availableUsers);
		} catch (ServletException e) {
			log.error(e);
		} catch (SessionNotFoundException e) {
			log.error(e);
		} catch (SessionTimeoutException e) {
			log.error(e);
		} catch (PermissionException e) {
			log.error(e);
		} catch (InvalidActionDataException e) {
			log.error(e);
		} catch (RemoteException e) {
			log.error(e);
		} catch (EncodingException e) {
			log.error(e);
		} catch (NotFoundException e) {
			log.error(e);
		}
	}

}
