package org.hyperic.hq.api.services.impl;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

import org.hyperic.hq.api.services.VMService;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.CPropKeyNotFoundException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.vm.VCManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VMServiceImpl extends RestApiService  implements VMService {
    private VCManager vmMgr;
    
    @Autowired
    public VMServiceImpl(VCManager vmMgr) {
        this.vmMgr=vmMgr;        
    }

    public void registerVC(String url, String user, String password) throws RemoteException, MalformedURLException, SessionNotFoundException, SessionTimeoutException, PermissionException, CPropKeyNotFoundException, AppdefEntityNotFoundException {
        this.vmMgr.registerVC(url,user,password);
    }

    public boolean validateVCSettings(String url, String user, String password) throws RemoteException, MalformedURLException,
            SessionNotFoundException, SessionTimeoutException, PermissionException, CPropKeyNotFoundException, AppdefEntityNotFoundException {
        return  this.vmMgr.validateVCSettings(url, user, password);
    }
}
