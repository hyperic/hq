package org.hyperic.hq.vm;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hyperic.hq.appdef.shared.CPropKeyNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.hq.hibernate.SessionManager;
import org.hyperic.hq.hibernate.SessionManager.SessionRunner;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.StringUtil;
import org.jasypt.hibernate.encryptor.HibernatePBEStringEncryptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.orm.hibernate3.support.OpenSessionInViewFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.vmware.vim25.Event;
import com.vmware.vim25.EventFilterSpec;
import com.vmware.vim25.EventFilterSpecByTime;
import com.vmware.vim25.GuestInfo;
import com.vmware.vim25.GuestNicInfo;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.VmEvent;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServerConnection;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

@SuppressWarnings("restriction")
@Service
public class VCManagerImpl implements VCManager, ApplicationContextAware {

    private static final String POWER_ON_VM_TASK = "PowerOnVM_Task";
    private static final String RESET_VM_TASK = "ResetVM_Task";
    private static final String SUSPEND_VM_TASK = "SuspendVM_Task";
    private static final String POWER_OFF_VM_TASK = "PowerOffVM_Task";
    private static final String[] DISABLED_METHOD_FOR_INACTIVE_VM = {POWER_ON_VM_TASK, RESET_VM_TASK, SUSPEND_VM_TASK, POWER_OFF_VM_TASK};
    
    private static final String VC_SYNCHRONIZER = "VCSynchronizer";
    protected final Log log = LogFactory.getLog(VCManagerImpl.class.getName());
    protected final VCDAO vcDao;
    protected final VCConfigDAO vcConfigDao;
    private final AuthzSubjectManager authzSubjectManager;
    private final Set<VCConfig> vcConfigs = new HashSet<VCConfig>();
    private ScheduledThreadPoolExecutor executor ; 
    private ApplicationContext appContext;
    private final int SYNC_INTERVAL_MINUTES;
    private final PlatformManager platformManager;
    private final ServerConfigManager serverConfigManager;



    @Autowired
    public VCManagerImpl(VCDAO vcDao, HibernatePBEStringEncryptor encryptor,
            AuthzSubjectManager authzSubjectManager, PlatformManager platformManager,
            VCConfigDAO vcConnectionDao, ServerConfigManager serverConfigManager,
            @Value("#{VCProperties['vc.sync.interval.minutes']}") int syncInterval){
        this.vcDao = vcDao;
        this.platformManager = platformManager;
        this.SYNC_INTERVAL_MINUTES = syncInterval;
        this.authzSubjectManager = authzSubjectManager;
        this.vcConfigDao = vcConnectionDao;
        this.serverConfigManager = serverConfigManager;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.appContext = applicationContext;
    }

    @PostConstruct
    void initialize() { 
        //get the vCenter credentials from the database
        try {
            SessionManager.runInSession(new SessionRunner() {

                public void run() throws Exception {
                    convertOldVCConfigsFromDB();
                    loadVCConfigsFromDB();
                }


                public String getName() {
                    return "VCManagerImpl";
                }

            });
        }catch(Exception e) {
            log.error(e,e);
        }

    }

