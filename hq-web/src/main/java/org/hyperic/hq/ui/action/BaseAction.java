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

package org.hyperic.hq.ui.action;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.exception.UndefinedForwardException;
import org.hyperic.hq.ui.util.ActionUtils;
import org.hyperic.hq.ui.util.SessionUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;

/**
 * An <code>Action</code> subclass that provides convenience methods
 * for recognizing form submission types (cancel, reset, ok, etc) and
 * deciding where to return after the action has completed.
 */
public class BaseAction extends Action {

    protected static final boolean YES_RETURN_PATH = true;
    protected static final boolean NO_RETURN_PATH = false;
    
    private static Log log = LogFactory.getLog(BaseAction.class.getName());

    //-------------------------------------protected methods

    /**
     * Doesn't do a thing.  It's here as a dummy method
     * used by list that don't submit a form.
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
    throws Exception {
        return null;   
    }
   
    /*
     * Finalize the method signature so that we don't mistakenly try to override
     * it when we try to convert from TileAction to Action and forget to change
     * the execute() parameter list.
     */
    public final ActionForward execute(ComponentContext context,
                                       ActionMapping mapping,
                                       ActionForm form,
                                       HttpServletRequest request,
                                       HttpServletResponse response)
        throws Exception {
        return null;
    }
        
    /**
     * Return an <code>ActionForward</code> if the form has been
     * cancelled or reset; otherwise return <code>null</code> so that
     * the subclass can continue to execute.
     */
    protected ActionForward checkSubmit(HttpServletRequest request,
                                     ActionMapping mapping, ActionForm form,
                                     Map params, boolean doReturnPath)
        throws Exception {
        BaseValidatorForm spiderForm = (BaseValidatorForm) form;

        if (spiderForm.isCancelClicked()) {
            return returnCancelled(request, mapping, params, doReturnPath);
        }

        if (spiderForm.isResetClicked()) {
            spiderForm.reset(mapping, request);
            return returnReset(request, mapping, params);
        }

        if (spiderForm.isCreateClicked()) {
            return returnNew(request, mapping, params);
        }

        if (spiderForm.isAddClicked()) {
            return returnAdd(request, mapping, params);
        }

        if (spiderForm.isRemoveClicked()) {
            return returnRemove(request, mapping, params);
        }

        return null;
    }

    protected ActionForward checkSubmit(HttpServletRequest request,
                                     ActionMapping mapping, ActionForm form,
                                     Map params)
        throws Exception {
        return checkSubmit(request, mapping, form, params, NO_RETURN_PATH);
    }

    protected ActionForward checkSubmit(HttpServletRequest request,
                                     ActionMapping mapping, ActionForm form,
                                     boolean doReturnPath)
        throws Exception {
        return checkSubmit(request, mapping, form, null, doReturnPath);
    }

    protected ActionForward checkSubmit(HttpServletRequest request,
                                     ActionMapping mapping, ActionForm form)
        throws Exception {
        return checkSubmit(request, mapping, form, null, NO_RETURN_PATH);
    }

    protected ActionForward checkSubmit(HttpServletRequest request,
                                     ActionMapping mapping, ActionForm form,
                                     String param, Object value,
                                     boolean doReturnPath)
        throws Exception {
        HashMap params = new HashMap(1);
        params.put(param, value);
        return checkSubmit(request, mapping, form, params, doReturnPath);
    }

    protected ActionForward checkSubmit(HttpServletRequest request,
                                     ActionMapping mapping, ActionForm form,
                                     String param, Object value)
        throws Exception {
        return checkSubmit(request, mapping, form, param, value, NO_RETURN_PATH);
    }
    
    /**
     * Return an <code>ActionForward</code> representing the
     * <em>add</em> form gesture, setting the return path to the
     * current URL.
     */
    protected ActionForward returnAdd(HttpServletRequest request,
                                   ActionMapping mapping,
                                   Map params)
        throws Exception {
        return constructForward(request, mapping, Constants.ADD_URL,
                                params, NO_RETURN_PATH);
    }

    protected ActionForward returnAdd(HttpServletRequest request,
                                   ActionMapping mapping)
        throws Exception {
        return returnAdd(request, mapping, null);
    }

    protected ActionForward returnAdd(HttpServletRequest request,
                                   ActionMapping mapping,
                                   String param, Object value)
        throws Exception {
        HashMap params = new HashMap(1);
        params.put(param, value);
        return returnAdd(request, mapping, params);
    }

    /**
     * Return an <code>ActionForward</code> representing the
     * <em>cancel</em> form gesture.
     */
    protected ActionForward returnCancelled(HttpServletRequest request,
                                         ActionMapping mapping,
                                         Map params, boolean doReturnPath)
        throws Exception {
            return constructForward(request, mapping, Constants.CANCEL_URL,
                                    params, doReturnPath);
    }

    protected ActionForward returnCancelled(HttpServletRequest request,
                                         ActionMapping mapping,
                                         Map params)
        throws Exception {
        return returnCancelled(request, mapping, params, YES_RETURN_PATH);
    }

