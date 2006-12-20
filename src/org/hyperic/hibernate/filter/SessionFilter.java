package org.hyperic.hibernate.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.hibernate.SessionAspectInterceptor;
import org.hyperic.hq.hibernate.SessionAspectInterceptor.SessionRunner;

/**
 * This filter runs to make sure that the entire duration of the web session
 * is wrapped within a Hibernate session.
 */
public class SessionFilter
    implements Filter
{
    private static final Log _log = LogFactory.getLog(SessionFilter.class);

    public void doFilter(final ServletRequest request, 
                         final ServletResponse response,
                         final FilterChain chain) 
        throws IOException, ServletException
    {
        try {
            SessionAspectInterceptor.runInSession(new SessionRunner() {
                public void run() throws Exception {
                    chain.doFilter(request, response);
                }
            
                public String getName() {
                    return "WebThread[" + Thread.currentThread().getName() + 
                            "]";
                }
            });
        } catch(Exception e) {
            if (e instanceof ServletException) {
                throw (ServletException) e;
            } else if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                throw new ServletException("Unhandled exception", e);
            }
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void destroy() {
    }
}
