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

package org.hyperic.hq.plugin.weblogic;

import java.io.IOException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import weblogic.security.Security;
import weblogic.security.auth.callback.URLCallback;

import org.hyperic.hq.product.Metric;

import org.hyperic.util.timer.StopWatch;

/**
 * Helper for JAAS support.
 */
public class WeblogicAuth
    implements CallbackHandler {

    private static Map handlers =
        Collections.synchronizedMap(new HashMap());

    //see agent/jaas.properties
    private static final String LOGIN_MODULE = "org.hyperic.hq.weblogic";

    private static Log log = LogFactory.getLog("WeblogicAuth");

    private LoginContext loginContext = null;
    private Subject subject = null;

    private String url;
    private String username;
    private char[] passwordArray;

    private WeblogicAuth() { }
    
    public static WeblogicAuth getInstance(Metric metric) {
        return getInstance(metric.getProperties());
    }

    public static void clearCache() {
        handlers.clear();
    }

    public static WeblogicAuth getInstance(Properties props) {
        String url =
            props.getProperty(WeblogicMetric.PROP_ADMIN_URL, "");
        String username =
            props.getProperty(WeblogicMetric.PROP_ADMIN_USERNAME, "");
        String password =
            props.getProperty(WeblogicMetric.PROP_ADMIN_PASSWORD, "");

        return getInstance(url, username, password);
    }

    public static WeblogicAuth getInstance(String url,
                                           String username,
                                           String password) {
        int hash = 0;
        hash ^= url.hashCode();
        hash ^= username.hashCode();
        hash ^= password.hashCode();

        Integer key = new Integer(hash);
        WeblogicAuth auth = (WeblogicAuth)handlers.get(key);

        if (auth == null) {
            auth = new WeblogicAuth();
            handlers.put(key, auth);
            auth.url = url;
            auth.username = username;
            auth.passwordArray = password.toCharArray();
        }

        return auth;
    }

    public Object runAs(PrivilegedAction action)
        throws SecurityException {
        return Security.runAs(getSubject(), action);
    }

    public Subject getSubject()
        throws SecurityException {

        if (this.subject != null) {
            return this.subject;
        }

        StopWatch timer = null;

        if (this.loginContext == null) {
            if (log.isDebugEnabled()) {
                timer = new StopWatch();
            }

            try {
                this.loginContext = new LoginContext(LOGIN_MODULE, this);
                this.loginContext.login();
            } catch (LoginException e) {
                //e.printStackTrace();
                this.loginContext = null;
                throw new SecurityException(e.getMessage());
            }
        }

        this.subject = this.loginContext.getSubject();

        if (timer != null) {
            log.debug(this.url + " login took: " + timer);
        }

        if (this.subject == null) {
            throw new SecurityException("Authentication failed: reason unknown.");
        }

        return this.subject;
    }

    public void handle(Callback[] callbacks)
        throws IOException, UnsupportedCallbackException {

        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof TextOutputCallback) {

                TextOutputCallback toc = (TextOutputCallback)callbacks[i];

                switch (toc.getMessageType()) {
                  case TextOutputCallback.INFORMATION:
                    log.info(toc.getMessage());
                    break;

                  case TextOutputCallback.ERROR:
                    log.error(toc.getMessage());
                    break;

                  case TextOutputCallback.WARNING:
                    log.warn(toc.getMessage());
                    break;

                  default:
                    throw new IOException("Unsupported message type: " +
                                          toc.getMessageType());
                }
            }
            else if (callbacks[i] instanceof NameCallback) {
                NameCallback nc = (NameCallback)callbacks[i];
                nc.setName(this.username);
            }
            else if (callbacks[i] instanceof URLCallback) {
                URLCallback uc = (URLCallback)callbacks[i];
                uc.setURL(this.url);
            }
            else if (callbacks[i] instanceof PasswordCallback) { 
                PasswordCallback pc = (PasswordCallback)callbacks[i];
                pc.setPassword(this.passwordArray);
            }
            else {
                throw new UnsupportedCallbackException(callbacks[i],
                                                       "Unrecognized Callback");
            }
        }
    }
}
    
