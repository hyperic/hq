package org.hyperic.hq.ui.action.attach;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.resource.common.inventory.ResourceInventoryPortalAction;

public class TabPortalAction extends ResourceInventoryPortalAction {

	private static Properties keyMethodMap = new Properties();

	static {
		keyMethodMap.setProperty(Constants.MODE_LIST, "listViews");
	}

	public ActionForward list(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception{
		
		setResource(request);
		Portal portal = Portal.createPortal(
				"attachment.title",
				".tab.Views");
		portal.setDialog(false);
		request.setAttribute(Constants.PORTAL_KEY, portal);
		return null;
	}

}
