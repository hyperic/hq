package org.hyperic.hq.ui.action.attach;

import java.util.Collection;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.hqu.server.session.Attachment;
import org.hyperic.hq.hqu.server.session.UIPluginManagerEJBImpl;
import org.hyperic.hq.hqu.server.session.ViewResourceCategory;
import org.hyperic.hq.hqu.shared.UIPluginManagerLocal;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

public class TabBodyAction extends BaseAction {

	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		// Look up the id
		Integer id;
		try {
			id = RequestUtils.getIntParameter(request, "id");
		} catch (Exception e) {
			id = null;
		}
		AppdefEntityID eid = RequestUtils.getEntityId(request);
		UIPluginManagerLocal pluginManager = UIPluginManagerEJBImpl.getOne();
		Collection availAttachents = pluginManager.findAttachments(eid,
				ViewResourceCategory.findByDescription("views"));
		// Set the list of avail attachments
		request.setAttribute("resourceViewTabAttachments", availAttachents);
		for (Iterator it = availAttachents.iterator(); it.hasNext();) {
			Attachment attach = (Attachment) it.next();
			if (attach.getId().equals(id)) {
				// Set the requested view
				String title = attach.getView().getDescription();
				request.setAttribute(Constants.TITLE_PARAM_ATTR, title);
				ServletContext ctx = getServlet().getServletContext();
				ProductBoss pBoss = ContextUtils.getProductBoss(ctx);
				request.setAttribute("resourceViewTabAttachment", pBoss.findViewById(
						RequestUtils.getSessionId(request).intValue(), attach
								.getView().getId()));
				String helpTag = title.replace(' ', '.');
				request.setAttribute(Constants.PAGE_TITLE_KEY, helpTag);
				break;
			}
		}
		Portal portal = Portal.createPortal("attachment.title", "");
		request.setAttribute(Constants.PORTAL_KEY, portal);
		return null;
	}
}