/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.plugin.netservices;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.directory.SearchControls;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Properties;

public class LDAPCollector extends NetServicesCollector {

    private static final String PROP_BASEDN    = "baseDN";
    private static final String PROP_BINDDN    = "bindDN";
    private static final String PROP_BINDPW    = "bindPW";
    private static final String PROP_FILTER    = "filter";

    private static Log log =
        LogFactory.getLog(LDAPCollector.class.getName());

    /**
     * Construct default SearchControls
     */
    private SearchControls getSearchControls() {
        // Set the scope to subtree, default is one-level
        int scope = SearchControls.SUBTREE_SCOPE;

        // Use 'socket timeout' for search timeout.
        int timeLimit = getTimeoutMillis();

        // No limit on the number of entries returned
        long countLimit = 0;

        // Attributes to return.
        String returnedAttributes[] = null;

        // Don't return the object
        boolean returnObject = false;

        // No dereferencing during the search
        boolean deference = false;

        SearchControls constraints = new SearchControls(scope,
                                                        countLimit,        
                                                        timeLimit,
                                                        returnedAttributes,
                                                        returnObject,
                                                        deference);
        return constraints;
    }

    public void collect() {

        // Setup initial LDAP properties
        Properties env = new Properties();
        Properties props = getProperties();

        // Set our default factory name if one is not given
        String factoryName = env.getProperty(Context.INITIAL_CONTEXT_FACTORY);
        if (factoryName == null) {
            env.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                            "com.sun.jndi.ldap.LdapCtxFactory");
        }

        // Set the LDAP url
        if (isSSL()) {
            env.put("java.naming.ldap.factory.socket",
            		LDAPSSLSocketFactory.class.getName());
            env.put(Context.SECURITY_PROTOCOL, "ssl");
        }
        String providerUrl = "ldap://" + getHostname() + ":" + getPort();
        env.setProperty(Context.PROVIDER_URL, providerUrl);

        // For log track
        setSource(providerUrl);

        // Follow referrals automatically
        env.setProperty(Context.REFERRAL, "follow");
        
        // Base DN
        String baseDN = props.getProperty(PROP_BASEDN);
        if (baseDN == null) {
            setErrorMessage("No Base DN given, refusing login");
            setAvailability(false);
            return;
        }

        // Search filter
        String filter = props.getProperty(PROP_FILTER);

        // Load any information we may need to bind
        String bindDN = props.getProperty(PROP_BINDDN);
        String bindPW = props.getProperty(PROP_BINDPW);
        if (bindDN != null) {
            env.setProperty(Context.SECURITY_PRINCIPAL, bindDN);
            env.setProperty(Context.SECURITY_CREDENTIALS, bindPW);
            env.setProperty(Context.SECURITY_AUTHENTICATION, "simple");
        }

        if (log.isDebugEnabled()) {
            log.debug("Using LDAP environment: " + env);
        }

        try {
            startTime();
            InitialLdapContext ctx = new InitialLdapContext(env, null);
            endTime();

            setAvailability(true);

            // If a search filter is specified, run the search and return the
            // number of matches as a metric
            if (filter != null) {
                log.debug("Using LDAP filter=" + filter);
                NamingEnumeration answer = ctx.search(baseDN, filter,
                                                      getSearchControls());

                long matches = 0;
                while (answer.hasMore()) {
                    matches++;
                    answer.next();
                }

                setValue("NumberofMatches", matches);
            }
        } catch (Exception e) {
            setAvailability(false);
            if (log.isDebugEnabled()) {
                log.debug("LDAP check failed: " + e, e);
            }

            setErrorMessage("LDAP check failed: " + e);
        }
    }
}

