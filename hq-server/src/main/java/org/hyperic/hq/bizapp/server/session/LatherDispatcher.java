package org.hyperic.hq.bizapp.server.session;

import org.hyperic.lather.LatherContext;
import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;

public interface LatherDispatcher {
    LatherValue dispatch(LatherContext ctx, String method, LatherValue arg)
        throws LatherRemoteException;

}
