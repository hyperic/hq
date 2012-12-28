package org.hyperic.hq.vm;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;




public interface VMManager {
    public void collect(AuthzSubject subject, final String url, final String usr, final String pass, String hostName) throws RemoteException, MalformedURLException, PermissionException;

    public String getUuid(String mac);
}
