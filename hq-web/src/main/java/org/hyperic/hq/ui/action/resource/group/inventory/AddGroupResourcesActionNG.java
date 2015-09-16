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

package org.hyperic.hq.ui.action.resource.group.inventory;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.util.LabelValueBean;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.apache.tiles.AttributeContext;
import org.hyperic.hq.appdef.shared.AppSvcClustDuplicateAssignException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.ResourceControllerNG;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

/**
 * An Action that adds Resources to a Group in the BizApp. This is first created
 * with AddGroupResourcesFormPrepareAction, which creates the list of pending
 * Resources to add to the group.
 * 
 * Heavily based on:
 * 
 * @see org.hyperic.hq.ui.action.resource.group.inventory.AddGroupResourcesFormPrepareAction
 */
@Component("addGroupResourcesActionNG")
@Scope(value = "prototype")
public class AddGroupResourcesActionNG extends ResourceControllerNG implements
		ModelDriven<AddGroupResourcesFormNG> {
	private final Log log = LogFactory.getLog(AddGroupResourcesActionNG.class
			.getName());
	@javax.annotation.Resource
	private ResourceGroupManager resourceGroupManager;
	@javax.annotation.Resource
	private ResourceManager resourceManager;
	@javax.annotation.Resource
	private AppdefBoss appdefBoss;
	AddGroupResourcesFormNG addForm = new AddGroupResourcesFormNG();
	private String internalEid;
	private String type;
	private String rid;

	/**
	 * Add roles to the user specified in the given
	 * <code>AddGroupResourcesForm</code>.
	 */
	@SkipValidation
	public String start() throws Exception {

		request = getServletRequest();
		findAndSetResource(request, response);
		setHeaderResources();

		try {

			Integer groupId = addForm.getRid();

			PageControl pcPending;

			if (groupId == null) {
				groupId = RequestUtils.getResourceId(request);
			}

			int sessionId = RequestUtils.getSessionIdInt(request);

			PageControl pcAvail = RequestUtils.getPageControl(request, "psa",
					"pna", "soa", "sca");
			pcPending = RequestUtils.getPageControl(request, "psp", "pnp",
					"sop", "scp");

			AppdefGroupValue group = (AppdefGroupValue) RequestUtils
					.getResource(request);
			if (group == null) {
				addActionError(getText("resource.group.inventory.error.GroupNotFound"));
				return Constants.FAILURE_URL;
			}

			RequestUtils.setResource(request, group);
			addForm.setRid(group.getId());

			log.trace("available page control: " + pcAvail);
			log.trace("pending page control: " + pcPending);
			log.trace("getting group [" + groupId + "]");

			// XXX: if group == null, throw AppdefGroupNotFoundException
			/*
			 * pending resources are those on the right side of the
			 * "add to list" widget- awaiting association with the group when
			 * the form's "ok" button is clicked.
			 */
			List<String> pendingResourceIds = SessionUtils.getListAsListStr(
					request.getSession(), Constants.PENDING_RESOURCES_SES_ATTR);

			String nameFilter = RequestUtils.getStringParameter(request,
					"nameFilter", null);
			log.trace("getting pending resources for group [" + groupId + "]");

			List<AppdefEntityID> entities = BizappUtils
					.buildAppdefEntityIds(pendingResourceIds);

			AppdefEntityID[] pendingResItems;

			if (entities.size() > 0) {
				pendingResItems = new AppdefEntityID[entities.size()];
				entities.toArray(pendingResItems);
			} else {
				pendingResItems = null;
			}

			List<AppdefResourceValue> pendingResources = BizappUtils
					.buildAppdefResources(sessionId, appdefBoss,
							pendingResItems);

			List<AppdefResourceValue> sortedPendingResource = BizappUtils
					.sortAppdefResource(pendingResources, pcPending);
			PageList<AppdefResourceValue> pendingList = new PageList<AppdefResourceValue>();

			pendingList.setTotalSize(sortedPendingResource.size());

			Pager pendingPager = Pager.getDefaultPager();

			pendingList = pendingPager.seek(sortedPendingResource,
					pcPending.getPagenum(), pcPending.getPagesize());

			request.setAttribute(Constants.PENDING_RESOURCES_ATTR, pendingList);
			request.setAttribute(Constants.NUM_PENDING_RESOURCES_ATTR,
					new Integer(sortedPendingResource.size()));

			/*
			 * available resources are all resources in the system that are not
			 * associated with the user and are not pending
			 */
			log.trace("getting available resources for group [" + groupId + "]");

			String filterBy = addForm.getFilterBy();

			int appdefType = -1;
			if (filterBy != null) {
				appdefType = Integer.parseInt(filterBy);
			}

			PrepareResourceGroup p;

			if (group.isGroupCompat())
				p = new PrepareCompatGroup();
			else if (group.getGroupType() == AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP)
				p = new PrepareApplicationGroup();
			else if (group.getGroupType() == AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP)
				p = new PrepareGroupOfGroups();
			else
				p = new PrepareMixedGroup();

			p.loadGroupMembers(sessionId, addForm, group, appdefBoss,
					appdefType, nameFilter, pendingResItems, pcAvail);

			PageList availResources = p.getAvailResources();

			request.setAttribute(Constants.AVAIL_RESOURCES_ATTR, availResources);
			request.setAttribute(Constants.NUM_AVAIL_RESOURCES_ATTR,
					new Integer(availResources.getTotalSize()));

		} catch (Exception e) {
			log.error(e);
		}
		return "loadAddGroupResources";
	}

	public String save() throws Exception {
		request = getServletRequest();
		Integer rid=addForm.getRid();
        Integer entityType=addForm.getType();
        internalEid=entityType+":"+rid;
		HttpSession session = request.getSession();
		AppdefEntityID aeid = new AppdefEntityID(addForm.getType().intValue(),
				addForm.getRid());
		request.setAttribute(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());
		request.setAttribute(Constants.ACCORDION_PARAM, "1");
		request.setAttribute(Constants.RESOURCE_PARAM, addForm.getRid()
				.toString());
		request.setAttribute(Constants.RESOURCE_TYPE_ID_PARAM, addForm
				.getType().toString());

		try {
			String forward = checkSubmit(addForm);

            if (forward != null) {
                if ( forward.equalsIgnoreCase("CANCEL") ) {
                    log.trace("removing pending group list");
                    SessionUtils.removeList(session, Constants.PENDING_RESOURCES_SES_ATTR);
                } else if (forward.equalsIgnoreCase("ADDED")) {
                    log.trace("adding to pending group list");
                    SessionUtils.addToList(session, Constants.PENDING_RESOURCES_SES_ATTR, addForm.getAvailableResources());
                } else if (forward.equalsIgnoreCase("REMOVED")) {
                    log.trace("removing from pending group list");
                    SessionUtils.removeFromList(session, Constants.PENDING_RESOURCES_SES_ATTR, addForm.getPendingResources());
                }

                return forward;
            }


			Integer sessionId = RequestUtils.getSessionId(request);

			log.trace("getting pending resource list");
			List<String> pendingResourceIds = SessionUtils.getListAsListStr(
					request.getSession(), Constants.PENDING_RESOURCES_SES_ATTR);

			if (pendingResourceIds.size() == 0) {
				return SUCCESS;
			}
			log.trace("getting group [" + aeid.getID() + "]");
			AppdefGroupValue agroup = appdefBoss.findGroup(
					sessionId.intValue(), aeid.getId());
			ResourceGroup group = appdefBoss.findGroupById(
					sessionId.intValue(), agroup.getId());

			List<AppdefEntityID> newIds = new ArrayList<AppdefEntityID>();

			for (String id : pendingResourceIds) {

				AppdefEntityID entity = new AppdefEntityID(id);
				Resource r = resourceManager.findResource(entity);

				if (!resourceGroupManager.isMember(group, r)) {
					newIds.add(entity);
				}
			}

			// XXX: We have the list of resources above. Should use this
			// instead of passing in IDs.. waste of effort.
			appdefBoss.addResourcesToGroup(sessionId.intValue(), group, newIds);

			log.trace("removing pending user list");

			SessionUtils.removeList(session,
					Constants.PENDING_RESOURCES_SES_ATTR);
			addActionMessage(getText("resource.group.inventory.confirm.AddResources"));
					

			return SUCCESS;
		} catch (AppSvcClustDuplicateAssignException e1) {
			log.debug("group update failed:", e1);

			addActionError(getText("ERR_DUP_CLUSTER_ASSIGNMENT"));

			return Constants.FAILURE_URL;
		} catch (AppdefGroupNotFoundException e) {
			addActionError(getText("resource.common.inventory.error.ResourceNotFound"));

			return Constants.FAILURE_URL;
		} catch (VetoException ve) {
			addActionError(getText(
					"resource.group.inventory.error.UpdateResourceListVetoed",
					ve.getMessage()));
			return Constants.FAILURE_URL;
		}
	}

	// @Override
	// protected ActionForward checkSubmit(HttpServletRequest request,
	// ActionMapping mapping, ActionForm form,
	// Map<String, Object> params, boolean doReturnPath) throws Exception {
	// HttpSession session = request.getSession();
	// BaseValidatorForm spiderForm = (BaseValidatorForm) form;
	//
	// if (spiderForm.isCancelClicked()) {
	// log.trace("removing pending/removed resources list");
	// SessionUtils.removeList(session, Constants.PENDING_RESOURCES_SES_ATTR);
	//
	// return returnCancelled(request, mapping, params, doReturnPath);
	// }
	//
	// if (spiderForm.isResetClicked()) {
	// log.trace("removing pending/removed resources list");
	// SessionUtils.removeList(session, Constants.PENDING_RESOURCES_SES_ATTR);
	// spiderForm.reset(mapping, request);
	//
	// return returnReset(request, mapping, params);
	// }
	//
	// if (spiderForm.isCreateClicked()) {
	// return returnNew(request, mapping, params);
	// }
	//
	// if (spiderForm.isAddClicked()) {
	// log.trace("adding to pending resources list");
	// SessionUtils.addToList(session, Constants.PENDING_RESOURCES_SES_ATTR,
	// ((AddGroupResourcesForm) form)
	// .getAvailableResources());
	//
	// return returnAdd(request, mapping, params);
	// }
	//
	// if (spiderForm.isRemoveClicked()) {
	// log.trace("removing from pending resources list");
	// SessionUtils.removeFromList(session,
	// Constants.PENDING_RESOURCES_SES_ATTR, ((AddGroupResourcesForm) form)
	// .getPendingResources());
	//
	// return returnRemove(request, mapping, params);
	// }
	//
	// return null;
	// }
	@SkipValidation
	public String cancel() throws Exception {
		setHeaderResources();
		HttpSession session = this.request.getSession();
		clearErrorsAndMessages();
		this.clearCustomErrorMessages();
		internalEid = addForm.getType() + ":" + addForm.getRid();
		// internalEid = getServletRequest().getParameter("eid").toString();
		// getServletRequest().setAttribute("eid",internalEid);
        log.trace("removing pending group list");
        SessionUtils.removeList(session, Constants.PENDING_RESOURCES_SES_ATTR);
		return "cancel";
	}

	@SkipValidation
	public String reset() throws Exception {
		setHeaderResources();
		HttpSession session = this.request.getSession();
		addForm.reset();
		clearErrorsAndMessages();
		this.clearCustomErrorMessages();
		internalEid = addForm.getType() + ":" + addForm.getRid();
		 rid = getServletRequest().getParameter("rid").toString();
		 type = getServletRequest().getParameter("type").toString();
		 SessionUtils.removeList(session, Constants.PENDING_RESOURCES_SES_ATTR);
		return "reset";
	}

	public AddGroupResourcesFormNG getAddForm() {
		return addForm;
	}

	public void setAddForm(AddGroupResourcesFormNG addForm) {
		this.addForm = addForm;
	}

	public String getInternalEid() {
		return internalEid;
	}
	

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getRid() {
		return rid;
	}

	public void setRid(String rid) {
		this.rid = rid;
	}

	public void setInternalEid(String internalEid) {
		this.internalEid = internalEid;
	}

	public AddGroupResourcesFormNG getModel() {

		return addForm;
	}
	

	private abstract class PrepareResourceGroup {
		/**
		 * @return a list of available resources.
		 */
		protected abstract PageList getAvailResources();

		/**
		 * This method loads group members from the back-end.
		 */
		protected abstract void loadGroupMembers(int sessionId,
				AddGroupResourcesFormNG addForm, AppdefGroupValue group,
				AppdefBoss boss, int appdefType, String nameFilter,
				AppdefEntityID[] pendingResItems, PageControl pcAvail)
				throws Exception;
	}

	/**
	 * inner class which represents the Compatible groups.
	 */
	private class PrepareCompatGroup extends PrepareResourceGroup {
		private PageList avail = null;

		protected PageList getAvailResources() {
			return avail;
		}

		protected void loadGroupMembers(int sessionId,
				AddGroupResourcesFormNG addForm, AppdefGroupValue group,
				AppdefBoss boss, int appdefType, String nameFilter,
				AppdefEntityID[] pendingResItems, PageControl pcAvail)
				throws Exception {
			avail = boss.findCompatInventory(sessionId,
					group.getGroupEntType(), group.getGroupEntResType(),
					group.getEntityId(), pendingResItems, nameFilter, pcAvail);
		}
	}

	/**
	 * prepares a Group of Groups
	 */
	private class PrepareApplicationGroup extends PrepareResourceGroup {
		PageList availMembers = null;

		protected PageList getAvailResources() {
			return availMembers;
		}

		protected void loadGroupMembers(int sessionId,
				AddGroupResourcesFormNG addForm, AppdefGroupValue group,
				AppdefBoss boss, int appdefType, String nameFilter,
				AppdefEntityID[] pendingResItems, PageControl pcAvail)
				throws Exception {
			if (appdefType == -1)
				appdefType = AppdefEntityConstants.APPDEF_TYPE_APPLICATION;

			availMembers = boss.findCompatInventory(sessionId, appdefType, -1,
					group.getEntityId(), pendingResItems, null, pcAvail);
		}

	}

	/**
	 * Inner class which represents the Group of Groups.
	 */
	private class PrepareGroupOfGroups extends PrepareResourceGroup {
		PageList availMembers = new PageList();

		protected PageList getAvailResources() {
			return availMembers;
		}

		protected void loadGroupMembers(int sessionId,
				AddGroupResourcesFormNG addForm, AppdefGroupValue group,
				AppdefBoss boss, int appdefType, String nameFilter,
				AppdefEntityID[] pendingResItems, PageControl pcAvail)
				throws Exception {
			if (appdefType == -1)
				appdefType = AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP;

			PageList<AppdefResourceValue> compatGroups = boss
					.findCompatInventory(sessionId,
							AppdefEntityConstants.APPDEF_TYPE_GROUP,
							appdefType, group.getEntityId(), pendingResItems,
							null, pcAvail);

			for (AppdefResourceValue compatGroup : compatGroups) {
				if (!compatGroup.getName().equals(
						AuthzConstants.rootResourceGroupName)
						&& !compatGroup.getName().equals(
								AuthzConstants.groupResourceTypeName)) {
					availMembers.add(compatGroup);
				}
			}

			/**
			 * load the group type filters
			 */
			addForm.setAvailResourceTypes(buildGroupTypes());
		}
	}

	/**
	 * inner class representing the Mixed Group of PSS
	 */
	private class PrepareMixedGroup extends PrepareResourceGroup {
		PageList filteredAvailList = null;

		protected PageList getAvailResources() {
			return filteredAvailList;
		}

		protected void loadGroupMembers(int sessionId,
				AddGroupResourcesFormNG addForm, AppdefGroupValue group,
				AppdefBoss boss, int appdefType, String nameFilter,
				AppdefEntityID[] pendingResItems, PageControl pcAvail)
				throws Exception {
			if (appdefType == -1)
				appdefType = AppdefEntityConstants.APPDEF_TYPE_PLATFORM;

			filteredAvailList = boss.findCompatInventory(sessionId, appdefType,
					-1, group.getEntityId(), pendingResItems, nameFilter,
					pcAvail);

			/**
			 * load the resource type filters
			 */
			addForm.setAvailResourceTypes(buildResourceTypes());
		}
	}

	/**
	 * builds a list of resource types (platform, server, service).
	 * 
	 * @return a list of group types from the list
	 */
	private List<LabelValueBean> buildResourceTypes() {
		List<LabelValueBean> gTypes = new ArrayList<LabelValueBean>();

		LabelValueBean bv = null;
		int type = -1;
		type = AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
		bv = new LabelValueBean(AppdefEntityConstants.typeToString(type),
				Integer.toString(type));

		gTypes.add(bv);

		type = AppdefEntityConstants.APPDEF_TYPE_SERVER;
		bv = new LabelValueBean(AppdefEntityConstants.typeToString(type),
				Integer.toString(type));

		gTypes.add(bv);

		type = AppdefEntityConstants.APPDEF_TYPE_SERVICE;
		bv = new LabelValueBean(AppdefEntityConstants.typeToString(type),
				Integer.toString(type));

		gTypes.add(bv);

		return gTypes;
	}

	/**
	 * builds a unique list of group types.
	 * 
	 * @return a unique list of group types from the list
	 */
	private List<LabelValueBean> buildGroupTypes() {
		List<LabelValueBean> gTypes = new ArrayList<LabelValueBean>();

		LabelValueBean bv = null;
		int type = -1;
		type = AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP;
		bv = new LabelValueBean(
				AppdefEntityConstants.getAppdefGroupTypeName(type),
				Integer.toString(type));

		gTypes.add(bv);

		type = AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS;
		bv = new LabelValueBean(
				AppdefEntityConstants.getAppdefGroupTypeName(type),
				Integer.toString(type));

		gTypes.add(bv);

		type = AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS;
		bv = new LabelValueBean(
				AppdefEntityConstants.getAppdefGroupTypeName(type),
				Integer.toString(type));

		gTypes.add(bv);

		return gTypes;
	}

	protected void findAndSetResource(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		AppdefEntityID aeid = setResource();

		// If this is a cluster, then it's possible that it's also part of an
		// application
		Integer sessionId = RequestUtils.getSessionId(request);
		PageList<ApplicationValue> appValues = appdefBoss.findApplications(
				sessionId.intValue(), aeid, PageControl.PAGE_ALL);

		if (appValues.getTotalSize() > 0) {
			request.setAttribute(Constants.APPLICATIONS_ATTR, appValues);
		}
	}

}
