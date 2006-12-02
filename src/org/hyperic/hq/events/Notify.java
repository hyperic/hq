package org.hyperic.hq.events;

import javax.naming.NamingException;
import javax.mail.internet.AddressException;

/**
 */
public interface Notify
{
    public void send(Integer alertId, String message)
        throws ActionExecuteException;
}
