package org.hyperic.hq.ui.action.portlet.addresource;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.StringConstants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;


@Component("addResourcesPortletPrepareActionNG")
@Scope("prototype")
public class AddResourcesPrepareActionNG extends BaseActionNG implements ModelDriven<AddResourcesFormNG> {

	
    private static final String BLANK_LABEL = "";
    private static final String BLANK_VAL = "";
    private static final String PLATFORM_KEY = "resource.hub.filter.PlatformType";

	private static final String SERVER_KEY = "resource.hub.filter.ServerType";
    private static final String SERVICE_KEY = "resource.hub.filter.ServiceType";
    private static final String GROUP_ADHOC_GRP_KEY = "resource.hub.filter.GroupGroup";
    private static final String GROUP_ADHOC_GRP_VAL = new Integer(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP)
        .toString();
    private static final String GROUP_ADHOC_PSS_KEY = "resource.hub.filter.GroupPSS";
    private static final String GROUP_ADHOC_PSS_VAL = new Integer(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS)
        .toString();
    private static final String GROUP_ADHOC_APP_KEY = "resource.hub.filter.GroupApp";
    private static final String GROUP_ADHOC_APP_VAL = new Integer(AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_APP)
        .toString();

    private static final int DEFAULT_RESOURCE_TYPE = -1;
    private final Log log = LogFactory.getLog(AddResourcesPrepareActionNG.class.getName());
    
    @Resource
    private AuthzBoss authzBoss;
    @Resource
    private AppdefBoss appdefBoss;
    @Resource
    private DashboardManager dashboardManager;
    
    private AddResourcesFormNG addForm = new AddResourcesFormNG();
    
	
    @SkipValidation
	public String display() {
        
		try {

		this.request = getServletRequest();
        HttpSession session = request.getSession();
        WebUser user;
		
		user = RequestUtils.getWebUser(session);

        Integer sessionId = user.getSessionId();

        DashboardConfig dashConfig = dashboardManager.findDashboard((Integer) session
            .getAttribute(Constants.SELECTED_DASHBOARD_ID), user, authzBoss);
        ConfigResponse dashPrefs = dashConfig.getConfig();

        PageControl pcAvail = RequestUtils.getPageControl(request, "psa", "pna", "soa", "sca");
        PageControl pcPending = RequestUtils.getPageControl(request, "psp", "pnp", "sop", "scp");

        /*
         * pending resources are those on the right side of the "add to list"
         * widget- awaiting association with the group when the form's "ok"
         * button is clicked.
         */

        log.debug("check session if there are pending resources");
        List pendingResourcesIds = (List) session.getAttribute(Constants.PENDING_RESOURCES_SES_ATTR);

        if (pendingResourcesIds == null) {
            log.debug("get avalable resources from user preferences");
            try {
                String currentKey = addForm.getKey();
                if (currentKey == null || currentKey.equals("")) {
                	currentKey = (String) session.getAttribute("currentPortletKey");
                }
                pendingResourcesIds = dashPrefs.getPreferenceAsList( currentKey , StringConstants.DASHBOARD_DELIMITER);
            } catch (InvalidOptionException e) {
                // Then we don't have any pending resources
                pendingResourcesIds = new ArrayList(0);
            }
            log.debug("put entire list of pending resources in session");
            session.setAttribute(Constants.PENDING_RESOURCES_SES_ATTR, pendingResourcesIds);
        }

        log.debug("get page of pending resources selected by user");
        Pager pendingPager = Pager.getDefaultPager();
        List pendingResources = DashboardUtils.listAsResources(pendingResourcesIds, user, appdefBoss);

        PageList pageOfPendingResources = pendingPager.seek(pendingResources, pcPending.getPagenum(), pcPending
            .getPagesize());

        log.debug("put selected page of pending resources in request");

        request.setAttribute(Constants.PENDING_RESOURCES_ATTR, pageOfPendingResources);
        request.setAttribute(Constants.NUM_PENDING_RESOURCES_ATTR, new Integer(pageOfPendingResources.getTotalSize()));

        /*
         * available resources are all resources in the system that are not
         * associated with the user and are not pending
         */
        log.debug("determine if user wants to filter available resources");

        this.setFilterFromSession();
        Integer ff = addForm.getFf();
        
        AppdefEntityTypeID ft = null;

        int appdefType = (ff == null) ? Constants.FILTER_BY_DEFAULT : ff.intValue();

        if (addForm.getFt() != null && !addForm.getFt().equals(String.valueOf(DEFAULT_RESOURCE_TYPE))) {
            try {
                ft = new AppdefEntityTypeID(addForm.getFt());
            } catch (InvalidAppdefTypeException e) {
                ft = new AppdefEntityTypeID(appdefType, new Integer(addForm.getFt()));
            }
        }
        
        int resourceType = ft == null ? -1 : ft.getID();
        boolean compat = false;
        if (appdefType == 0)
            appdefType = Constants.FILTER_BY_DEFAULT;
        else if (appdefType == AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC) {
            // this is all to accomidate the compat group type as a seperate
            // dropdown
            appdefType = AppdefEntityConstants.APPDEF_TYPE_GROUP;
            compat = true;
        }

        List<AppdefEntityID> pendingEntityIds = DashboardUtils.listAsEntityIds(pendingResourcesIds);

        AppdefEntityID[] pendingEntities = pendingEntityIds.toArray(new AppdefEntityID[0]);

        PageList<AppdefResourceValue> avail;
        if (appdefType == AppdefEntityConstants.APPDEF_TYPE_GROUP) {
            int groupSubtype = -1;

            if (compat) {
                groupSubtype = AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC;
            } else {
                // resourceType straight up tells us what group
                // subtype was chosen
                groupSubtype = AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS;

                // for findCompatInventory, resourceType always need
                // to be this, for whatever reason
                resourceType = DEFAULT_RESOURCE_TYPE;
            }

            avail = appdefBoss.findCompatInventory(sessionId.intValue(), groupSubtype, appdefType,
                ft == null ? DEFAULT_RESOURCE_TYPE : ft.getType(), resourceType, addForm.getNameFilter(),
                pendingEntities, pcAvail);
        } else {
            avail = appdefBoss.findCompatInventory(sessionId.intValue(), appdefType, resourceType, null,
                pendingEntities, addForm.getNameFilter(), pcAvail);
        }

        PageList filteredAvailList = new PageList();
        Pager availPager = Pager.getDefaultPager();

        filteredAvailList = availPager.seek(avail, pcAvail.getPagenum(), pcAvail.getPagesize());

        filteredAvailList.setTotalSize(avail.getTotalSize());

        request.setAttribute(Constants.AVAIL_RESOURCES_ATTR, avail);
        request.setAttribute(Constants.NUM_AVAIL_RESOURCES_ATTR, new Integer(filteredAvailList.getTotalSize()));

        log.debug("get the available resources user can filter by");
        setDropDowns(addForm, request, sessionId.intValue(), appdefType, compat);

		} catch (Exception e) {
			log.error(e);
			
		}
		return SUCCESS;
	}
	
