package org.hyperic.hq.ui.security;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.stereotype.Component;

@Component
public class BaseSessionInitializationStrategy implements SessionAuthenticationStrategy {
    private static Log log = LogFactory.getLog(BaseSessionInitializationStrategy.class.getName());
    
    public void onAuthentication(Authentication authentication, HttpServletRequest request, HttpServletResponse response) throws SessionAuthenticationException
    {
        final boolean debug = log.isDebugEnabled();

        if (debug) log.debug("Initializing UI session parameters...");

        String username = authentication.getName();
        
        // The following is logic taken from the old HQ Authentication Filter
        try {
            AuthzSubjectManagerLocal authzSubjectManager = AuthzSubjectManagerEJBImpl.getOne();
            int sessionId = SessionManager.getInstance().put(authzSubjectManager.findSubjectByName(username));
            HttpSession session = request.getSession();
            ServletContext ctx = session.getServletContext();
            AuthzBoss authzBoss = ContextUtils.getAuthzBoss(ctx);
            AuthBoss authBoss = ContextUtils.getAuthBoss(ctx);

            // look up the subject record
            AuthzSubject currentSubject = authzBoss.getCurrentSubject(sessionId);
            boolean needsRegistration = false;
            
            if (currentSubject == null) {
                try {
                    AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
                    currentSubject = authzSubjectManager.createSubject(overlord, username, true, HQConstants.ApplicationName, "", "", "", "", "", "", false);
                } catch (CreateException e) {
                    throw new SessionAuthenticationException("Unable to add user to authorization system");
                }
                
                needsRegistration = true;
                sessionId = SessionManager.getInstance().put(currentSubject);
            } else {
                needsRegistration = currentSubject.getEmailAddress() == null || currentSubject.getEmailAddress().length() == 0;
            }

            AuthzSubjectValue subject = currentSubject.getAuthzSubjectValue();
            
            // figure out if the user has a principal
            boolean hasPrincipal = authBoss.isUser(sessionId, subject.getName());
            ConfigResponse preferences = needsRegistration ? new ConfigResponse() : 
                                                             getUserPreferences(ctx, sessionId, subject.getId(), authzBoss);
            WebUser webUser = new WebUser(subject, sessionId, preferences, hasPrincipal);

            // Add WebUser to Session
            session.setAttribute(Constants.WEBUSER_SES_ATTR, webUser);

            if (debug) log.debug("WebUser object created and stashed in the session");
            
            // TODO - We should use Spring Security for handling user
            // permissions...
            Map<String, Boolean> userOperationsMap = new HashMap<String, Boolean>();

            if (webUser.getPreferences().getKeys().size() > 0) {
                userOperationsMap = loadUserPermissions(webUser.getSessionId(), authzBoss);
            }

            session.setAttribute(Constants.USER_OPERATIONS_ATTR, userOperationsMap);

            if (debug) log.debug("Stashing user operations in the session");

            if (debug && needsRegistration) log.debug("Authentic user but no HQ entity, must have authenticated outside of HQ...needs registration");
        } catch (SessionException e) {
            if (debug) log.debug("Authentication of user {" + username + "} failed due to an session error.");
            
            throw new SessionAuthenticationException("login.error.application");
        } catch (RemoteException e) {
            if (debug) log.debug("Authentication of user {" + username + "} failed due to an remote error.");
            
            throw new SessionAuthenticationException("login.error.application");
        } catch (PermissionException e) {
            if (debug) log.debug("Authentication of user {" + username + "} failed due to an permissions error.");
            
            throw new SessionAuthenticationException("login.error.application");
        } catch (FinderException e) {
            if (debug) log.debug("Authentication of user {" + username + "} failed due to an finder error.");
            
            throw new SessionAuthenticationException("login.error.application");
        }
    }
    
    protected static Map<String, Boolean> loadUserPermissions(Integer sessionId, AuthzBoss authzBoss) 
    throws SessionTimeoutException, SessionNotFoundException, PermissionException, RemoteException, FinderException {
        // look up the user's permissions
        Map<String, Boolean> userOperationsMap = new HashMap<String, Boolean>();
        List<Operation> userOperations = authzBoss.getAllOperations(sessionId);
        
        for (Iterator<Operation> it = userOperations.iterator(); it.hasNext();) {
            Operation operation = it.next();
            
            userOperationsMap.put(operation.getName(), Boolean.TRUE);
        }
        
        return userOperationsMap;
    }   

    protected static ConfigResponse getUserPreferences(ServletContext ctx, Integer sessionId, Integer subjectId, AuthzBoss authzBoss) 
    throws RemoteException {
        // look up the user's preferences
        ConfigResponse defaultPreferences = (ConfigResponse) ctx.getAttribute(Constants.DEF_USER_PREFS);
        ConfigResponse preferences = authzBoss.getUserPrefs(sessionId, subjectId);
        
        preferences.merge(defaultPreferences, false);
        
        return preferences;
    }
}