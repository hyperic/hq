package org.hyperic.hq.authz.server.session;

public interface GroupChangeCallback {
    void postGroupCreate(ResourceGroup g);
    void preGroupDelete(ResourceGroup g);
}