	private void setDropDowns(AddResourcesFormNG addForm, HttpServletRequest request, int sessionId, int appdefType, boolean compat) throws Exception {

		HttpSession session = request.getSession();
        String dropDownState = (String) session.getAttribute("typeDropDown");
        boolean fullTypeDropDown = true;
        if (dropDownState!= null && dropDownState.equals("partial")){
        	fullTypeDropDown = false;
        }
        
        
		// just need a blank one for this stuff
		PageControl pc = PageControl.PAGE_ALL;

		// set up resource "functions" (appdef entity s)
		String[][] entityTypes = appdefBoss.getAppdefTypeStrArrMap();

		// CAM's group constructs suck, so we do sucky things to support them
		// boolean pss = "platform-server-service".equals(mapping.getWorkflow());
		boolean pss=false;
		String pefix = "resource.hub.filter.";

		if (entityTypes != null) {
			for (int i = 0; i < entityTypes.length; i++) {
				int type = Integer.parseInt(entityTypes[i][0]);
				if (pss && type > AppdefEntityConstants.APPDEF_TYPE_SERVICE)
					continue;

				// suck: for the portlet's purposes, explicitly call
				// "Groups" "Mixed Groups"
				if (type == AppdefEntityConstants.APPDEF_TYPE_GROUP)
					continue;
				
				if (type == AppdefEntityConstants.APPDEF_TYPE_APPLICATION && !fullTypeDropDown)
					continue;

				addForm.addFunction(entityTypes[i][0], pefix + entityTypes[i][1]);

			}

			if (!pss && fullTypeDropDown) {
				// there are two "major" types of groups, suckah mofo
				addForm.addFunction(Integer.toString(AppdefEntityConstants.APPDEF_TYPE_GROUP), pefix + "mixedGroups" );
				addForm.addFunction(Integer.toString(AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC), pefix + "compatibleGroups" );
			}
		}

		addForm.addType("-1", "resource.hub.filter.AllResourceTypes");
		if (appdefType == AppdefEntityConstants.APPDEF_TYPE_GROUP) {
			if (compat) {
				// the entity is a compatible group- we build a
				// combined menu containing all platform, server and
				// service types
				List<PlatformTypeValue> platformTypes = appdefBoss
						.findViewablePlatformTypes(sessionId, pc);
				addCompatTypeOptions(addForm, platformTypes,
						msg(request, PLATFORM_KEY));
				List<ServerTypeValue> serverTypes = appdefBoss
						.findViewableServerTypes(sessionId, pc);
				addCompatTypeOptions(addForm, serverTypes,
						msg(request, SERVER_KEY));
				List<ServiceTypeValue> serviceTypes = appdefBoss
						.findViewableServiceTypes(sessionId, pc);
				addCompatTypeOptions(addForm, serviceTypes,
						msg(request, SERVICE_KEY));

			} else {

				addForm.addType( GROUP_ADHOC_GRP_VAL, msg(request, GROUP_ADHOC_GRP_KEY));
				addForm.addType( GROUP_ADHOC_PSS_VAL, msg(request, GROUP_ADHOC_PSS_KEY));
				addForm.addType( GROUP_ADHOC_APP_VAL, msg(request, GROUP_ADHOC_APP_KEY));
			}

		} else {
			List<AppdefResourceTypeValue> types = appdefBoss
					.findAllResourceTypes(sessionId, appdefType, pc);
			for (AppdefResourceTypeValue value : types) {
				addForm.addType( value.getId().toString(), value.getName());
			}
		}
		
		if (addForm.getTypes().size() < 2) {
			addForm.getTypes().clear();
		}
		// this.request.setAttribute("addForm", addForm);
	}
		
