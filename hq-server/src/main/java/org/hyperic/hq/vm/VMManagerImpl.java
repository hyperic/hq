package org.hyperic.hq.vm;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformManagerImpl;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.vim25.GuestInfo;
import com.vmware.vim25.GuestNicInfo;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

@Service("VMManagerImpl")
//@Transactional
public class VMManagerImpl implements VMManager {
    protected final Log log = LogFactory.getLog(VMManagerImpl.class.getName());
    protected Map<String,String> macToUUID = new HashMap<String,String>();
    @Autowired
    protected PlatformManager platformMgr;
    
    private static final String HOST_SYSTEM = "HostSystem";
    private static final String DATACENTER = "Datacenter";
    private static final String CLOUD_CONFIGURATION = "cloud_configuration";
    
    public void collect(AuthzSubject subject, String url, String usr, String pass, String hostName) throws RemoteException, MalformedURLException, PermissionException {
      ServiceInstance si = new ServiceInstance(new URL(url), usr, pass, true);
      
      List<VirtualMachine> vms = getAllVms(si, hostName);
      save(subject, vms);
    }

    @Transactional(readOnly = false)
    protected void save(AuthzSubject subject, List<VirtualMachine> vms) throws PermissionException {
        for(VirtualMachine vm:vms) {
            VirtualMachineConfigInfo vmConf = vm.getConfig();
            if (vmConf==null) {
                if (log.isDebugEnabled()) {
                    log.debug("no conf info for vm " + vm.getName());
                }
                continue;
            }
            
            String uuid = vmConf.getUuid();
            if (uuid==null || uuid.equals("")) {
                if (log.isDebugEnabled()) {
                    log.debug("no UUID for vm " + vm.getName());
                }
                continue;
            }
            
            GuestInfo guest = vm.getGuest();
            if (guest==null)  {
                if (log.isDebugEnabled()) {
                    log.debug("no guest for vm " + vm.getName());
                }
                continue;
            }
            
            GuestNicInfo[] nics = guest.getNet();
            if (nics == null) {continue;}
            for (int i=0; i<nics.length; i++) {
                if (nics[i]==null)  {
                    if (log.isDebugEnabled()) {
                        log.debug("nic no." + i + "is null on " + vm.getName());
                    }
                    continue;
                }
                
                String mac = nics[i].getMacAddress();
                if (mac==null) {
                    if (log.isDebugEnabled()) {
                        log.debug("no mac address on nic" + nics[i] + " of vm " + vm.getName());
                    }
                    continue;
                }
                
                macToUUID.put(mac,uuid);
                Collection<Platform> platforms = this.platformMgr.getPlatformByMacAddr(subject, mac);
                if (platforms==null || platforms.isEmpty()) {
                    if (log.isDebugEnabled()) {
                        log.debug("no platform in the system is assosiated to any of the " + vm.getName() + " VM mac addresses");
                    }
                    continue;
                }
                // will set UUID field for virtual and for regular platforms
                for(Platform platform:platforms) {
                    platform.setUuid(uuid);
                }
                // assume one mac address is sufficient for VM-platform mapping
                break;
             }
            //TODO~ check if updates DB by the end of the transaction
            //TODO~ make sure the uuid is extracted in the resource mapper for platforms
        }
    }
    
    public List<VirtualMachine> getAllVms(ServiceInstance si, String hostName) throws InvalidProperty, RuntimeFault, RemoteException {
        List<VirtualMachine> vmsList = new ArrayList<VirtualMachine>();
        Folder rootFolder = si.getRootFolder();
        ManagedEntity[] me = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
        if(me==null || me.length==0){
            return null;
        }
        for (Object vm : me) {
            vmsList.add((VirtualMachine)vm);
        }
        si.getServerConnection().logout();
        return vmsList;
    }

    public String getUuid(String mac) {
        return this.macToUUID.get(mac);
    }
}
