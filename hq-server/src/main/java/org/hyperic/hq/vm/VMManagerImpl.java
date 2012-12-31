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
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.CPropKeyNotFoundException;
import org.hyperic.hq.appdef.shared.CPropManager;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.vim25.GuestInfo;
import com.vmware.vim25.GuestNicInfo;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServerConnection;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

@Service("VMManagerImpl")
public class VMManagerImpl implements VMManager {
    protected final Log log = LogFactory.getLog(VMManagerImpl.class.getName());
    protected Map<String,String> macToUUID = new HashMap<String,String>();
    @Autowired
    protected PlatformManager platformMgr;
    @Autowired
    protected CPropManager propMgr;

//    @PostConstruct
//    public void afterPropertiesSet() throws Exception {
//        this.vmMgr = (VMManager) Bootstrap.getBean("VMManagerImpl");
//    }

    @Transactional(readOnly = false)
    public void collect(AuthzSubject subject, String url, String usr, String pass) throws RemoteException, MalformedURLException, PermissionException, CPropKeyNotFoundException, AppdefEntityNotFoundException {
      ServiceInstance si = new ServiceInstance(new URL(url), usr, pass, true);
//      List<VirtualMachine> vms = getAllVms(si);
//      List<VirtualMachine> vms = new ArrayList<VirtualMachine>();
      try {
      Folder rootFolder = si.getRootFolder();
      ManagedEntity[] me = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
      if(me==null || me.length==0){
          return;
//          return null;
      }
      for (Object o : me) {
//          vms.add((VirtualMachine)vm);
//      }

//      save(subject, vms);
//      for(VirtualMachine vm:vms) {
          VirtualMachine vm = (VirtualMachine)o;
          VirtualMachineConfigInfo vmConf=null;
          try { 
              vmConf = vm.getConfig();
          } catch (Throwable e) {
              log.error(e);
          }
          if (vmConf==null) {
              try { 
                  log.error("no conf info for vm " + vm.getName());
              } catch (Throwable e) {
                  log.error("no conf info for vm");
              }
              continue;
          }
          
          String uuid = vmConf.getUuid();
          if (uuid==null || uuid.equals("")) {
              try {
                  log.error("no UUID for vm " + vm.getName());
              } catch (Throwable e) {
                  log.error("no UUID for vm");
              }                
              continue;
          }
          
          GuestInfo guest = vm.getGuest();
          if (guest==null)  {
              try {
                  log.error("no guest for vm " + vm.getName());
              } catch (Throwable e) {
                  log.error("no guest for vm");
              }
              continue;
          }
          
          GuestNicInfo[] nics = guest.getNet();
          if (nics == null) {continue;}
          for (int i=0; i<nics.length; i++) {
              if (nics[i]==null)  {
                  try {
                      log.error("nic no." + i + "is null on " + vm.getName());
                  } catch (Throwable e) {
                      log.error("nic no." + i + "is null on the vm");
                  }
                  continue;
              }
                  
              String mac = nics[i].getMacAddress();
              if (mac==null) {
                  try {
                      log.error("no mac address on nic" + nics[i] + " of vm " + vm.getName());
                  } catch (Throwable e) {
                      log.error("no mac address on nic" + nics[i] + " of the vm");
                  }
                  continue;
              }
              
              macToUUID.put(mac,uuid);
              Collection<Platform> platforms = this.platformMgr.getPlatformByMacAddr(subject, mac);
              if (platforms==null || platforms.isEmpty()) {
                  if (log.isDebugEnabled()) {
                      try {
                          log.debug("no platform in the system is assosiated to any of the " + vm.getName() + " VM mac addresses");
                      } catch (Throwable e) {
                          log.debug("no platform in the system is assosiated to any of the VM mac addresses");
                      }
                  }
                  continue;
              }
              // will set UUID field for virtual and for regular platforms
              for(Platform platform:platforms) {
                  AppdefEntityID id = platform.getEntityId();
                  int typeId = platform.getAppdefResourceType().getId().intValue();
                  this.propMgr.setValue(id, typeId, "UUID", uuid);
//                  platform.setUuid(uuid);
              }
              // assume one mac address is sufficient for VM-platform mapping
              break;
          }
          //TODO~ check if updates DB by the end of the transaction
          //TODO~ make sure the uuid is extracted in the resource mapper for platforms
      }
      } finally {
          if (si!=null) {
              ServerConnection sc = si.getServerConnection();
              if (sc!=null) {
                  sc.logout();
              }
          }
      }
    }

