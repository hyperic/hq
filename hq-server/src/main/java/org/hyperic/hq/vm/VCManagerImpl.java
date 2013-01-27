package org.hyperic.hq.vm;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.CPropKeyNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.vim25.GuestInfo;
import com.vmware.vim25.GuestNicInfo;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServerConnection;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

@Service
public class VCManagerImpl implements VCManager {
    protected final Log log = LogFactory.getLog(VCManagerImpl.class.getName());
    @Autowired
    protected VCDAO vcDao;

    public Map<VMID,Set<String>> collectUUIDs(ManagedEntity[] mes, String vcUUID) throws RemoteException, MalformedURLException {
        Map<VMID,Set<String>> vmidToMacsMap = new HashMap<VMID,Set<String>>();
        // this is done in order to prevent gathering of VMs whichc share identical mac addresses
        Map<String,VMID> overallMacsSet = new HashMap<String,VMID>();
        for (ManagedEntity me : mes) {
            // gather data from the vm
            VirtualMachine vm = (VirtualMachine)me;
            GuestNicInfo[] nics = null;
            try { 
                String vmName = vm.getName();
                GuestInfo guest = vm.getGuest();
                if (guest==null)  {
                    log.debug("no guest for vm " + vmName);
                    continue;
                }
                nics = guest.getNet();
                if (nics == null || nics.length==0) {
                    log.debug("no nics defined on vm " + vmName);
                    continue;
                }
                ManagedObjectReference moref = vm.getMOR();
                if (moref==null) {
                    log.debug("no moref is defined for vm " + vmName);
                    continue;
                }
                VMID vmid = new VMID(moref.getVal(),vcUUID);

                // gather macs
                boolean foundDupMacOnCurrVM = false;
                for (int i=0; i<nics.length ; i++) {
                    if (nics[i]==null)  {
                        log.debug("nic no." + i + " is null on " + vmName);
                        continue;
                    }
                    String mac = nics[i].getMacAddress();
                    if (mac==null || "00:00:00:00:00:00".equals(mac)) {
                        log.debug("no mac address / mac address is 00:00:00:00:00:00 on nic" + nics[i] + " of vm " + vmName);
                        continue;
                    }
                    mac = mac.toUpperCase();
                    VMID dupMacVM = overallMacsSet.get(mac);
                    if (dupMacVM!=null) {
                        // mark this VM and the one the 1st duplicate mac is at as illegal as there is another VM with identical mac addresses, and continue collecting the rest of this discovered VMs mac addresses, in order to find duplications on other VMs
                        foundDupMacOnCurrVM = true;
                        // remove the other VM with the duplicate mac from the response object, as this is illegal
                        vmidToMacsMap.remove(dupMacVM);
                        continue;
                    }
                    overallMacsSet.put(mac,vmid);
                    if (!foundDupMacOnCurrVM) {
                        Set<String> macs = vmidToMacsMap.get(vmid);
                        if (macs==null) {
                            macs = new TreeSet<String>();
                            vmidToMacsMap.put(vmid,macs);
                        }
                        macs.add(mac);
                    }
                }
            } catch (Throwable e) {
                log.error(e);
            }
        }
        return vmidToMacsMap;
    }
    
    /**
     * 
     * @param url
     * @param usr
     * @param pass
     * @return a map of VM IDs mapped to the set of mac addresses of this VM. duplicate macs over VMs is not allowed, so VMs which share identical mac addresses wouldn't be returned.
     * @throws RemoteException
     * @throws MalformedURLException
     */
    protected Map<VMID,Set<String>> collectUUIDs(final String url, final String usr, final String pass) throws RemoteException, MalformedURLException {
        ServiceInstance si = new ServiceInstance(new URL(url), usr, pass, true);
        try {
            String vcUUID = si.getServiceContent().getAbout().getInstanceUuid();
            Folder rootFolder = si.getRootFolder();
            ManagedEntity[] me = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
            if(me==null || me.length==0){
                if (log.isDebugEnabled()) {
                    log.debug("no virtual machines were discovered on " + url);
                }
                return null;
            }
            return collectUUIDs(me, vcUUID);
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
    protected void updateUUIDToMacsMapping(AuthzSubject subject, Map<VMID, Set<String>> uuidToMacsMap) {
        List<MacToUUID> macToUUIDs = this.vcDao.findAll();
        this.vcDao.remove(macToUUIDs);
        List<MacToUUID> toSave = new ArrayList<MacToUUID>();
        for(VMID vmid : uuidToMacsMap.keySet()) {
            Set<String> macs = uuidToMacsMap.get(vmid);
            for (String mac : macs) {
                MacToUUID uuidToMacs = new MacToUUID(mac,vmid.getMoref(),vmid.getVcUUID());
                toSave.add(uuidToMacs);
            }
        }
        this.vcDao.save(toSave);
    }

    @Transactional(readOnly = false)
    public Map<VMID, Set<String>> collect(AuthzSubject subject, String url, String usr, String pass) throws RemoteException, MalformedURLException, PermissionException, CPropKeyNotFoundException, AppdefEntityNotFoundException {
        Map<VMID, Set<String>> uuidToMacsMap = this.collectUUIDs(url,usr,pass);
        updateUUIDToMacsMapping(subject,uuidToMacsMap);
        return uuidToMacsMap;
    }

    public VMID getVMID(final List<String> macs) {
        for(String mac:macs) {
            try {
                //TODO~ change to findByID if more efficient, and turn mac the id of this class
                VMID vmid = this.vcDao.findByMac(mac);
                if (vmid!=null) {
                    return vmid;
                }
            } catch (DupMacException e) {
                log.error(e);
            }
        }
        return null;
    }
}
