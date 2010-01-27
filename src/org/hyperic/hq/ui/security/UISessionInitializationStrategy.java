package org.hyperic.hq.ui.security;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

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
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.server.session.DashboardManagerEJBImpl;
import org.hyperic.hq.ui.server.session.UserDashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManagerLocal;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.image.widget.ResourceTree;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.stereotype.Component;

/*
 * This class is responsible for setting up the new session and determining whether or not the user needs to register.
 */

@Component
public class UISessionInitializationStrategy extends BaseSessionInitializationStrategy {
    private static Log log = LogFactory.getLog(UISessionInitializationStrategy.class.getName());
    
    public void onAuthentication(Authentication authentication, HttpServletRequest request, HttpServletResponse response) throws SessionAuthenticationException {
        super.onAuthentication(authentication, request, response);
        
        final boolean debug = log.isDebugEnabled();
        
        if (debug) log.debug("Initializing UI session parameters...");
        
        HttpSession session = request.getSession();
        WebUser webUser = (WebUser) session.getAttribute(Constants.WEBUSER_SES_ATTR);
        
        assert(webUser != null); // At this point webUser should never be null
        
        if (webUser.getPreferences().getKeys().size() == 0) {
            // will be cleaned out during registration
            session.setAttribute(Constants.PASSWORD_SES_ATTR, authentication.getCredentials().toString());
            session.setAttribute(Constants.NEEDS_REGISTRATION, Boolean.TRUE);
            
            if (debug) log.debug("Stashing registration parameters in the session for later use");
        }
        
        ServletContext ctx = session.getServletContext();
        AuthzBoss authzBoss = ContextUtils.getAuthzBoss(ctx);
        
        // Load up the user's dashboard preferences
        loadDashboard(ctx, webUser, authzBoss);
        
        // Determine if we can render chart images
        setXlibFlag(session);
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
            log.warn("Server cannot render chart images.", t);
            
            session.setAttribute(Constants.XLIB_INSTALLED, Boolean.FALSE);
        }
    }
    
    private void loadDashboard(ServletContext ctx, WebUser webUser, AuthzBoss authzBoss) {
        try {
            DashboardManagerLocal dashManager = DashboardManagerEJBImpl.getOne();
            ConfigResponse defaultUserDashPrefs =
                (ConfigResponse) ctx.getAttribute(Constants.DEF_USER_DASH_PREFS);
            AuthzSubject me =
                authzBoss.findSubjectById(webUser.getSessionId(),
                                          webUser.getSubject().getId());
            UserDashboardConfig userDashboard = dashManager.getUserDashboard(me, me);
            
            if (userDashboard == null) {
                userDashboard = dashManager.createUserDashboard(me, me, webUser.getName());
            }
            
            ConfigResponse userDashobardConfig = userDashboard.getConfig();
            
            if (mergeValues(userDashobardConfig, defaultUserDashPrefs, false)) {
                dashManager.configureDashboard(me, userDashboard,
                                               userDashobardConfig);
            }
        } catch (PermissionException e) {
            log.error(e);
        } catch (SessionNotFoundException e) {
            // User not logged in
            log.error(e);
        } catch (SessionTimeoutException e) {
            // User session has expired
            log.error(e);
        } catch (RemoteException e) {
            // Cannot look up this user
            log.error(e);
        }
    }
}