    protected ActionForward returnCancelled(HttpServletRequest request,
                                         ActionMapping mapping)
        throws Exception {
        return returnCancelled(request, mapping, null);
    }

    protected ActionForward returnCancelled(HttpServletRequest request,
                                         ActionMapping mapping,
                                         String param, Object value)
        throws Exception {
        
        HashMap params = new HashMap(1);
        params.put(param, value);
        return returnCancelled(request, mapping, params);
    }
    
    /**
     * Return an <code>ActionForward</code> representing the
     * <em>failure</em> action state.
     */
    protected ActionForward returnFailure(HttpServletRequest request,
                                       ActionMapping mapping,
                                       Map params, boolean doReturnPath)
        throws Exception {
        return constructForward(request, mapping, Constants.FAILURE_URL,
                                params, doReturnPath);
    }

    protected ActionForward returnFailure(HttpServletRequest request,
                                       ActionMapping mapping,
                                       Map params)
        throws Exception {
        return returnFailure(request, mapping, params, NO_RETURN_PATH);
    }

    protected ActionForward returnFailure(HttpServletRequest request,
                                       ActionMapping mapping,
                                       boolean doReturnPath)
        throws Exception {
        return returnFailure(request, mapping, null, doReturnPath);
    }
    protected ActionForward returnFailure(HttpServletRequest request,
                                       ActionMapping mapping)
        throws Exception {
        return returnFailure(request, mapping, NO_RETURN_PATH);
    }

    protected ActionForward returnFailure(HttpServletRequest request,
                                       ActionMapping mapping,
                                       String param, Object value)
        throws Exception {
        HashMap params = new HashMap(1);
        params.put(param, value);
        return constructForward(request, mapping, Constants.FAILURE_URL,
                                params, NO_RETURN_PATH);
    }
    
    /**
     * Return an <code>ActionForward</code> representing the
     * <em>new</em> form gesture, setting the return path to the
     * current URL.
     */
    protected ActionForward returnNew(HttpServletRequest request,
                                   ActionMapping mapping,
                                   Map params)
        throws Exception {
        return constructForward(request, mapping, Constants.SUCCESS_URL, 
            params, NO_RETURN_PATH);
    }

    protected ActionForward returnNew(HttpServletRequest request,
                                   ActionMapping mapping)
        throws Exception {
        return returnNew(request, mapping, null, NO_RETURN_PATH);
    }

    protected ActionForward returnNew(HttpServletRequest request,
                                   ActionMapping mapping,
                                   String param, Object value)
        throws Exception {
        HashMap params = new HashMap();
        params.put(param, value);
        return constructForward(request, mapping, Constants.SUCCESS_URL, 
            params, NO_RETURN_PATH);
    }

     
    /**
     * Return an <code>ActionForward</code> representing the
     * <em>success</em> action state.
     */
    protected ActionForward returnNew(HttpServletRequest request,
                                       ActionMapping mapping, Map params,
                                       boolean doReturnPath)
        throws Exception {

        return constructForward(request, mapping, Constants.SUCCESS_URL,
                                params, doReturnPath);
    }
    

    protected ActionForward returnNew(HttpServletRequest request,
                                   ActionMapping mapping, String param,
                                   Object value, boolean doReturnPath)
        throws Exception {
        HashMap params = new HashMap(1);
        params.put(param, value);
        return returnNew(request, mapping, params, doReturnPath);
    }
    
    /**
     * Return an <code>ActionForward</code> representing the
     * <em>remove</em> form gesture, setting the return path to the
     * current URL.
     */
    protected ActionForward returnRemove(HttpServletRequest request,
                                      ActionMapping mapping,
                                      Map params)
        throws Exception {
        return constructForward(request, mapping, Constants.REMOVE_URL,
                                params, NO_RETURN_PATH);
    }

    protected ActionForward returnRemove(HttpServletRequest request,
                                   ActionMapping mapping)
        throws Exception {

        return returnRemove(request, mapping, null);
    }

    protected ActionForward returnRemove(HttpServletRequest request,
                                   ActionMapping mapping,
                                   String param, Object value)
        throws Exception {
        HashMap params = new HashMap(1);
        params.put(param, value);
        return returnRemove(request, mapping, params);
    }
    
    /**
     * Return an <code>ActionForward</code> representing the
     * <em>reset</em> form gesture.
     */
    protected ActionForward returnReset(HttpServletRequest request,
                                     ActionMapping mapping,
                                     Map params, boolean doReturnPath)
        throws Exception {
        return constructForward(request, mapping, Constants.RESET_URL,
                                params, doReturnPath);
    }

    protected ActionForward returnReset(HttpServletRequest request,
                                     ActionMapping mapping,
                                     Map params)
        throws Exception {
        return returnReset(request, mapping, params, NO_RETURN_PATH);
    }

    protected ActionForward returnReset(HttpServletRequest request,
                                     ActionMapping mapping)
        throws Exception {
        return returnReset(request, mapping, null);
    }

