package org.hyperic.hq.auth;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.auth.shared.SubjectNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.dao.PrincipalDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class UserDetailsServiceImpl implements UserDetailsService {

    private PrincipalDAO principalDao;

    private AuthzSubjectManager authzSubjectManager;

    @Autowired
    public UserDetailsServiceImpl(PrincipalDAO principalDao, AuthzSubjectManager authzSubjectManager) {
        this.principalDao = principalDao;
        this.authzSubjectManager = authzSubjectManager;
    }

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
        Principal principal = principalDao.findByUsername(username);
        if (principal == null) {
            throw new UsernameNotFoundException("Username " + username + " not found");
        }
        boolean enabled = true;
        try {
            AuthzSubject subject = authzSubjectManager.findSubjectByAuth(username, HQConstants.ApplicationName);
            enabled = subject.getActive();
        } catch (SubjectNotFoundException fe) {
            // User not found in the authz system. Create it. This could happen
            // with LDAP and/or Kerberos Authentication the first time a user
            // authenticates
            // TODO does this logic belong elsewhere? Revisit when we add
            // LDAP/Kerberos support
            AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
            try {
                authzSubjectManager.createSubject(overlord, username, true, HQConstants.ApplicationName, "", "", "",
                    "", "", "", false);
            } catch (PermissionException e) {
                throw new PermissionDeniedDataAccessException("Error adding new subject", e);
            } catch (ApplicationException e) {
                // This should only be thrown if subject already exists. Do
                // nothing
            }
        }

        List<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();

        // ...TODO right now, every user is given the "ROLE USER" grant
        // authority, once we fully integrate with
        // spring security this should be updated with a better approach...
        grantedAuthorities.add(new GrantedAuthorityImpl("ROLE_USER"));

        return new User(username, principal.getPassword(), enabled, true, true, true, grantedAuthorities);
    }

}
