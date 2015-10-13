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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

/**
 * An action that adds users to an alert definition in the BizApp.
 * 
 */
@Component("addAlertUsersActionNG")
@Scope("prototype")
public class AddUsersActionNG extends AddNotificationsActionNG implements
		ModelDriven<AddUsersFormNG> {

	private final Log log = LogFactory.getLog(AddUsersActionNG.class.getName());

	

	public AddUsersActionNG() {
		addForm = new AddUsersFormNG();
	}

	public String add() throws Exception {
		return super.execute();
	}

	public String reset() throws Exception {
		SessionUtils.removeList(getServletRequest().getSession(),
				Constants.PENDING_USERS_SES_ATTR);
		if (addForm.getAd() != null) {
			alertDefId = addForm.getAd().toString();
		}else{
			alertDefId = getServletRequest().getParameter("ad");
		}
		if (addForm.getEid() != null) {
			eid = addForm.getEid();
		}else{
			eid = getServletRequest().getParameter("eid");
		}
		if (addForm.getAetid() != null) {
			aetid = addForm.getAetid();
		}else{
			aetid = getServletRequest().getParameter("aetid");
		}
		return RESET;
	}
	

	public String cancel() throws Exception {
		SessionUtils.removeList(getServletRequest().getSession(),
				Constants.PENDING_USERS_SES_ATTR);
		if (addForm.getAd() != null) {
			alertDefId = addForm.getAd().toString();
		}else{
			alertDefId = getServletRequest().getParameter("ad");
		}
		if (addForm.getEid() != null) {
			eid = addForm.getEid();
		}else{
			eid = getServletRequest().getParameter("eid");
		}
		if (addForm.getAetid() != null) {
			aetid = addForm.getAetid();
		}else{
			aetid = getServletRequest().getParameter("aetid");
		}
		return CANCELED;
	}

	protected String preProcess(HttpServletRequest request,
			AddNotificationsFormNG form, Map<String, Object> params,
			HttpSession session) throws Exception {

		if (form.getAd() != null) {
			alertDefId = form.getAd().toString();
		}
		if (form.getEid() != null) {
			eid = form.getEid();
		}
		if (form.getAetid() != null) {
			aetid = form.getAetid();
		}

		String forward = checkSubmit(form);
		if (forward != null) {
			if (addForm.isCancelClicked() || addForm.isResetClicked()) {
				log.debug("removing pending user list");
				SessionUtils.removeList(session,
						Constants.PENDING_USERS_SES_ATTR);
			} else if (((AddUsersFormNG) addForm).isAddClicked()) {
				log.debug("adding to pending user list");
				SessionUtils.addToList(session,
						Constants.PENDING_USERS_SES_ATTR,
						((AddUsersFormNG) addForm).getAvailableUsers());
				log.debug("@@@@@@@@@@"
						+ ((AddUsersFormNG) addForm).getAvailableUsers()
								.toString());
				for (int i = 0; i < ((AddUsersFormNG) addForm)
						.getAvailableUsers().length; i++) {
					log.debug("Avalilable Users "
							+ ((AddUsersFormNG) addForm).getAvailableUsers()[i]);
				}
			} else if (addForm.isRemoveClicked()) {
				log.debug("removing from pending user list");
				SessionUtils.removeFromList(session,
						Constants.PENDING_USERS_SES_ATTR,
						((AddUsersFormNG) addForm).getPendingUsers());
			}
		}

		return forward;
	}

	protected void postProcess(HttpServletRequest request, HttpSession session) {
		log.debug("removing pending user list");
		SessionUtils.removeList(session, Constants.PENDING_USERS_SES_ATTR);
		addActionMessage(  "alerts.config.confirm.AddUsers" );
	}

	protected Set<Object> getNotifications(AddNotificationsFormNG form,
			HttpSession session) {
		log.debug("getting pending user list");
		Integer[] pendingUserIds = SessionUtils.getList(session,
				Constants.PENDING_USERS_SES_ATTR);
		Set<Object> userIds = new HashSet<Object>();
		for (int i = 0; i < pendingUserIds.length; i++) {
			userIds.add(pendingUserIds[i]);
			log.debug("adding user [" + pendingUserIds[i] + "]");
		}

		return userIds;
	}

	public int getNotificationType() {
		return EmailActionConfig.TYPE_USERS;
	}

	

	public AddUsersFormNG getAddForm() {
		return (AddUsersFormNG) addForm;
	}

	public void setAddForm(AddUsersFormNG addForm) {
		this.addForm = addForm;
	}

	public AddUsersFormNG getModel() {

		return (AddUsersFormNG) addForm;
	}

}
