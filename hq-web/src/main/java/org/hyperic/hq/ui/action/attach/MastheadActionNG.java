package org.hyperic.hq.ui.action.attach;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.hqu.AttachmentDescriptor;
import org.hyperic.hq.hqu.server.session.Attachment;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.stereotype.Component;

@Component(value = "mastheadActionNG")
public class MastheadActionNG extends BaseActionNG {
	@Resource
	private ProductBoss productBoss;

	
	public String execute() throws Exception {
		// Look up the id
		Integer id = RequestUtils.getIntParameter(request, "typeId");

		int sessionId = RequestUtils.getSessionIdInt(request);
		AttachmentDescriptor attachDesc = productBoss.findAttachment(sessionId,
				id);
		if (attachDesc != null) {
			Attachment attachment = attachDesc.getAttachment();
			String title = attachDesc.getHTML();
			request.setAttribute(Constants.TITLE_PARAM_ATTR, title);

			request.setAttribute("attachment", productBoss.findViewById(
					sessionId, attachment.getView().getId()));

			request.setAttribute(Constants.PAGE_TITLE_KEY,
					attachDesc.getHelpTag());
			Portal portal = Portal.createPortal("attachment.title", "");
			request.setAttribute(Constants.PORTAL_KEY, portal);

		}

		return SUCCESS;
	}

}
