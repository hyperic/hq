package org.hyperic.hq.ui.security;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

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
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.server.session.UserDashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.image.widget.ResourceTree;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.stereotype.Component;

/*
 * This class is responsible for setting up the new session and determining whether or not the user needs to register.
 */

@Component
public class SessionInitializationStrategy implements SessionAuthenticationStrategy {
    private final Log log = LogFactory.getLog(SessionInitializationStrategy.class.getName());
    private AuthzSubjectManager authzSubjectManager;
    private DashboardManager dashboardManager;
    private AuthzBoss authzBoss;
    private AuthBoss authBoss;
    
    
    
    @Autowired
    public SessionInitializationStrategy(AuthzSubjectManager authzSubjectManager, DashboardManager dashboardManager,
                                         AuthzBoss authzBoss, AuthBoss authBoss) {
        this.authzSubjectManager = authzSubjectManager;
        this.dashboardManager = dashboardManager;
        this.authzBoss = authzBoss;
        this.authBoss = authBoss;
    }

    public void onAuthentication(Authentication authentication, HttpServletRequest request, HttpServletResponse response) throws SessionAuthenticationException {
        if (log.isDebugEnabled()) {
            log.debug("Initializing User Preferences...");
        }
        
        // The following is logic taken from the old HQ Authentication Filter
        try {
           
            String username = authentication.getName();
            AuthzSubject subj = authzSubjectManager.findSubjectByName(username);
            int sessionId = SessionManager.getInstance().put(subj);
            boolean needsRegistration = false;
            HttpSession session = request.getSession();
            ServletContext ctx = session.getServletContext();
          
                    
            // look up the subject record
            AuthzSubject subjPojo = authzBoss.getCurrentSubject(sessionId);
            AuthzSubjectValue subject = null;
                
            if (subjPojo == null) {
                subject = new AuthzSubjectValue();
                subject.setName(username);
                needsRegistration = true;
            } else {
                subject = subjPojo.getAuthzSubjectValue();
                needsRegistration = subjPojo.getEmailAddress() == null || subjPojo.getEmailAddress().length() == 0;
            }
            
            if (needsRegistration && log.isDebugEnabled()) {
                log.debug("Authentic user but no HQ entity...must have authenticated against LDAP/Kerberos/Other authentication handler...needs registration");
            }
            
            // figure out if the user has a principal
            boolean hasPrincipal = authBoss.isUser(sessionId, subject.getName());
            ConfigResponse preferences = needsRegistration ? new ConfigResponse() : getUserPreferences(ctx, sessionId, subject.getId(), authzBoss);
            WebUser webUser =  new WebUser(subject, sessionId, preferences, hasPrincipal);
            
            // Add WebUser to Session
            session.setAttribute(Constants.WEBUSER_SES_ATTR, webUser);
            
            if (log.isDebugEnabled()) {
                log.debug("WebUser object created and stashed in the session");
            }
            
            // TODO - We should use Spring Security for handling user permissions...
            Map<String, Boolean> userOperationsMap = new HashMap<String, Boolean>();
                    
            if (webUser.getPreferences().getKeys().size() == 0) {
                // will be cleaned out during registration
                session.setAttribute(Constants.PASSWORD_SES_ATTR, authentication.getCredentials().toString());
                session.setAttribute(Constants.NEEDS_REGISTRATION, Boolean.TRUE);
                
                if (log.isDebugEnabled()) {
                    log.debug("Stashing registration parameters in the session for later use");
                }
            } else {
                userOperationsMap = loadUserPermissions(webUser.getSessionId(), authzBoss);
            }
                    
            session.setAttribute(Constants.USER_OPERATIONS_ATTR, userOperationsMap);
            
            // Load up the user's dashboard preferences
            loadDashboard(ctx, webUser, authzBoss);
            setXlibFlag(session);
            
            if (log.isDebugEnabled()) {
                log.debug("Stashing user operations in the session");
            }
        } catch (SessionException e) {
            log.error(e);
            
            throw new SessionAuthenticationException("Session exception occurred");
        }  catch (PermissionException e) {
            log.error(e);
            
            throw new SessionAuthenticationException("Permission exception occurred");
        } 
    }
    
    private boolean mergeValues(ConfigResponse config, ConfigResponse other, boolean overWrite) {
        boolean updated = true;
        Set<Entry<Object,Object>> entrySet = other.toProperties().entrySet();
        
        for (Iterator<Entry<Object, Object>> i = entrySet.iterator(); i.hasNext();) {
            Entry<Object, Object> entry = i.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();

            if (overWrite || config.getValue(key) == null) {
                config.setValue(key, value);
                updated = true;
            }
        }
        return updated;
    }
    
    private static void setXlibFlag(HttpSession session) {
        try {
            new ResourceTree(1); // See if graphics engine is present
            session.setAttribute(Constants.XLIB_INSTALLED, Boolean.TRUE);
        } catch (Throwable t) {
            session.setAttribute(Constants.XLIB_INSTALLED, Boolean.FALSE);
        }
    }
    
    private void loadDashboard(ServletContext ctx, WebUser webUser, AuthzBoss authzBoss) {
        try {
            
            ConfigResponse defaultUserDashPrefs =
                (ConfigResponse) ctx.getAttribute(Constants.DEF_USER_DASH_PREFS);
            AuthzSubject me =
                authzBoss.findSubjectById(webUser.getSessionId(),
                                          webUser.getSubject().getId());
            UserDashboardConfig userDashboard = dashboardManager.getUserDashboard(me, me);
            
            if (userDashboard == null) {
                userDashboard = dashboardManager.createUserDashboard(me, me, webUser.getName());
            }
            
            ConfigResponse userDashobardConfig = userDashboard.getConfig();
            
            if (mergeValues(userDashobardConfig, defaultUserDashPrefs, false)) {
                dashboardManager.configureDashboard(me, userDashboard,
                                               userDashobardConfig);
            }
        } catch (PermissionException e) {
            e.printStackTrace();
        } catch (SessionNotFoundException e) {
            // User not logged in
        } catch (SessionTimeoutException e) {
            // User session has expired
        } 
    }
    
    private Map<String, Boolean> loadUserPermissions(Integer sessionId, AuthzBoss authzBoss) 
    throws SessionTimeoutException, SessionNotFoundException, PermissionException  {
        // look up the user's permissions
        Map<String, Boolean> userOperationsMap = new HashMap<String, Boolean>();
        List<Operation> userOperations = authzBoss.getAllOperations(sessionId);
        
        for (Iterator<Operation> it = userOperations.iterator(); it.hasNext();) {
            Operation operation = it.next();
            
            userOperationsMap.put(operation.getName(), Boolean.TRUE);
        }
        
        return userOperationsMap;
    }

    private ConfigResponse getUserPreferences(ServletContext ctx, Integer sessionId, Integer subjectId, AuthzBoss authzBoss) {
        // look up the user's preferences
        ConfigResponse defaultPreferences = (ConfigResponse) ctx.getAttribute(Constants.DEF_USER_PREFS);
        ConfigResponse preferences = authzBoss.getUserPrefs(sessionId, subjectId);
        
        preferences.merge(defaultPreferences, false);
        
        return preferences;
    }
}
