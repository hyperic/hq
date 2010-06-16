package org.hyperic.hq.ui.security;

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.bizapp.server.session.AuthBossEJBImpl;
import org.hyperic.hq.bizapp.shared.AuthBossLocal;
import org.hyperic.hq.common.AccountDisabledException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.PasswordIsNullException;
import org.hyperic.hq.common.ServerStillStartingException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;

/*
 * This class is responsible for authenticating a user using HQ's internal user store. It can also be configured to enable guest user access as well as override the guest username.
 * 
 */
public class InternalAuthenticationProvider implements AuthenticationProvider {
    private static final Log log = LogFactory.getLog(InternalAuthenticationProvider.class.getName());
                                                                     
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        final boolean debug = log.isDebugEnabled();
        
        // TODO: Once this is evolution, remove the getOne in favor of DI
        AuthBossLocal authBoss = AuthBossEJBImpl.getOne();
        AuthzSubjectManagerLocal authzManager = AuthzSubjectManagerEJBImpl.getOne();
        AuthzSubject guestUser = authzManager.getSubjectById(AuthzConstants.guestId);
        
        // First, we get the username and password from the authentication object passed in...
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
           
        // ...then we attempt to authenticate using authBoss...
        try {
            // ...check to see if we the user is trying to log in as guest user...
            int sid;
            
            if (guestUser != null && guestUser.getActive() && guestUser.getName().equalsIgnoreCase(username)) {
                sid = authBoss.loginGuest();
            } else {
                // ...this is a non guest user, authenticate...
                sid = authBoss.login(username, password);
            }
            
            if (debug) log.debug("Logged in as [" + username + "] with session id [" + sid + "]");
        } catch (LoginException e) {
            if (debug) log.debug("Authentication of user {" + username + "} failed due to a login error.");
            
            throw new BadCredentialsException("login.error.login", e);
        } catch (ApplicationException e) {
            if (debug) log.debug("Authentication of user {" + username + "} failed due to an application error.");
            
            throw new AuthenticationServiceException("login.error.application", e);
        } catch (AccountDisabledException e) {
            if (debug) log.debug("Authentication of user {" + username + "} failed because account is disabled.");
            
            throw new BadCredentialsException("login.error.accountDisabled", e);
        } catch (PasswordIsNullException e) {
            if (debug) log.debug("Authentication of user {" + username + "} failed because password being null.");
            
            throw new BadCredentialsException("login.error.login", e);
        } catch (ServerStillStartingException e) {
            if (debug) log.debug("Authentication of user {" + username + "} failed because server is still starting up.");
            
            throw new AuthenticationServiceException("login.error.serverStillStarting", e);
        }
        
        // ...finally, we need to create a list grant authorities for Spring Security...
        List<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();
        
        // ...TODO right now, every user is given the "ROLE USER" grant authority, once we fully integrate with
        // spring security this should be updated with a better approach...
        grantedAuthorities.add(new GrantedAuthorityImpl("ROLE_USER"));
       
        return new UsernamePasswordAuthenticationToken(username, password, grantedAuthorities);
    }

    public boolean supports(Class<? extends Object> authentication) {
        // ...for now, we only support objects that implement Authentication if we find that we need more information 
        // we can add some interfaces to make sure we get that information and check for it here...
        return Authentication.class.getClass().isInstance(authentication); 
    }
}
