aspect SessionAspect {
    pointcut setupSession(): 
		execution(public * javax.ejb.SessionBean+.*(..))
    ||  execution(javax.ejb.SessionBean+.new(..));

    Object around(): setupSession() {
        boolean setup = false;
        try {
            setup = 
                org.hyperic.hq.hibernate.SessionAspectInterceptor.setupSession();
            return proceed();
        } finally {
            if (setup)
                org.hyperic.hq.hibernate.SessionAspectInterceptor.cleanupSession();
        }
    }
}

