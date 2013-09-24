package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.zevents.ZeventManager;

public class NewResourceVerifiedZevent extends ResourceZevent {

    private boolean _success;
    private Exception _failureEx;
    
    static {
        ZeventManager.getInstance()
            .registerEventClass(NewResourceVerifiedZevent.class);
    }

    public NewResourceVerifiedZevent(Integer subject, AppdefEntityID id, boolean success, Exception failureEx) {
        super(subject, id);
        this._success = success;
        this._failureEx = failureEx;
    }

    public NewResourceVerifiedZevent(AuthzSubject subject, AppdefEntityID id, boolean success, Exception failureEx) {
        super(subject.getId(), id);
        this._success = success;
        this._failureEx = failureEx;
    }

    public NewResourceVerifiedZevent(ResourceZeventSource source, ResourceZeventPayload payload, boolean success, Exception failureEx) {
        super(source, payload);
        this._success = success;
        this._failureEx = failureEx;
    }

    @Override
    public String toString() {
        String failureString = "null";
        if (_failureEx != null) {
            failureString = _failureEx.getMessage();
        }
        return "NewResourceVerifiedZevent[srcId=" + _sourceId + ", payload=" + _payload +
                ", success=" + _success + ", failureEx=" + failureString + "]";
    }

    public boolean isSuccess() {
        return _success;
    }

    public void setSuccess(boolean success) {
        this._success = success;
    }

    public Exception getFailureEx() {
        return _failureEx;
    }

    public void setFailureEx(Exception failureEx) {
        this._failureEx = failureEx;
    }
}
