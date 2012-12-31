package org.hyperic.hq.vm;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.CPropKeyNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;




public interface VMManager {
    public void collect(AuthzSubject subject, final String url, final String usr, final String pass) throws RemoteException, MalformedURLException, PermissionException, CPropKeyNotFoundException, AppdefEntityNotFoundException;

    public String getUuid(String mac);
}
