package org.hyperic.hq.ui.action.attach;

import java.util.Collection;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.hqu.server.session.Attachment;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

public class MastheadAction extends BaseAction {

    public ActionForward execute(ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        // Look up the id
        Integer id = RequestUtils.getIntParameter(request, "id");
        
        // Look up the Attachment bean
        Collection attachments = (Collection)
            request.getSession().getAttribute("mastheadAttachments");
        
        for (Iterator it = attachments.iterator(); it.hasNext(); ) {
            Attachment attach = (Attachment) it.next();
            if (attach.getId().equals(id)) {
                String title = attach.getView().getDescription();
                request.setAttribute(Constants.TITLE_PARAM_ATTR, title);
                ServletContext ctx = getServlet().getServletContext();
                ProductBoss pBoss = ContextUtils.getProductBoss(ctx );
                request.setAttribute("attachment",
                    pBoss.findViewById(
                        RequestUtils.getSessionId(request).intValue(),
                        attach.getView().getId()));
                
                // Set the help text
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
