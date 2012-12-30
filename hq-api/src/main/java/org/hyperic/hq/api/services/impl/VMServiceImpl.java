package org.hyperic.hq.api.services.impl;

import java.net.MalformedURLException;
import java.rmi.RemoteException;


import org.hyperic.hq.api.model.cloud.CloudConfiguration;
import org.hyperic.hq.api.services.VMService;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.vm.VMManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vmware.vim25.mo.ComputeResource;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

@Component
public class VMServiceImpl extends RestApiService  implements VMService {
    private VMManager vmMgr;
    
    @Autowired
    public VMServiceImpl(VMManager vmMgr) {
        this.vmMgr=vmMgr;        
    }

    public void collect(CloudConfiguration cloudConfiguration, String hostName) throws RemoteException, MalformedURLException, SessionNotFoundException, SessionTimeoutException, PermissionException {
        ApiMessageContext apiMessageContext = newApiMessageContext();
        AuthzSubject authzSubject = apiMessageContext.getAuthzSubject();

        this.vmMgr.collect(authzSubject, cloudConfiguration.getUrl(),cloudConfiguration.getUsername(),cloudConfiguration.getPassword(), hostName);
    }
}
