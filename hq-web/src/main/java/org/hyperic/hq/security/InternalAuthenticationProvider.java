package org.hyperic.hq.security;

import java.util.Properties;
import java.util.Set;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.util.ConfigPropertyException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * This class is responsible for authenticating a user using HQ's internal user
 * store. It can also be configured to enable guest user access as well as
 * override the guest username.
 */
public class InternalAuthenticationProvider implements AuthenticationProvider {

    private AuthzSubjectManager authzSubjectManager;
    private Set<HQAuthenticationProvider> hqAuthenticationProviders;
    private ServerConfigManager serverConfigManager;

    public InternalAuthenticationProvider(AuthzSubjectManager authzSubjectManager,
                                          Set<HQAuthenticationProvider> hqAuthenticationProviders, ServerConfigManager serverConfigManager) {
        this.authzSubjectManager = authzSubjectManager;
        this.hqAuthenticationProviders = hqAuthenticationProviders;
        this.serverConfigManager = serverConfigManager;
    }

    public Authentication authenticate(Authentication authentication)
        throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        // Check if we're dealing with a guest user...
        AuthzSubject guestUser = authzSubjectManager.findSubjectById(AuthzConstants.guestId);

        if (guestUser == null || !guestUser.getActive() ||
            !guestUser.getName().equalsIgnoreCase(username)) {
            // ...we're not dealing with a guest authentication...
            AuthenticationException lastException = null;
            Authentication result = null;
            Properties serverConfig;
            try {
                serverConfig = serverConfigManager.getConfig();
            } catch (ConfigPropertyException e) {
                throw new AuthenticationServiceException(
                    "Unable to read server configuration to determine authentication type", e);
            }
            for (HQAuthenticationProvider authProvider : hqAuthenticationProviders) {
                if (authProvider.supports(serverConfig)) {
                    try {
                        result = authProvider.authenticate(username, password);
                    } catch (AuthenticationException e) {
                        lastException = e;
                    }
                    if (result != null) {
                        return result;
                    }
                }
            }
            if (lastException != null) {
                throw lastException;
            }
        }
        return null;
    }

    public boolean supports(Class<? extends Object> authentication) {
        // ...for now, we only support objects that implement Authentication if
        // we find that we need more information
        // we can add some interfaces to make sure we get that information and
        // check for it here...
        return Authentication.class.getClass().isInstance(authentication);
    }
}
