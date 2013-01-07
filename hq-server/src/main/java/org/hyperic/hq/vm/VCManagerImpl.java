package org.hyperic.hq.vm;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.CPropKeyNotFoundException;
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

@Service("VCManagerImpl")
public class VCManagerImpl implements VCManager {
    protected final Log log = LogFactory.getLog(VCManagerImpl.class.getName());
    @Autowired
    protected VCDAO vcDao;
    protected PlatformManager platformMgr;
    @Autowired
    public void setPlatformMgr(PlatformManager platformMgr) {
        this.platformMgr = platformMgr;
    }

    protected Map<String,Set<String>> collectUUIDs(final String url, final String usr, final String pass) throws RemoteException, MalformedURLException {
        ServiceInstance si = new ServiceInstance(new URL(url), usr, pass, true);
        try {
            Folder rootFolder = si.getRootFolder();
            ManagedEntity[] me = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
            if(me==null || me.length==0){
                if (log.isDebugEnabled()) {
                    log.debug("no virtual machines were discovered on " + url);
                }
                return null;
            }
            Map<String,Set<String>> uuidToMacsMap = new HashMap<String,Set<String>>();
            for (Object o : me) {
                // gather data from the vc
                VirtualMachine vm = (VirtualMachine)o;
                String vmName = "";
                String uuid = "";
                GuestNicInfo[] nics = null;
                try { 
                    VirtualMachineConfigInfo vmConf = vm.getConfig();
                    if (vmConf==null) {
                        log.error("no conf info for vm " + vmName);
                        continue;
                    }
                    GuestInfo guest = vm.getGuest();
                    if (guest==null)  {
                        log.error("no guest for vm " + vmName);
                        continue;
                    }
                    nics = guest.getNet();
                    if (nics == null || nics.length==0) {
                        log.error("no nics defined on vm " + vmName);
                        continue;
                    }
                    vmName = vm.getName();
                    uuid = vmConf.getUuid();
                    if (uuid==null || uuid.equals("")) {
                        log.error("no UUID discovered on vm " + vmName);
                        continue;
                    }
                } catch (Throwable e) {
                    log.error(e);
                    continue;
                }

                // gather macs
                for (int i=0; i<nics.length; i++) {
                    if (nics[i]==null)  {
                        log.error("nic no." + i + " is null on " + vmName);
                        continue;
                    }
                    String mac = nics[i].getMacAddress();
                    if (mac==null || "00:00:00:00:00:00".equals(mac)) {
                        log.error("no mac address / mac address is 00:00:00:00:00:00 on nic" + nics[i] + " of vm " + vmName);
                        continue;
                    }
                    Set<String> macs = uuidToMacsMap.get(uuid);
                    if (macs==null) {
                        macs = new TreeSet<String>();
                        uuidToMacsMap.put(uuid,macs);
                    }
                    macs.add(mac.toUpperCase());
                }
            }
            return uuidToMacsMap;
        } finally {
            if (si!=null) {
                ServerConnection sc = si.getServerConnection();
                if (sc!=null) {
                    sc.logout();
                }
            }
        }
    }

    /**
     * persist mac-uuid mapping to DB
     * 
     * @param subject
     * @param uuidToMacsMap
     */
    protected void updateUUIDToMacsMapping(AuthzSubject subject, Map<String, Set<String>> uuidToMacsMap) {
        List<MacToUUID> macToUUIDs = this.vcDao.findAll();
        this.vcDao.remove(macToUUIDs);
        List<MacToUUID> toSave = new ArrayList<MacToUUID>();
        for(String uuid : uuidToMacsMap.keySet()) {
            Set<String> macs = uuidToMacsMap.get(uuid);
            for (String mac : macs) {
                MacToUUID uuidToMacs = new MacToUUID(mac,uuid);
                toSave.add(uuidToMacs);
            }
        }
        this.vcDao.save(toSave);
    }
    
    @Transactional(readOnly = false)
    public void collect(AuthzSubject subject, String url, String usr, String pass) throws RemoteException, MalformedURLException, PermissionException, CPropKeyNotFoundException, AppdefEntityNotFoundException {
        Map<String, Set<String>> uuidToMacsMap = this.collectUUIDs(url,usr,pass);
        updateUUIDToMacsMapping(subject,uuidToMacsMap);
        this.platformMgr.mapUUIDToPlatforms(subject, uuidToMacsMap);
    }

    public String getUuid(final List<String> macs) {
        for(String mac:macs) {
            try {
                //TODO~ change to findByID if more efficient, and turn mac the id of this class
                String uuid = this.vcDao.findByMac(mac);
                if (uuid!=null) {
                    return uuid;
                }
            } catch (DupMacException e) {
                log.error(e);
            }
        }
        return null;
    }
}