	private void addCompatTypeOptions(AddResourcesFormNG form,
			List<? extends AppdefResourceTypeValue> types, String label) {
		if (types.size() > 0) {
			form.addType(BLANK_LABEL, BLANK_VAL);
			form.addType(BLANK_VAL, label);
			addTypeOptions(form, types);
		}
	}
		
	private void addTypeOptions(AddResourcesFormNG form,
			List<? extends AppdefResourceTypeValue> types) {
		if (types.size() > 0) {
			for (AppdefResourceTypeValue value : types) {
				form.addType( value.getAppdefTypeKey(), value.getName());
			}
		}
	}
		
	private String msg(HttpServletRequest request, String key) {
		return RequestUtils.message(request, key);
	}
	
    public AddResourcesFormNG getAddForm() {
		return addForm;
	}

	public void setAddForm(AddResourcesFormNG addForm) {
		this.addForm = addForm;
	}

	public AddResourcesFormNG getModel() {
		return addForm;
	}
	
    @SkipValidation
    public String cancel() throws Exception {
        clearErrorsAndMessages();
        clearCustomErrorMessages();
        removeFilterSettingFromSession();
        return "cancel";
    }

    @SkipValidation
    public String reset() throws Exception {
    	addForm.reset();
    	// Set previous filter if one exists
    	this.setFilterFromSession();
        clearErrorsAndMessages();
        clearCustomErrorMessages();
        return "reset";
    }
    
    private void setFilterFromSession(){
    	HttpSession session = this.request.getSession();
        if (addForm.getFf() == null) {
        	// If filter came up empty, search to see if we have it in the session
        	addForm.setFf( (Integer) session.getAttribute("latestFf") );
        } else {
        	// keep last filter in session
        	this.setValueInSession("latestFf", addForm.getFf() );
        }
        
        if (addForm.getFt() == null) {
        	// If filter came up empty, search to see if we have it in the session
        	addForm.setFt( (String) session.getAttribute("latestFt") );       	
        } else {
        	// keep last filter in session
        	this.setValueInSession("latestFt", addForm.getFt() );
        }
        
        if (addForm.getNameFilter() == null ) {
        	// If filter came up empty, search to see if we have it in the session
        	addForm.setNameFilter( (String) session.getAttribute("latestNameFilter") );       	
        } else {
        	// keep last filter in session
        	this.setValueInSession("latestNameFilter", addForm.getNameFilter() );
        }
    }
    
    private void removeFilterSettingFromSession(){
    	this.removeValueInSession("latestNameFilter");
    	this.removeValueInSession("latestFt");
    	this.removeValueInSession("latestFf");
    	this.removeValueInSession("typeDropDown");
    }
	
}