    @Transactional(readOnly = true)
    private void loadVCConfigsFromDB() {
        List<VCConfig> vcDBConfigs = getVCConfigsFromDB();
        if (null != executor) {
            executor.shutdown();
        }
        executor = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            private final AtomicLong i = new AtomicLong(0);
            public Thread newThread(Runnable r) {
                final Thread rtn = new Thread(r, VC_SYNCHRONIZER + i.getAndIncrement());
                rtn.setDaemon(true);
                return rtn;
            }
        });
        vcConfigs.clear();
        //create a scheduled 'sync task' for each vCenter 
        for (VCConfig conf : vcDBConfigs) {
            vcConfigs.add(conf);
            executor.scheduleWithFixedDelay(new VCSynchronizer(conf), 0, SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES);
        }
    }


    /**
     * @param conf - the credentials of the vCenter 
     */
    @Transactional(readOnly = false)
    private void doVCEventsScan(VCConfig conf) {
        Event[] events = null;
        ServiceInstance si = null;
        EventFilterSpec filterSpec;
        Set<ManagedEntity> vms= new HashSet<ManagedEntity>();
        Folder rootFolder;
        InventoryNavigator navigator = null;
        List<VmMapping> vmsMapping = null;
        boolean doFullSync = false;

        try {
            si = new ServiceInstance(new URL(conf.getUrl()), conf.getUser(), conf.getPassword(), true);
            rootFolder = si.getRootFolder();
            navigator = new InventoryNavigator(rootFolder);
            //if the last sync was not successful - run a full sync
            if (!conf.lastSyncSucceeded()) {
                log.info("Collection full inventory for '" + conf.getUrl() + "'");
                if (null == doVCFullScan(si)){
                    conf.setLastSyncSucceeded(false);
                }
                else {
                    conf.setLastSyncSucceeded(true);
                }
                return;
            } 

            filterSpec = createVCEventsFilter();
            //query last events
            events = si.getEventManager().queryEvents(filterSpec);
            if (null == events) {
                return;
            }
            Set<String> addedEntities = new HashSet<String>();
            for (Event event : events) {
                //we only care about virtual machine's events
                if (event instanceof VmEvent){
                    ManagedEntity vm = navigator.searchManagedEntity("VirtualMachine", event.vm.name);
                    //if the vm is null we need to run a full scan because it probably means that
                    //this vm was deleted
                    if (null == vm) {
                        doFullSync = true;
                        break;
                    }
                    if(!addedEntities.contains(vm.getName())) {
                        vms.add(vm);
                        addedEntities.add(vm.getName());
                    }
                }
            }
            String vcUUID = si.getServiceContent().getAbout().getInstanceUuid();
            if (doFullSync) {
                doVCFullScan(si);
                return;
            }
            if (!vms.isEmpty()) {  
                //do the mapping only for virtual machines that we got events on
                vmsMapping = mapVMToMacAddresses(vms, vcUUID);              
            }
        } catch (InvalidProperty e) {
            log.error(e, e);     
            conf.setLastSyncSucceeded(false);
            return;
        } catch (RuntimeFault e) {
            log.error(e, e);     
            conf.setLastSyncSucceeded(false);
            return;
        } catch (RemoteException e) {
            log.error(e, e);     
            conf.setLastSyncSucceeded(false);
            return;
        } catch (MalformedURLException e) {
            log.error(e, e);     
            conf.setLastSyncSucceeded(false);
            return;
        } finally{
            if (si!=null) {
                ServerConnection sc = si.getServerConnection();
                if (sc!=null) {
                    sc.logout();
                }
            }
        }
        //mark this sync as successful
        conf.setLastSyncSucceeded(true);
        //persist the mapping
        persistMapping(vmsMapping);
    }

    /**
     * @return a vCenter 'time based' events filter - we want to get all the events that happened in the last
     * 2 * sync_interval minutes just to make sure we don't miss anything, for example - if we pull the vCenter
     * events every 2 minutes, each time we will ask for all the events that happened in the last 4 minutes
     */
    private EventFilterSpec createVCEventsFilter() {
        EventFilterSpec filterSpec;
        EventFilterSpecByTime timeSpec;
        filterSpec = new EventFilterSpec();
        timeSpec = new EventFilterSpecByTime();
        Calendar begin = (Calendar) Calendar.getInstance().clone();
        begin.setTimeInMillis(System.currentTimeMillis() - (SYNC_INTERVAL_MINUTES * 2 * 60 * 1000));
        timeSpec.setBeginTime(begin);
        timeSpec.setEndTime(Calendar.getInstance());
        filterSpec.setTime(timeSpec);
        return filterSpec;
    }


    public boolean vcConfigExistsByUrl(String url) {
        for (VCConfig connection : vcConfigs) {
            if (connection.getUrl().equalsIgnoreCase(url)) {
                return true;
            }
        }
        return false;
    }


    /* (non-Javadoc)
     * @see org.hyperic.hq.vm.VCManager#getActiveVCConfings()
     */
    public Set<VCConfig> getActiveVCConfings(){
        return vcConfigs;
    }
    
    /* (non-Javadoc)
     * @see org.hyperic.hq.vm.VCManager#getActiveVCConfingsAsString()
     */
    public String getActiveVCConfingsAsString() {
        StringBuffer sb = new StringBuffer();
        sb.append("vCenter configurations - ").append("\n");
        for (VCConfig config:vcConfigs) {
            sb.append(config.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * @return a Set of VC credentials from the database of all the vCenters 'registered' for VM => macs mapping 
     */
    private List<VCConfig> getVCConfigsFromDB() {    
        return vcConfigDao.findAll();
    }
    
   @Transactional(readOnly = false)
   private void convertOldVCConfigsFromDB() {
       //after upgrade from versions < 5.8
       //we need to convert the vCenter config entries
       //to the new format
       Properties conf;
       Set<String> keysToDelete = new HashSet<String>();
       try {
           conf = serverConfigManager.getConfig();
       } catch (ConfigPropertyException ex) {
           throw new SystemException(ex);
       }

       for (int i=0; ; i++) {
           String vCenterURL = conf.getProperty(HQConstants.vCenterURL + "_" + i);
           String vCenterUser = conf.getProperty(HQConstants.vCenterUser+ "_" + i);
           String vCenterPassword = conf.getProperty(HQConstants.vCenterPassword+ "_" + i);
           if ((null == vCenterURL) || (null == vCenterUser) || (null == vCenterPassword)) {
               break;
           }
           keysToDelete.add(HQConstants.vCenterURL + "_" + i);
           keysToDelete.add(HQConstants.vCenterUser + "_" + i);
           keysToDelete.add(HQConstants.vCenterPassword + "_" + i);
           VCConfig vcConfig = new VCConfig(vCenterURL, vCenterUser, vCenterPassword);
           if (i == 0) {
               vcConfig.setSetByUI(true);
           }
           setVcUuid(vcConfig);
           vcConfigDao.save(vcConfig);
           vcConfigDao.getSession().flush();
           vcConfigDao.getSession().clear();
       }
          
       if (!keysToDelete.isEmpty()) {
           try {
               serverConfigManager.deleteConfig(authzSubjectManager.getOverlordPojo(), keysToDelete);
           }catch(Exception ex) {
               throw new SystemException(ex);
           }
       }
   }

    /**
     * @param me - the managed entity (the virtual machine) 
     * @param vcUUID - the vCenter UUID
     * @return - an instance of VmMapping that represents the given virtual machine
     * or null in case we cannot extract all the mandatory information from the managed entity
     */
    private VmMapping getVMFromManagedEntity(ManagedEntity me, String vcUUID) {
        GuestNicInfo[] nics = null;
        VirtualMachine vm = (VirtualMachine)me; 
            
        //Check if this VM is an inactive or a fault tolerant second machine 
        if (Arrays.asList(vm.getDisabledMethod()).
                containsAll(Arrays.asList(DISABLED_METHOD_FOR_INACTIVE_VM))){
           if (log.isDebugEnabled()) {
               log.debug("Found an inactive or a fault tolerant second machine '" + vm.getName() + "'");
           }
           return null;
        }
        
        String vmName = vm.getName();
        GuestInfo guest = vm.getGuest();
        final boolean debug = log.isDebugEnabled();
        if (guest==null)  {
            if (debug) log.debug("no guest for vm " + vmName);
            return null;
        }
        ManagedObjectReference moref = vm.getMOR();
        if (moref==null) {
            if (debug) log.debug("no moref is defined for vm " + vmName);
            return null;
        }
        nics = guest.getNet();
        if (debug && ((nics == null) || (nics.length==0))) {
            log.debug("no nics defined on vm " + vmName);
        }
        VmMapping vmMapping = new VmMapping(moref.getVal(),vcUUID);
        vmMapping.setName(vm.getName());
        vmMapping.setGuestNicInfo(nics);
        return vmMapping;
    }

    /**
     * @param vms - a collection of virtual machines
     * @param vcUUID - the UUID of the vCenter
     * @return a list of VmMapping objects
     */
    private List<VmMapping> mapVMToMacAddresses(Collection<ManagedEntity> vms, String vcUUID) {
        final boolean debug = log.isDebugEnabled();
        List<VmMapping> mapping = new ArrayList<VmMapping>();
        // this is done in order to prevent gathering of VMs whichc share identical mac addresses
        Map<String,VmMapping> overallMacsSet = new HashMap<String,VmMapping>();
        for (ManagedEntity me : vms) {
            Set<String> macs = new HashSet<String>();
            VmMapping vmMapping = getVMFromManagedEntity(me, vcUUID);
            if (null == vmMapping || null == vmMapping.getGuestNicInfo()) {
                continue;
            }
            boolean foundDupMacOnCurrVM = false;
            for (int i=0; i<vmMapping.getGuestNicInfo().length ; i++) {
                if (vmMapping.getGuestNicInfo()[i]==null)  {
                    if (debug) { log.debug("nic no. " + i + " is null on " + vmMapping); }
                    continue;
                }
                String mac = vmMapping.getGuestNicInfo()[i].getMacAddress();
                if ((mac==null) || "00:00:00:00:00:00".equals(mac)) {
                    if (debug) {
                        log.debug("no mac address / mac address is 00:00:00:00:00:00 on nic" +
                                vmMapping.getGuestNicInfo()[i] + " of vm " + vmMapping);
                    }
                    continue;
                }
                mac = mac.toUpperCase();
                macs.add(mac);
                VmMapping dupMacVM = overallMacsSet.get(mac);
                if (dupMacVM!=null) {
                    // remove the other VM with the duplicate mac from the response object, as this is illegal
                    mapping.remove(dupMacVM);
                    foundDupMacOnCurrVM = true;
                    try {
                        //check if we already have saved virtual machines with this mac address
                        //and if there are such machines - delete them
                        VmMapping existingVM = vcDao.findVMByMac(mac);
                        if (null != existingVM) {
                            persistMapping(null, Arrays.asList(existingVM));
                        }
                    }catch(DupMacException e) {
                     
                    }
                    continue;
                }else {
                    overallMacsSet.put(mac,vmMapping);
                }
            }
            if (!foundDupMacOnCurrVM) {
                String macsString = "";
                for(String mac : macs) {
                    macsString += mac;
                    macsString += ";";
                }
                vmMapping.setMacs(macsString);
                mapping.add(vmMapping);
            }
        }
        return mapping;
    }


    /**
     * @param toSave - the list of VmMapping objects we want to save in the database
     */
    private void persistMapping(List<VmMapping> toSave) {
        persistMapping(toSave, null);
    }


    /**
     * @param toSave - the list of VmMapping objects we want to save in the database
     * @param toRemove - the list of VmMapping objects we want to remove from the database
     */
    private void persistMapping(List<VmMapping> toSave, List<VmMapping> toRemove) {
        if(null != toRemove) {
            this.vcDao.remove(toRemove);
            List<String> macAddresses = new ArrayList<String>();
            for (VmMapping mapping : toRemove) {
                macAddresses.addAll(Arrays.asList(mapping.getMacs().split(";")));
            }
            try {
                platformManager.removePlatformVmMapping(authzSubjectManager.getOverlordPojo(), macAddresses);
            } catch (PermissionException e) {
                log.error(e,e);
            }
        }
        if(null != toSave) {
            this.vcDao.save(toSave);
            try {
                platformManager.mapUUIDToPlatforms(authzSubjectManager.getOverlordPojo(), toSave);
            } catch (CPropKeyNotFoundException e) {
                log.error(e,e);
            } catch (PermissionException e) {
                log.error(e,e);
            }
        }
    }

    /**
     * @param si - the 'connection instance' to the vCenter
     * @return a list of VmMapping objects
     */
    @Transactional(readOnly = false)
    private List<VmMapping> doVCFullScan(ServiceInstance si) throws InvalidProperty, RuntimeFault, RemoteException {

        List<VmMapping> activeVms = null;
        String vcUUID = si.getServiceContent().getAbout().getInstanceUuid();
        Folder rootFolder = si.getRootFolder();
        ManagedEntity[] me = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
        if((me==null) || (me.length==0)){
            if (log.isDebugEnabled()) {
                log.debug("no virtual machines were discovered on " + vcUUID);
            }
            return null;
        }
        activeVms = mapVMToMacAddresses(Arrays.asList(me), vcUUID);
        List<VmMapping> deletedVms = new ArrayList<VmMapping>();
        for (VmMapping mapping : vcDao.findByVcUUID(si.getServiceContent().getAbout().getInstanceUuid())) {
            if (!activeVms.contains(mapping)) {
                deletedVms.add(mapping);
            }
        }
        
        //Now check if there are virtual machines with the same mac address in other vCenters
        for (VmMapping existingMapping : vcDao.getVMsFromOtherVcenters(si.getServiceContent().getAbout().getInstanceUuid())) {
           Iterator<VmMapping> iter = activeVms.iterator();
           while (iter.hasNext()) {
               VmMapping newMapping = iter.next();
               for (String mac : existingMapping.getMacs().split(";")) {
                   //Found duplicate mac address, remove both of the machines
                   if (newMapping.macs.contains(mac)) {
                       iter.remove();
                       deletedVms.add(existingMapping);
                   }
               }
           }
        }
        vcDao.getSession().clear();
        persistMapping(activeVms, deletedVms);

        return activeVms;
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.vm.VCManager#getVMID(java.util.List)
     */
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

    /* (non-Javadoc)
     * @see org.hyperic.hq.vm.VCManager#deleteVCConfig(int)
     */
    @Transactional(readOnly = false)
    public void deleteVCConfig(int id) {
        deleteVCMapping(vcConfigDao.get(id));
        vcConfigDao.remove(vcConfigDao.get(id));
        vcConfigDao.getSession().flush();
        vcConfigDao.getSession().clear();
        loadVCConfigsFromDB();
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.vm.VCManager#deleteVCConfig(java.lang.String)
     */
    @Transactional(readOnly = false)
    public void deleteVCConfig(String id) {
        deleteVCConfig(Integer.valueOf(id));
    }


    /* (non-Javadoc)
     * @see org.hyperic.hq.vm.VCManager#vcConfigExists(int)
     */
    public boolean vcConfigExists(int id) {
        if (null == vcConfigDao.get(id)) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.vm.VCManager#vcConfigExists(java.lang.String)
     */
    public boolean vcConfigExists(String id) {
        try {
            return vcConfigExists(Integer.valueOf(id));
        }catch(Exception e){
            return false;
        }

    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.vm.VCManager#updateVCConfig(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Transactional(readOnly = false)
    public void updateVCConfig(String id, String url, String user, String password) throws ApplicationException {
        if (null == id) {
            throw new ApplicationException("Please provide the ID of the vCenter you like to update");
        }

        VCConfig vcConfig = vcConfigDao.get(Integer.valueOf(id));
        if (null == vcConfig) {
            throw new ApplicationException("There is no VC connection with id '" + id + "'");
        }
        vcConfig.setUrl(url);
        vcConfig.setUser(user);
        vcConfig.setPassword(password);
        updateVCConfig(vcConfig);
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.vm.VCManager#updateVCConfig(org.hyperic.hq.vm.VCConfig)
     */
    @Transactional(readOnly = false)
    public void updateVCConfig(VCConfig vc) throws ApplicationException {  
        VCConfig vcConfig = null;
        for(VCConfig con : vcConfigs) {
            if(con.equals(vc)) {
                vcConfig = con;
                break;
            }
        }

        if (null == vcConfig) {
            throw new ApplicationException("There is no VC connection with id '" + vc.getId() + "'");
        }

        if (vcConfig.getUrl() != null && !vcConfig.getUrl().equalsIgnoreCase(vc.getUrl())) {
            deleteVCMapping(vcConfig);
        }

        vcConfig.setUrl(vc.getUrl());
        vcConfig.setPassword(vc.getPassword());
        vcConfig.setUser(vc.getUser());
        vcConfig.setLastSyncSucceeded(false);
        setVcUuid(vcConfig);
        vcConfigDao.getSession().clear();
        vcConfigDao.save(vcConfig);
    }
    

    public VCConfig getVCConfig(int id) {
        for (VCConfig conf : vcConfigs) {
            if (conf.getId() == id) {
                return conf;
            }
        }
        return null;
    }
    
    
    private void setVcUuid(VCConfig config){
        ServiceInstance si = null;
        String vcUuid = null;
        try {
            si = new ServiceInstance(new URL(config.getUrl()), config.getUser(), config.getPassword(),
                    true);
            vcUuid = si.getServiceContent().getAbout().getInstanceUuid();
        } catch (RemoteException e) {
            log.warn(e,e);
        } catch (MalformedURLException e) {
            log.warn(e,e);
        } finally {
            if (si != null) {
                ServerConnection sc = si.getServerConnection();
                if (sc != null) {
                    sc.logout();
                }
            }
        }
        if (null != vcUuid) {
            config.setVcUuid(vcUuid);
        }
    }

    /**
     * @param config - the vCenter config for which we want to
     * remove all related VM mapping entries in the DB
     */
    private void deleteVCMapping(VCConfig config) {
       
        if (null != config.getVcUuid()) {
            List<VmMapping> deletedVms = new ArrayList<VmMapping>();
            for (VmMapping mapping : vcDao.findByVcUUID(config.getVcUuid())) {
                deletedVms.add(mapping);
            }
            vcDao.getSession().clear();
            persistMapping(null, deletedVms);
        }
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.vm.VCManager#addVCConfig(java.lang.String, java.lang.String, java.lang.String, boolean)
     */
    @Transactional(readOnly = false)
    public VCConfig addVCConfig(String url, String user, String password, boolean setByUi) throws ApplicationException {
       
        if ((StringUtil.isNullOrEmpty(url) || StringUtil.isNullOrEmpty(user) || StringUtil.isNullOrEmpty(password))) {
            throw new ApplicationException(" missing one or more of these fields - vCenter URL, username and password");
        }
        if (vcConfigExistsByUrl(url)) {
            throw new ApplicationException("There is already an existing vCenter configuration for '" + 
        url + "'");
        }
        VCConfig vcConfig = new VCConfig(url, user, password);
        vcConfig.setSetByUI(setByUi);
        vcConfigs.add(vcConfig);
        // create a scheduled 'sync task' for the vCenter 
        executor.scheduleWithFixedDelay(new VCSynchronizer(vcConfig), 0, SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES);
        setVcUuid(vcConfig);
        vcConfigDao.save(vcConfig);
        return vcConfig;
    }
    

  
    /* (non-Javadoc)
     * @see org.hyperic.hq.vm.VCManager#addVCConfig(java.lang.String, java.lang.String, java.lang.String)
     */
    @Transactional(readOnly = false)
    public VCConfig addVCConfig(String url, String user, String password) throws ApplicationException {
       return addVCConfig(url, user, password, false);
    }



    /* (non-Javadoc)
     * @see org.hyperic.hq.vm.VCManager#getVCConfigSetByUI()
     */
    public VCConfig getVCConfigSetByUI() {
        return vcConfigDao.getVCConnectionSetByUI();
    }

    /**
     * A worker class responsible for keeping the vCenter mapping up to date, 
     * each worker is responsible for one vCenter.
     */
    private class VCSynchronizer implements Runnable{

        private final VCConfig config;
        private AtomicInteger scanCount = new AtomicInteger(0);
        
        public VCSynchronizer(VCConfig credantials) {    
            this.config = credantials;
        }

        public void run() {
            Session session = null;
            SessionFactory sessionFactory = null;
            try{
                //Binds a Hibernate Session to the thread for the entire
                //processing of the request. This logic is copied from the OpenSessionInViewFilter filter
                sessionFactory = 
                        appContext.getBean(OpenSessionInViewFilter.DEFAULT_SESSION_FACTORY_BEAN_NAME, SessionFactory.class);
                session = SessionFactoryUtils.getSession(sessionFactory, true);
                session.setFlushMode(FlushMode.MANUAL);
                if (!TransactionSynchronizationManager.hasResource(sessionFactory)) {
                    TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
                }
                
                //Every 10 scans - do a full VC scan
                if (scanCount.incrementAndGet() >= 9) {
                    scanCount.set(0);
                    config.setLastSyncSucceeded(false);
                }
                
                doVCEventsScan(config);
            } catch (Error e) {
                log.fatal(e,e);
                throw e;
            } catch (Throwable t) {
                log.error(t,t);
            }finally{
                TransactionSynchronizationManager.unbindResource(sessionFactory);
                //Close the given Session
                SessionFactoryUtils.closeSession(session);
            }
        }
    }




}