    protected void save(AuthzSubject subject, List<VirtualMachine> vms) throws PermissionException, CPropKeyNotFoundException, AppdefEntityNotFoundException {
        for(VirtualMachine vm:vms) {
            VirtualMachineConfigInfo vmConf=null;
            try { 
                vmConf = vm.getConfig();
            } catch (Throwable e) {
                log.error(e);
            }
            if (vmConf==null) {
                try { 
                    log.error("no conf info for vm " + vm.getName());
                } catch (Throwable e) {
                    log.error("no conf info for vm");
                }
                continue;
            }
            
            String uuid = vmConf.getUuid();
            if (uuid==null || uuid.equals("")) {
                try {
                    log.error("no UUID for vm " + vm.getName());
                } catch (Throwable e) {
                    log.error("no UUID for vm");
                }                
                continue;
            }
            
            GuestInfo guest = vm.getGuest();
            if (guest==null)  {
                try {
                    log.error("no guest for vm " + vm.getName());
                } catch (Throwable e) {
                    log.error("no guest for vm");
                }
                continue;
            }
            
            GuestNicInfo[] nics = guest.getNet();
            if (nics == null) {continue;}
            for (int i=0; i<nics.length; i++) {
                if (nics[i]==null)  {
                    try {
                        log.error("nic no." + i + "is null on " + vm.getName());
                    } catch (Throwable e) {
                        log.error("nic no." + i + "is null on the vm");
                    }
                    continue;
                }
                    
                String mac = nics[i].getMacAddress();
                if (mac==null) {
                    try {
                        log.error("no mac address on nic" + nics[i] + " of vm " + vm.getName());
                    } catch (Throwable e) {
                        log.error("no mac address on nic" + nics[i] + " of the vm");
                    }
                    continue;
                }
                
                macToUUID.put(mac,uuid);
                Collection<Platform> platforms = this.platformMgr.getPlatformByMacAddr(subject, mac);
                if (platforms==null || platforms.isEmpty()) {
                    if (log.isDebugEnabled()) {
                        try {
                            log.debug("no platform in the system is assosiated to any of the " + vm.getName() + " VM mac addresses");
                        } catch (Throwable e) {
                            log.debug("no platform in the system is assosiated to any of the VM mac addresses");
                        }
                    }
                    continue;
                }
                // will set UUID field for virtual and for regular platforms
                for(Platform platform:platforms) {
                    AppdefEntityID id = platform.getEntityId();
                    int typeId = platform.getAppdefResourceType().getId().intValue();
                    this.propMgr.setValue(id, typeId, "UUID", uuid);
//                    platform.setUuid(uuid);
                }
                // assume one mac address is sufficient for VM-platform mapping
                break;
            }
            //TODO~ check if updates DB by the end of the transaction
            //TODO~ make sure the uuid is extracted in the resource mapper for platforms
        }
    }
    
//    public List<VirtualMachine> getAllVms(ServiceInstance si) throws InvalidProperty, RuntimeFault, RemoteException {
//        List<VirtualMachine> vmsList = new ArrayList<VirtualMachine>();
//        Folder rootFolder = si.getRootFolder();
//        ManagedEntity[] me = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
//        if(me==null || me.length==0){
//            return null;
//        }
//        for (Object vm : me) {
//            vmsList.add((VirtualMachine)vm);
//        }
//        si.getServerConnection().logout();
//        return vmsList;
//    }

    public String getUuid(String mac) {
        return this.macToUUID.get(mac);
    }
}
