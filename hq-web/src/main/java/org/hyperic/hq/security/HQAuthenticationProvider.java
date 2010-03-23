package org.hyperic.hq.security;

import java.util.Properties;

import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
/**
 * Internal authenticators for HQ
 * @author jhickey
 *
 */
public interface HQAuthenticationProvider extends Ordered {

    /**
     * 
     * @param serverConfigProps The server configuration properties, should
     *        include authentication-related props
     * @return true if the server configuration indicates that this type of
     *         authentication should be used
     */
    boolean supports(Properties serverConfigProps);

    /**
     * 
     * @param username The name of the user to authenticate
     * @param password The password
     * @return The authenticated user's information as retrieved and populated
     *         by the specific implementation. Should not be null.
     * @throws AuthenticationException If user cannot be authenticated or if an
     *         error occurs attempting to authenticate
     */
    Authentication authenticate(String username, String password) throws AuthenticationException;

}
