/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.ui.action.resource.common.inventory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.WorkflowPrepareAction;
import org.hyperic.hq.ui.action.resource.ResourceForm;
import org.hyperic.hq.ui.util.RequestUtils;

/**
 * An Action that retrieves data from the BizApp to facilitate display of the
 * form for editing a resource's general properties.
 */
public class EditResourceGeneralPropertiesFormPrepareAction
    extends WorkflowPrepareAction {

    /**
     * Retrieve the data necessary to display the
     * <code>GeneralPropertiesForm</code> page.
     * 
     */
    public ActionForward workflow(ComponentContext context, ActionMapping mapping, ActionForm form,
                                  HttpServletRequest request, HttpServletResponse response) throws Exception {

        AppdefResourceValue resource = RequestUtils.getResource(request);
        if (resource == null) {
            RequestUtils.setError(request, Constants.ERR_RESOURCE_NOT_FOUND);
            return null;
        }

        ResourceForm resourceForm = (ResourceForm) form;
        resourceForm.loadResourceValue(resource);

        return null;
    }
}
