package org.hyperic.hq.authz.server.session;

import org.hyperic.hq.authz.server.session.GroupChangeCallback;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.common.VetoException;

public class FakeGroupChangeCallback implements GroupChangeCallback {

    public void groupMembersChanged(ResourceGroup g) {
        // TODO Auto-generated method stub

    }

    public void postGroupCreate(ResourceGroup g) {
        // TODO Auto-generated method stub

    }

    public void preGroupDelete(ResourceGroup g) throws VetoException {
        // TODO Auto-generated method stub

    }

}
