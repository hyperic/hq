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
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.util.ConfigPropertyException;
import org.springframework.security.authentication.AuthenticationProvider;
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
    private static Log log = LogFactory.getLog(InternalAuthenticationProvider.class.getName());
    
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // TODO: Once this is evolution, remove the getOne in favor of DI
        AuthBossLocal authBoss = AuthBossEJBImpl.getOne();
        AuthzSubjectManagerLocal authzManager = AuthzSubjectManagerEJBImpl.getOne();
        AuthzSubject guestUser = authzManager.findSubjectById(AuthzConstants.guestId);
        
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
            
            if (log.isTraceEnabled()) {
                log.trace("Logged in as [" + username + "] with session id [" + sid + "]");
            }
        } catch (SecurityException e) {
            if (log.isDebugEnabled()) {
                log.debug("Authentication of user {" + username + "} failed.", e);
            }
            
            throw new BadCredentialsException("Login failed", e);
        } catch (LoginException e) {
            if (log.isDebugEnabled()) {
                log.debug("Authentication of user {" + username + "} failed.", e);
            }
            
            throw new BadCredentialsException("Login failed", e);
        } catch (ApplicationException e) {
            if (log.isDebugEnabled()) {
                log.debug("Authentication of user {" + username + "} failed.", e);
            }
            
            throw new BadCredentialsException("Login failed", e);
        } catch (ConfigPropertyException e) {
            if (log.isDebugEnabled()) {
                log.debug("Authentication of user {" + username + "} failed.", e);
            }
            
            throw new BadCredentialsException("Login failed", e);
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
