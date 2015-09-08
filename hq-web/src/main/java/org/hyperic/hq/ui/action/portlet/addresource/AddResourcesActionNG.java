package org.hyperic.hq.ui.action.portlet.addresource;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.StringConstants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.BaseValidatorForm;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

@Component("addNewResourcesProtletActionNG")
@Scope("prototype")
public class AddResourcesActionNG extends BaseActionNG implements ModelDriven<AddResourcesFormNG>{

    private final Log log = LogFactory.getLog(AddResourcesActionNG.class.getName());
    
    @Resource
    private ConfigurationProxy configurationProxy;
	
	private AddResourcesFormNG addForm = new AddResourcesFormNG();

	public AddResourcesFormNG getModel() {
		return addForm;
	}
	
	public AddResourcesFormNG getAddForm() {
		return addForm;
	}

	public void setAddForm(AddResourcesFormNG addForm) {
		this.addForm = addForm;
	}
	
	public String execute() throws Exception {
		this.request = getServletRequest();
        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(session);

		String forward = checkSubmitAndClear(addForm);
		
        if (forward != null) {

            if (forward.equals(BaseActionNG.CANCELED)) {
                log.trace("removing pending resources list");
                SessionUtils.removeList(session, Constants.PENDING_RESOURCES_SES_ATTR);
            } else if (forward.equals(BaseActionNG.ADD)) {
                log.trace("adding to pending resources list");
                if (addForm.getAvailableResources()!= null ) {
                	SessionUtils.addToList(session, Constants.PENDING_RESOURCES_SES_ATTR, addForm.getAvailableResources());
                }
            } else if (forward.equals(BaseActionNG.REMOVE)) {
                log.trace("removing from pending resources list");
                if (addForm.getPendingResources()!= null ) {
                	SessionUtils.removeFromList(session, Constants.PENDING_RESOURCES_SES_ATTR, addForm.getPendingResources());
                }
            }
            return forward;
        }
        
        log.trace("getting pending resources list");
        List<String> pendingResourceIds = SessionUtils.getListAsListStr(request.getSession(),
            Constants.PENDING_RESOURCES_SES_ATTR);

        StringBuffer resourcesAsString = new StringBuffer();

        for (Iterator<String> i = pendingResourceIds.iterator(); i.hasNext();) {
            resourcesAsString.append(StringConstants.DASHBOARD_DELIMITER);
            resourcesAsString.append(i.next());
        }

        SessionUtils.removeList(session, Constants.PENDING_RESOURCES_SES_ATTR);

        // RequestUtils.setConfirmation(request, "admin.user.confirm.AddResource");
        
        String currentKey = addForm.getKey();
        if (currentKey == null || currentKey.equals("")) {
        	currentKey = (String) session.getAttribute("currentPortletKey");
        }
        
        configurationProxy.setPreference(session, user, currentKey , resourcesAsString.toString());     
		return SUCCESS;
	}
	

}
