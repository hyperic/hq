package org.hyperic.hq.ui.action.portlet.recentlyApproved;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.portlet.autoDisc.PropertiesForm;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.hq.ui.util.SessionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

@Component("recentlyApprovedModifyActionNG")
public class ModifyActionNG extends BaseActionNG implements ModelDriven<PropertiesFormNG> {
	@Resource
	private ConfigurationProxy configurationProxy;
	
	PropertiesFormNG pForm=new PropertiesFormNG();

	@Autowired
	public ModifyActionNG(ConfigurationProxy configurationProxy) {
		super();
		this.configurationProxy = configurationProxy;

	}

	public String execute()
			throws Exception {

		String forward = checkSubmit(pForm);

        if (forward != null) {
            return forward;
        }
		
		HttpSession session = request.getSession();
		WebUser user = SessionUtils.getWebUser(session);
		String range = pForm.getRange().toString();

		configurationProxy.setPreference(session, user, PropertiesFormNG.RANGE,
				range);

		session.removeAttribute(Constants.USERS_SES_PORTAL);

		return SUCCESS;
	}
	
    @SkipValidation
    public String cancel() throws Exception {
        clearErrorsAndMessages();
        return "cancel";
    }

    @SkipValidation
    public String reset() throws Exception {
    	pForm.reset();
        clearErrorsAndMessages();
        return "reset";
    }

	public PropertiesFormNG getModel() {
		return pForm;
	}

	public PropertiesFormNG getpForm() {
		return pForm;
	}

	public void setpForm(PropertiesFormNG pForm) {
		this.pForm = pForm;
	}

}
