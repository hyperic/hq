package org.hyperic.hq.vm;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.List;

import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.CPropKeyNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;

public interface VCManager {
    public void collect(final AuthzSubject subject, final String url, final String usr, final String pass) throws RemoteException, MalformedURLException, PermissionException, CPropKeyNotFoundException, AppdefEntityNotFoundException;

    /**
     *  retrieve a uuid of the VM the platform
     *  
     * @param macs   mac addresses of a single platform
     * @return a uuid of the VM the platform identified by these mac addresses is on
     */
    public String getUuid(final List<String> macs);
    
    boolean validateVCSettings(String url, String user, String password);
}
