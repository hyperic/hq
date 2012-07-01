package org.hyperic.hq.api.security;

import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.api.services.impl.RestApiService;
import org.hyperic.hq.auth.server.session.UserAuditFactory;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.RoleManager;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.shared.HQConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.stereotype.Component;

@Component
public class ApiSessionInitializationStrategy implements SessionAuthenticationStrategy {

    private static Log log = LogFactory.getLog(ApiSessionInitializationStrategy.class.getName());
    private SessionManager sessionManager;
    private AuthzSubjectManager authzSubjectManager;
    private AuthzBoss authzBoss;
    private AuthBoss authBoss;
    private UserAuditFactory userAuditFactory;
    private RoleManager roleManager;
    
    @Autowired
    public ApiSessionInitializationStrategy(AuthBoss authBoss, AuthzBoss authzBoss,
                                             AuthzSubjectManager authzSubjectManager,
                                             UserAuditFactory userAuditFactory,
                                             SessionManager sessionManager, RoleManager roleManager) {
        this.authBoss = authBoss;
        this.authzBoss = authzBoss;
        this.authzSubjectManager = authzSubjectManager;
        this.sessionManager = sessionManager;       
        this.userAuditFactory = userAuditFactory;
        this.roleManager = roleManager;
    }
    
    public void onAuthentication(Authentication authentication, HttpServletRequest request,
                                 HttpServletResponse response)
    throws SessionAuthenticationException {
        final boolean debug = log.isDebugEnabled();

        if (debug) log.debug("Initializing API session parameters...");

        String username = authentication.getName();
        
        // The following is logic taken from the old HQ Authentication Filter
        try {
            int sessionId = sessionManager.put(authzSubjectManager.findSubjectByName(username));
            HttpSession session = request.getSession();
            ServletContext ctx = session.getServletContext();
            
            // look up the subject record
            AuthzSubject subj = authzBoss.getCurrentSubject(sessionId);
            boolean needsRegistration = false;
            
            if (subj == null) {
                try {
                    AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
                    subj = authzSubjectManager.createSubject(
                        overlord, username, true, HQConstants.ApplicationName, "", "", "", "",
                        "", "", false);
                    //every user has ROLE_HQ_USER.  If other roles assigned, automatically assign them to new user
                    if(authentication.getAuthorities().size() > 1) {
                        Collection<Role> roles = roleManager.getAllRoles();
                        for(GrantedAuthority authority: authentication.getAuthorities()) {
                            if(authority.getAuthority().equals("ROLE_HQ_USER")) {
                                continue;
                            }
                            for(Role role: roles) {
                                if(("ROLE_" + role.getName()).equalsIgnoreCase(authority.getAuthority())) {
                                    roleManager.addSubjects(authzSubjectManager.getOverlordPojo(), role.getId(), 
                                        new Integer[] {subj.getId()});
                                }
                            }
                        }
                    }
                } catch (ApplicationException e) {
                    throw new SessionAuthenticationException(
                        "Unable to add user to authorization system");
                }
                
                needsRegistration = true;
                sessionId = sessionManager.put(subj);
            } else {
                needsRegistration = subj.getEmailAddress() == null ||
                                    subj.getEmailAddress().length() == 0;
            }

            userAuditFactory.loginAudit(subj);
            AuthzSubjectValue subject = subj.getAuthzSubjectValue();
            
            // figure out if the user has a principal
            boolean hasPrincipal = authBoss.isUser(sessionId, subject.getName());

            ApiUser webUser = new ApiUser(subject, sessionId, hasPrincipal);

            // Add WebUser to Session
            session.setAttribute(RestApiService.APIUSER_SES_ATTR, webUser);

            if (debug) log.debug("ApiUser object created and stashed in the session");
            
        } catch (SessionException e) {
            if (debug) {
                log.debug("Authentication of user {" + username + "} failed due to an session error.");
            }
            
            throw new SessionAuthenticationException("login.error.application");
        }
    }

    
}
