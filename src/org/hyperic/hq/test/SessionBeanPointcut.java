package org.hyperic.hq.test;

import java.lang.reflect.Method;

import org.mockejb.interceptor.Pointcut;

/**
 * We use this pointcut to tell MockEJB that we need to intercept method
 * calls made to our session beans.  Fortunately we have a nice naming 
 * convention for everything.
 */
public class SessionBeanPointcut 
    implements Pointcut 
{
    public SessionBeanPointcut() {
    }

    public boolean matchesJointpoint(Method method) {
        Class owner = method.getDeclaringClass();

        return owner.getName().indexOf("EJBImpl") != -1;
    }
    
    public boolean equals(Object o){
        if (o == null || o instanceof SessionBeanPointcut == false)
            return false;
        
        return true;
    }

    public int hashCode() { 
        return super.hashCode();
    }    
}
