package org.hyperic.hq.authz.server.session;

import org.springframework.context.ApplicationEvent;
/**
 * Abstract class for any events in the Authz subsystem
 * @author jhickey
 *
 */
abstract public class AuthzApplicationEvent extends ApplicationEvent {

    public AuthzApplicationEvent(Object source) {
        super(source);
    }

}