    protected ActionForward returnReset(HttpServletRequest request,
                                     ActionMapping mapping,
                                     String param, Object value)
        throws Exception {
        HashMap params = new HashMap(1);
        params.put(param, value);
        return returnReset(request, mapping, params);
    }

    /**
     * Return an <code>ActionForward</code> representing the
     * <em>okassign</em> action state.
     */
    protected ActionForward returnOkAssign(HttpServletRequest request,
                                       ActionMapping mapping, Map params)
        throws Exception {

        return returnOkAssign(request, mapping, params, YES_RETURN_PATH);
    }
    
    /**
     * Return an <code>ActionForward</code> representing the
     * <em>okassign</em> action state.
     */
    protected ActionForward returnOkAssign(HttpServletRequest request, ActionMapping mapping,
                                        Map params, boolean doReturnPath)
        throws Exception {

        return constructForward(request, mapping, Constants.OK_ASSIGN_URL, 
                                params, doReturnPath);
    }

    /**
     * Return an <code>ActionForward</code> representing the
     * <em>success</em> action state.
     */
    protected ActionForward returnSuccess(HttpServletRequest request,
                                       ActionMapping mapping, Map params,
                                       boolean doReturnPath)
        throws Exception {
        if (doReturnPath) {
            doReturnPath = !SessionUtils.getReturnPathIgnoredForOk
                ( request.getSession() ).booleanValue();
        }
        try {
            return constructForward(request, mapping, Constants.SUCCESS_URL,
                                    params, doReturnPath);
        }
        catch (UndefinedForwardException e) {
            // if there's no success forward defined, struts will send
            // us back to the same place we were before
            return null;
        }
    }

    protected ActionForward returnSuccess(HttpServletRequest request,
                                       ActionMapping mapping, Map params)
        throws Exception {
        return returnSuccess(request, mapping, params, YES_RETURN_PATH);
    }

    protected ActionForward returnSuccess(HttpServletRequest request,
                                       ActionMapping mapping)
        throws Exception {
        return returnSuccess(request, mapping, null);
    }

    protected ActionForward returnSuccess(HttpServletRequest request,
                                       ActionMapping mapping, String param,
                                       Object value, boolean doReturnPath)
        throws Exception {
        HashMap params = new HashMap(1);
        params.put(param, value);
        return returnSuccess(request, mapping, params, doReturnPath);
    }

    protected ActionForward returnSuccess(HttpServletRequest request,
                                       ActionMapping mapping, String param,
                                       Object value) throws Exception {
        return returnSuccess(request, mapping, param, value, YES_RETURN_PATH);
    }

    
    //-------------------------------------protected methods

    /**
     * Return an <code>ActionForward</code> corresponding to the given
     * form gesture or action state. Utilize the session return path
     * if it is set. Optionally set a request parameter to the path.
     */
    protected ActionForward constructForward(HttpServletRequest request,
                                             ActionMapping mapping,
                                             String forwardName,
                                             Map params,
                                             boolean doReturnPath)
        throws Exception {
        ActionForward forward = null;
        ActionForward mappedForward = mapping.findForward(forwardName);
        HttpSession session = request.getSession();

        if (mapping instanceof BaseActionMapping) {
            BaseActionMapping smap = (BaseActionMapping) mapping;
            String workflow = smap.getWorkflow();
            String returnPath = null;
            if (doReturnPath && workflow != null && !"".equals(workflow)) {
                returnPath = SessionUtils.popWorkflow(session, workflow);
            }
            if ( log.isTraceEnabled() ) {
                log.trace("forwardName=" + forwardName);
                log.trace("returnPath=" + returnPath);
            }
            if (returnPath != null) {
                boolean redirect = mappedForward != null ?
                    mappedForward.getRedirect() :
                    false;
                forward = new ActionForward( forwardName, returnPath,
                                             redirect );
            }
        }

        if (forward == null) {
            // no return path, use originally requested forward
            forward = mappedForward;
        }

        if (forward == null) {
            // requested forward not defined
            throw new UndefinedForwardException(forwardName);
        }

        if (params != null) {
            forward = ActionUtils.changeForwardPath(forward, params);
        }

        return forward;
    }

    protected ActionForward constructForward(HttpServletRequest request,
                                             ActionMapping mapping,
                                             String forwardName,
                                             String param, Object value,
                                             boolean doReturnPath)
        throws Exception {
        HashMap params = new HashMap(1);
        params.put(param, value);
        return constructForward(request, mapping, forwardName, params,
                                doReturnPath);
    }

    protected ActionForward constructForward(HttpServletRequest request,
                                             ActionMapping mapping,
                                             String forwardName,
                                             boolean doReturnPath)
        throws Exception {
        return constructForward(request, mapping, forwardName, null,
                                doReturnPath);
    }

    protected ActionForward constructForward(HttpServletRequest request,
                                             ActionMapping mapping,
                                             String forwardName)
        throws Exception {
        return constructForward(request, mapping, forwardName, null,
                                NO_RETURN_PATH);
    }
}

// EOF
