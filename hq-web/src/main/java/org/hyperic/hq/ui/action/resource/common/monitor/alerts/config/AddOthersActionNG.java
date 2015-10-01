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
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.util.SessionUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

/**
 * An action that adds other email addresses ( those that are not in CAM ) to an
 * alert definition in the BizApp.
 * 
 */
@Component("addOthersActionNG")
@Scope("prototype")
public class AddOthersActionNG extends AddNotificationsActionNG implements
		ModelDriven<AddOthersFormNG> {

	private AddOthersFormNG addForm = new AddOthersFormNG();

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

		return checkSubmit(form);
	}

	protected void postProcess(HttpServletRequest request, HttpSession session) {
		addActionMessage(getText("alerts.config.confirm.AddOthers"));
	}

	protected Set<Object> getNotifications(AddNotificationsFormNG form,
			HttpSession session) {

		String emailAddresses = addForm.getEmailAddresses();
		StringTokenizer token = new StringTokenizer(emailAddresses, ",;");
		Set<Object> emails = new HashSet<Object>();
		while (token.hasMoreTokens()) {
			emails.add(token.nextToken().trim());
		}

		return emails;
	}
	public String add() throws Exception {
		return super.execute();
	}
	public String reset() throws Exception {
	
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
	public int getNotificationType() {
		return EmailActionConfig.TYPE_EMAILS;
	}

	public AddOthersFormNG getAddForm() {
		return addForm;
	}

	public void setAddForm(AddOthersFormNG addForm) {
		this.addForm = addForm;
	}

	public AddOthersFormNG getModel() {

		return addForm;
	}
}
