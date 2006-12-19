package org.hyperic.hq.hibernate;

import org.hibernate.classic.Session;
import org.hibernate.context.CurrentSessionContext;
import org.hibernate.engine.SessionFactoryImplementor;

public class AspectizedCurrentSession implements CurrentSessionContext {
    public AspectizedCurrentSession(SessionFactoryImplementor factory) {
    }

    public Session currentSession() {
        return (Session)SessionAspectInterceptor.currentSession();
    }
}
