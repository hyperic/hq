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

    @Transactional(readOnly = false)
    public void collect(AuthzSubject subject, String url, String usr, String pass) throws RemoteException, MalformedURLException, PermissionException, CPropKeyNotFoundException, AppdefEntityNotFoundException {
      ServiceInstance si = new ServiceInstance(new URL(url), usr, pass, true);
      try {
          Folder rootFolder = si.getRootFolder();
          ManagedEntity[] me = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
          if(me==null || me.length==0){
              
              return;
          }
          for (Object o : me) {
              VirtualMachine vm = (VirtualMachine)o;
              VirtualMachineConfigInfo vmConf=null;
              String vmName = "";
              try { 
                  vmConf = vm.getConfig();
                  vmName = vm.getName();
              } catch (Throwable e) {
                  log.error(e);
              }
              if (vmConf==null) {
                  log.error("no conf info for vm " + vmName);
                  continue;
              }

              String uuid = vmConf.getUuid();
              if (uuid==null || uuid.equals("")) {
                  log.error("no UUID for vm " + vmName);
                  continue;
              }

              GuestInfo guest = vm.getGuest();
              if (guest==null)  {
                  log.error("no guest for vm " + vmName);
                  continue;
              }

              GuestNicInfo[] nics = guest.getNet();
              if (nics == null) {continue;}
              for (int i=0; i<nics.length; i++) {
                  if (nics[i]==null)  {
                      log.error("nic no." + i + "is null on " + vmName);
                      continue;
                  }

                  String mac = nics[i].getMacAddress();
                  if (mac==null || "00:00:00:00:00:00".equals(mac)) {
                      log.error("no mac address / mac address is 00:00:00:00:00:00 on nic" + nics[i] + " of vm " + vmName);
                      continue;
                  }

                  macToUUID.put(mac.toUpperCase(),uuid);
                  Collection<Platform> platforms = this.platformMgr.getPlatformByMacAddr(subject, mac);
                  if (platforms==null || platforms.isEmpty()) {
                      if (log.isDebugEnabled()) {
                          log.debug("no platform in the system is assosiated to any of the " + vmName + " VM mac addresses");
                      }
                      continue;
                  }
                  // will set UUID field for virtual and for regular platforms
                  for(Platform platform:platforms) {
                      AppdefEntityID id = platform.getEntityId();
                      int typeId = platform.getAppdefResourceType().getId().intValue();
                      this.propMgr.setValue(id, typeId, "UUID", uuid);
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

    public String getUuid(final List<String> macs) {
        for(String mac:macs) {
            String uuid = this.macToUUID.get(mac.toUpperCase());
            if (uuid!=null) {
                return uuid;
            }
        }
        return null;
    }
}
