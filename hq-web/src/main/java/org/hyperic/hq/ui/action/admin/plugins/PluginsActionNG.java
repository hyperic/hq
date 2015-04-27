package org.hyperic.hq.ui.action.admin.plugins;

import java.util.Collection;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.hqu.AttachmentDescriptor;
import org.hyperic.hq.hqu.server.session.AttachType;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.stereotype.Component;

@Component(value = "pluginsActionNG")
public class PluginsActionNG extends BaseActionNG {

	@Resource
	private ProductBoss productBoss;

	public String execute() throws Exception {

		Collection<AttachmentDescriptor> a = productBoss.findAttachments(
				RequestUtils.getSessionIdInt(request), AttachType.ADMIN);

		this.request.setAttribute("adminAttachments", a);

		return SUCCESS;
	}
}
