package org.hyperic.hq.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.Principal;
import org.hyperic.hq.auth.shared.SubjectNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.dao.PrincipalDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link HQAuthenticationProvider} that authenticates users
 * from HQ's database store
 * @author jhickey
 * @author dcrutchfield
 * 
 */
@Component
public class JdbcHQAuthenticationProvider implements HQAuthenticationProvider {

    private final Log log = LogFactory.getLog(JdbcHQAuthenticationProvider.class.getName());
    private PrincipalDAO principalDao;
    private AuthzSubjectManager authzSubjectManager;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public JdbcHQAuthenticationProvider(PrincipalDAO principalDao,
                                        AuthzSubjectManager authzSubjectManager,
                                        PasswordEncoder passwordEncoder) {
        this.principalDao = principalDao;
        this.authzSubjectManager = authzSubjectManager;
        this.passwordEncoder = passwordEncoder;
    }

    public Authentication authenticate(String username, String password) {
        final boolean debug = log.isDebugEnabled();
        try {
            // TODO We shouldn't have to make two calls here...
            AuthzSubject subject = authzSubjectManager.findSubjectByAuth(username,
                HQConstants.ApplicationName);
            Principal principal = principalDao.findByUsername(username);

            if (password == null || principal == null) {
                if (debug) {
                    log.debug("Authentication of user {" + username +
                              "} failed because password being null.");
                }

                throw new BadCredentialsException("login.error.login");
            } else if (!subject.getActive()) {
                if (debug) {
                    log.debug("Authentication of user {" + username +
                              "} failed because account is disabled.");
                }

                throw new BadCredentialsException("login.error.accountDisabled");
            } else if (!passwordEncoder.isPasswordValid(principal.getPassword(), password, null)) {
                if (debug) {
                    log.debug("Authentication of user {" + username +
                              "} failed due to a login error.");
                }

                throw new BadCredentialsException("login.error.login");
            }

            if (debug) {
                log.debug("Logged in as [" + username + "]");
            }
        } catch (SubjectNotFoundException e) {
            if (debug) {
                log.debug("Authentication of user {" + username + "} failed due to a login error.",
                    e);
            }
            throw new BadCredentialsException("login.error.login");
        }
        // ...finally, we need to create a list grant authorities for Spring
        // Security...
        List<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();

        // ...TODO right now, every user is given the "ROLE USER" grant
        // authority, once we fully integrate with
        // spring security this should be updated with a better approach...
        grantedAuthorities.add(new GrantedAuthorityImpl("ROLE_USER"));

        return new UsernamePasswordAuthenticationToken(username, password, grantedAuthorities);
    }

    public boolean supports(Properties serverConfigProps) {
        // We always support authentication through our internal data store
        return true;
    }

    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}