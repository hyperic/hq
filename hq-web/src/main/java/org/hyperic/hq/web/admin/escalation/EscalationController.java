package org.hyperic.hq.web.admin.escalation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.DuplicateObjectException;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.shared.EscalationManager;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.web.admin.escalation.beans.EscalationForm;
import org.hyperic.hq.web.admin.escalation.beans.EscalationListUIBean;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;



/**
 * This class is the controller for Escalation Scheme
 * @author yechen
 *
 */
@Controller
public class EscalationController {
    
    private EscalationManager escalationManager;
    private SessionManager sessionManager;
    private EventsBoss eventsBoss;//should be deleted at last
    
    private Validator validator;
    
    public void setValidator(Validator validator){
        this.validator = validator;
    }
    
    @InitBinder
    protected void initBinder(WebDataBinder binder){
        binder.setValidator(new EscalationFormValidator());
    }
    
    private final Log log = LogFactory.getLog(EscalationController.class.getName());

    
    @Autowired
    public EscalationController(EscalationManager escalationManager, SessionManager sessionManager,EventsBoss eventsBoss){
        this.escalationManager = escalationManager;
        this.sessionManager = sessionManager;
        this.eventsBoss = eventsBoss;
    }
    


    /**
     * List all the escalation, if there's no escalation: will have empty "escalations" in model
     *                          if exception happens when trying to get escalations, the escalations won't be in model
     * Maps to admin/escalations.jsp
     * @throws ServletException from getSessionId
     */
    @RequestMapping(value="/admin/escalations", method=RequestMethod.GET)
    protected ModelAndView listAllEscalation(HttpServletRequest request, HttpServletResponse response) 
                                            throws ServletException{
    
        ModelAndView result = new ModelAndView();

        List<EscalationListUIBean> escalations = new ArrayList();
        try {
            escalations = getAllEscalations();
            result.addObject(escalations);
        } catch (Exception e) {
            log.debug("not able to get all escalations, so the escalations won't be in the model");
            e.printStackTrace();
        }
        return result;
    }
    
    /**
     * The subject param that escalationManager.findAll(null) take is for security concern.
     * However, there is no authorization check for "findAll." In addition, we want to get 
     * rid of this authorization, use Spring Security instead.
     */
    private List<EscalationListUIBean> getAllEscalations () {
        
        List<Escalation> allEscalations;
        try {
            allEscalations = (List<Escalation>) escalationManager.findAll(null);
        } catch (PermissionException e) {
            // won't have this exception!
            e.printStackTrace();
            log.debug("failed to get all the escalations from escaaltionManager");
            allEscalations = null;
        }
        List<EscalationListUIBean> escalations = new ArrayList<EscalationListUIBean>();
        
        for(int i=0 ; i < allEscalations.size();i++){    
            
            Escalation esc =  allEscalations.get(i);
            
           //TODO: set action number
            int actionNum = 0;
            //TODO: set alert number
            int alertNum = 0;
            
            EscalationListUIBean row = new EscalationListUIBean();
            row.setEscId(esc.getId());
            row.setEscName(esc.getName());
            row.setActionNum(actionNum);
            row.setAlertNum(alertNum);
            
            escalations.add(row);
        }
        return escalations;
    }

    /**
     * Delete the escalation
     * @throws ServletException from getSessionId
     * Security: authorization checked in escalationManager, using "alertPermissionManager.canRemoveEscalation(subject.getId());"
     */
    @RequestMapping(value="/admin/escalation/{escId}", method=RequestMethod.DELETE)
    protected String deleteEscalation(@PathVariable int escId, HttpServletRequest request,
                                             HttpSession session) throws ServletException {
        
        log.debug("entering EscalationConroller: deleteEscalation");
    
        try{
            WebUser webUser = (WebUser) session.getAttribute(Constants.WEBUSER_SES_ATTR);

            Integer sessionId = webUser.getSessionId();
             
            AuthzSubject subject = sessionManager.getSubject(sessionId);
            
            Escalation e = escalationManager.findById(escId);
            escalationManager.deleteEscalation(subject, e);

        } catch(ApplicationException e){
            RequestUtils.setError(request, "admin.config.error.escalation.CannotDelete");
        }
        
        return "redirect:/app/admin/escalations";
    }
    
    @RequestMapping(value="/admin/escalation/new", method=RequestMethod.GET)
    protected void showNewForm(Model model){
        model.addAttribute(new EscalationForm());
    }
    
    /**
     * Security: should have authorization checking
     * @param escalationForm
     * @return 
     */
    @RequestMapping(value="/admin/escalation", method=RequestMethod.POST)
    protected ModelAndView createEscalation(EscalationForm escalationForm,
                                            BindingResult result){
        log.debug("in createEscalation method.");
        ModelAndView mav = new ModelAndView();
        try {
            validator.validate(escalationForm, result);
            //TODO: shouldn't save MaxPauseTime if "isPauseAllowed" is false
            Escalation esc = escalationManager.createEscalation(escalationForm.getEscalationName(), 
                escalationForm.getDescription(), escalationForm.isPauseAllowed(), 
                escalationForm.getMaxPauseTime(), escalationForm.isNotifyAll(), 
                escalationForm.isRepeat());
            
            RedirectView redirect = new RedirectView("/app/admin/escalation/"+String.valueOf(esc.getId()));
            mav.setView(redirect);
            return mav;
            
        } catch (DuplicateObjectException e) {
            e.printStackTrace();
            mav.addObject("errorMsg", "Already has an escalation has the same name. Please change the escalation name.");//TODO: use spring messageResource to do it. Should have other form validation.
            mav.setViewName("admin/escalation/new");
            return mav;
        }

    }
    
    @RequestMapping(value="/admin/escalation/{escId}", method=RequestMethod.GET)
    protected String showEscalation(@PathVariable int escId, Model model) throws ServletException {
        Escalation escalation;
      
        try {
            escalation = (Escalation) escalationManager.findById(null, escId);

            EscalationForm escalationForm = new EscalationForm();
            escalationForm.setEscalationName(escalation.getName());
            escalationForm.setDescription(escalation.getDescription());//TODO is it possible in html?
            escalationForm.setMaxPauseTime(escalation.getMaxPauseTime());
            escalationForm.setNotifyAll(escalation.isNotifyAll());
            escalationForm.setPauseAllowed(escalation.isPauseAllowed());
            escalationForm.setRepeat(escalation.isRepeat());
            escalationForm.setId(escalation.getId());
            
            model.addAttribute(escalationForm);
        } catch (PermissionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //model.addAttribute("mode", "new");  
        return "admin/escalation";

    }
    
    @RequestMapping(value="/admin/escalation/{escId}", method=RequestMethod.PUT)
    protected String updateEscalation(@PathVariable int escId, Model model, HttpServletRequest request,
                                      HttpServletResponse response) throws ServletException {
        
        return null;
    }
    
    
   
}
