package org.hyperic.hq.hqu.grails.hqugapi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.hqu.grails.commons.WebUserWrapper;
import org.hyperic.hq.ui.WebUser;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base abstract class for api implementations having need to use user level
 * authentication against core manager beans.
 */
public abstract class BaseHQUGApi {

	private static final Log log = LogFactory.getLog(BaseHQUGApi.class);

	private AuthzBoss authzBoss = Bootstrap.getBean(AuthzBoss.class);

    @Autowired
    private WebUserWrapper webUserWrapper;
    
    public BaseHQUGApi(){}

    /**
     * Returns authz subject from wrapped and session scoped webuser instance.
     * 
     * @return AuthzSubject, null if subject was not found.
     */
    protected AuthzSubject getSubject() {
    	AuthzSubject user = null;
    	WebUser webUser = webUserWrapper.getWebUser();
		Integer bossSession = webUser.getSessionId();
		try {
			user = authzBoss.getCurrentSubject(bossSession.intValue());
		} catch (SessionException e) {
			log.warn("Could not get authz subject. Is request coming outside of controller?", e);
		}
    	return user;
    }


}
