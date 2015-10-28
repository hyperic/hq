package org.hyperic.hq.ui.action.admin.user;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.portlet.metricviewer.ViewActionNG;
import org.springframework.stereotype.Component;
import org.hyperic.hq.ui.Constants;


@Component(value = "editUserFormPrepareActionNG")
public class EditUserFormPrepareActionNG extends BaseActionNG implements ViewPreparer {
	
	private final Log log = LogFactory.getLog(EditUserFormPrepareActionNG.class);

	public void execute(TilesRequestContext tilesContext, AttributeContext attributeContext) {
		this.request = getServletRequest();
		
		WebUser user = (WebUser) request.getAttribute(Constants.USER_ATTR);
		if (user == null) {
			try {
				setUser();
			} catch (Exception ex) {
				log.error(ex, ex);
				return;
			}
		}
		

        UserNG userForm = new UserNG();

        

        if (userForm.getFirstName() == null) {
            userForm.setFirstName(user.getFirstName());
        }
        if (userForm.getLastName() == null) {
            userForm.setLastName(user.getLastName());
        }
        if (userForm.getDepartment() == null) {
            userForm.setDepartment(user.getDepartment());
        }
        if (userForm.getName() == null) {
            userForm.setName(user.getName());
        }
        if (userForm.getEmailAddress() == null) {
            userForm.setEmailAddress(user.getEmailAddress());
        }
        if (userForm.getPhoneNumber() == null) {
            userForm.setPhoneNumber(user.getPhoneNumber());
        }
        if (userForm.getSmsAddress() == null) {
            userForm.setSmsAddress(user.getSmsaddress());
        }

        userForm.setHtmlEmail(user.isHtmlEmail());
        if (user.getActive()) {
            userForm.setEnableLogin("yes");
        } else {
            userForm.setEnableLogin("no");
        }
        request.setAttribute("userForm", userForm);
	}

}
