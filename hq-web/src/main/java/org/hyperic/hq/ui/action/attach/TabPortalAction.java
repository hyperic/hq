package org.hyperic.hq.ui.action.attach;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.resource.common.inventory.ResourceInventoryPortalAction;
import org.springframework.beans.factory.annotation.Autowired;

public class TabPortalAction
    extends ResourceInventoryPortalAction {

    @Autowired
    public TabPortalAction(AppdefBoss appdefBoss, AuthzBoss authzBoss, ControlBoss controlBoss) {
        super(appdefBoss, authzBoss, controlBoss);
    }

    public ActionForward list(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                              HttpServletResponse response) throws Exception {

        setResource(request);
        Portal portal = Portal.createPortal("attachment.title", ".tab.Views");
        portal.setDialog(false);
        request.setAttribute(Constants.PORTAL_KEY, portal);
        return null;
    }

}
