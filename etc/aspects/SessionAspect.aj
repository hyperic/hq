aspect SessionAspect {
    pointcut setupSession(): 
		execution(public    * javax.ejb.SessionBean+.*(..))
	||  execution(protected * javax.ejb.SessionBean+.*(..))
    ||  execution(javax.ejb.SessionBean+.new(..))
    ||  execution(public    * org.hyperic.hq.hibernate.RequiresSession+.*(..))
	||  execution(protected * org.hyperic.hq.hibernate.RequiresSession+.*(..))
    ||  execution(org.hyperic.hq.hibernate.RequiresSession+.new(..))
	||  execution(public    * javax.jms.MessageListener+.*(..))
	||  execution(protected * javax.jms.MessageListener+.*(..))
    ||  execution(javax.jms.MessageListener+.new(..));


    Object around(): setupSession() {
        boolean setup = false;
        try {
            String meth = thisJoinPoint.getSignature().getName();
			String dbgTxt;
			
            if (meth.equals("<init>") ||
                meth.equals("ejbCreate") ||
                meth.equals("setSessionContext"))
            {
            	dbgTxt = null;
            } else {
				dbgTxt = 
				    thisJoinPoint.getSignature().getDeclaringType().getName() +
				    "." + meth;            
            }
            
            setup = 
                org.hyperic.hq.hibernate.SessionAspectInterceptor.setupSession(dbgTxt);
            return proceed();
        } finally {
            if (setup)
                org.hyperic.hq.hibernate.SessionAspectInterceptor.cleanupSession();
        }
    }
}

