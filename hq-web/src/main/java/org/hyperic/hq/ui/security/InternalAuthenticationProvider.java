package org.hyperic.hq.ui.security;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.Principal;
import org.hyperic.hq.auth.shared.SubjectNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.dao.PrincipalDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.authentication.encoding.PlaintextPasswordEncoder;
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
    private PrincipalDAO principalDao;
    private AuthzSubjectManager authzSubjectManager;
    private PasswordEncoder passwordEncoder = new PlaintextPasswordEncoder();
    
    @Autowired
    public InternalAuthenticationProvider(PrincipalDAO principalDao, AuthzSubjectManager authzSubjectManager) {
    	this.principalDao = principalDao;
    	this.authzSubjectManager = authzSubjectManager;
    }
    
    public PasswordEncoder getPasswordEncoder() {
		return passwordEncoder;
	}

	public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        final boolean debug = log.isDebugEnabled();
        
        // First, we get the username and password from the authentication object passed in...
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();     

        // ...then we check if we're dealing with a guest user...
        AuthzSubject guestUser = authzSubjectManager.findSubjectById(AuthzConstants.guestId);
        
        if (guestUser == null || !guestUser.getActive() || !guestUser.getName().equalsIgnoreCase(username)) {
        	// ...we're not dealing with a guest authentication...
        	try {
        		// TODO We shouldn't have to make two calls here...
        		AuthzSubject subject = authzSubjectManager.findSubjectByAuth(username, HQConstants.ApplicationName);
        		Principal principal = principalDao.findByUsername(username);
                
        		if (password == null || principal == null) {
    		        if (debug) log.debug("Authentication of user {" + username + "} failed because password being null.");
    		        
    		        throw new BadCredentialsException("login.error.login");           		
            	} else if (!subject.getActive()) {
    		        if (debug) log.debug("Authentication of user {" + username + "} failed because account is disabled.");
    		        
    		        throw new BadCredentialsException("login.error.accountDisabled");            		
            	} else if (!passwordEncoder.isPasswordValid(principal.getPassword(), password, null)) {
            		if (debug) log.debug("Authentication of user {" + username + "} failed due to a login error.");
    		        
    		        throw new BadCredentialsException("login.error.login");            		
            	}
        		
        		if (debug) log.debug("Logged in as [" + username + "]");
        	} catch(SubjectNotFoundException e) {
        		if (debug) log.debug("Authentication of user {" + username + "} failed due to a login error.", e);
		        
		        throw new BadCredentialsException("login.error.login");
        	}
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
