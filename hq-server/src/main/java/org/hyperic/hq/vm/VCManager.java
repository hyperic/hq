package org.hyperic.hq.vm;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.CPropKeyNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;

import com.vmware.vim25.mo.ManagedEntity;

public interface VCManager {
    public Map<VMID, Set<String>> collect(final AuthzSubject subject, final String url, final String usr, final String pass) throws RemoteException, MalformedURLException, PermissionException, CPropKeyNotFoundException, AppdefEntityNotFoundException;
    public Map<VMID,Set<String>> collectUUIDs(ManagedEntity[] me, String vcUUID) throws RemoteException, MalformedURLException;

    /**
     *  retrieve a uuid of the VM the platform
     *  
     * @param macs   mac addresses of a single platform
     * @return a uuid of the VM the platform identified by these mac addresses is on
     */
    public VMID getVMID(final List<String> macs);
    
    boolean validateVCSettings(String url, String user, String password);
}
