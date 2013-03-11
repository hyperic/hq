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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.bizapp.shared.ConfigBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.util.ConfigPropertyException;
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

    private static final String VC_SYNCHRONIZER = "VCSynchronizer";
    protected final Log log = LogFactory.getLog(VCManagerImpl.class.getName());
    protected final VCDAO vcDao;
    private final ServerConfigManager serverConfigManager;
    private final AuthzSubjectManager authzSubjectManager;
    private final Set<VCConnection> vcConnections = new HashSet<VCConnection>();
    private final ConfigBoss configBoss;
    private ScheduledThreadPoolExecutor executor ; 
    private ApplicationContext appContext;
    private final int SYNC_INTERVAL_MINUTES;
    private PlatformManager platformManager;


    @Autowired
    public VCManagerImpl(VCDAO vcDao, ServerConfigManager serverConfigManager,
            AuthzSubjectManager authzSubjectManager, PlatformManager platformManager,
            ConfigBoss configBoss, @Value("#{VCProperties['vc.sync.interval.minutes']}") int syncInterval){
        this.vcDao = vcDao;
        this.serverConfigManager = serverConfigManager;
        this.platformManager = platformManager;
        this.configBoss = configBoss;
        this.SYNC_INTERVAL_MINUTES = syncInterval;
        this.authzSubjectManager = authzSubjectManager;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.appContext = applicationContext;
    }

    @PostConstruct
    void initialize() { 
        //get the vCenter credentials from the database
        Set<VCConnection> vcCredentials = getVCCredentials();
        //create a thread pool with a core size of the number of vCenters we keep mapped or one in case
        //we don't have any vCenters yet
        this.executor = new ScheduledThreadPoolExecutor((vcCredentials.isEmpty() ? 1 : vcCredentials.size()), new ThreadFactory() {
            private final AtomicLong i = new AtomicLong(0);
            public Thread newThread(Runnable r) {
                return new Thread(r, VC_SYNCHRONIZER + i.getAndIncrement());
            }
        });
        //create a scheduled 'sync task' for each vCenter 
        for (VCConnection cred : vcCredentials) {
            vcConnections.add(cred);
            executor.scheduleWithFixedDelay(new VCSynchronizer(cred), 0, SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES);
        }
    }

    /**
     * @param credentials - the credentials of the vCenter 
     */
    @Transactional(readOnly = false)
    private void doVCEventsScan(VCConnection credentials) {
        Event[] events = null;
        ServiceInstance si = null;
        EventFilterSpec filterSpec;
        Set<ManagedEntity> vms= new HashSet<ManagedEntity>();
        Folder rootFolder;
        InventoryNavigator navigator = null;
        List<VmMapping> vmsMapping = null;
        boolean doFullSync = false;

        try {
            si = new ServiceInstance(new URL(credentials.getUrl()), credentials.getUser(), credentials.getPassword(), true);
            rootFolder = si.getRootFolder();
            navigator = new InventoryNavigator(rootFolder);
            //if the last sync was not successful - run a full sync
            if (!credentials.lastSyncSucceeded()) {
                log.info("Collection full inventory for '" + credentials.getUrl() + "'");
                if (null == doVCFullScan(si)){
                    credentials.setLastSyncSucceeded(false);
                }
                else {
                    credentials.setLastSyncSucceeded(true);
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
                    ManagedEntity vm = null;
                    try {
                        vm = navigator.searchManagedEntity("VirtualMachine", event.vm.name);
                    }catch(Throwable t) {
                        log.error("Could not find virtual machine '" + event.vm.name + "' in the VC inventory");
                    }
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

        }catch(Throwable e) {
            log.warn(e, e);     
            //mark this sync as failure - a full sync will be attempted next sync
            credentials.setLastSyncSucceeded(false);
            return;
        }finally{
            if (si!=null) {
                ServerConnection sc = si.getServerConnection();
                if (sc!=null) {
                    sc.logout();
                }
            }
        }
        //mark this sync as successful
        credentials.setLastSyncSucceeded(true);
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


    public Set<VCConnection> getActiveVCConnections(){
        return vcConnections;
    }

    /**
     * @return a Set of VC credentials from the database of all the vCenters 'registered' for VM => macs mapping 
     */
    private Set<VCConnection> getVCCredentials() {
        Properties conf;
        Set<VCConnection> credentials = new HashSet<VCConnection>();
        try {
            conf = serverConfigManager.getConfig();
        } catch (ConfigPropertyException e) {
            throw new SystemException(e);
        }
        for (int i=0; ; i++) {
            String vCenterURL = conf.getProperty(HQConstants.vCenterURL + "_" + i);
            String vCenterUser = conf.getProperty(HQConstants.vCenterUser+ "_" + i);
            String vCenterPassword = conf.getProperty(HQConstants.vCenterPassword+ "_" + i);
            if (null == vCenterURL || null == vCenterUser || null == vCenterPassword) {
                break;
            }
            VCConnection cred = new VCConnection(vCenterURL, vCenterUser, vCenterPassword);
            credentials.add(cred);
        }
        return credentials;
    }

    /**
     * @param me - the managed entity (the virtual machine) 
     * @param vcUUID - the vCenter UUID
     * @return - an instance of VmMapping that represents the given virtual machine
     * or null in case we cannot extract all the mandatory information from the managed entity
     */
    private VmMapping getVMFromManagedEntity(ManagedEntity me, String vcUUID) {
        GuestNicInfo[] nics = null;
        try{
            VirtualMachine vm = (VirtualMachine)me; 
            String vmName = vm.getName();
            GuestInfo guest = vm.getGuest();

            if (guest==null)  {
                log.debug("no guest for vm " + vmName);
                return null;
            }

            ManagedObjectReference moref = vm.getMOR();
            if (moref==null) {
                log.debug("no moref is defined for vm " + vmName);
                return null;
            }

            nics = guest.getNet();
            if (nics == null || nics.length==0) {
                log.debug("no nics defined on vm " + vmName);
            }

            VmMapping vmMapping = new VmMapping(moref.getVal(),vcUUID);
            vmMapping.setName(vm.getName());
            vmMapping.setGuestNicInfo(nics);
            return vmMapping;
        }catch(Throwable t) {
        }
        return null;
    }

    /**
     * @param vms - a collection of virtual machines
     * @param vcUUID - the UUID of the vCenter
     * @return a list of VmMapping objects
     */
    private List<VmMapping> mapVMToMacAddresses(Collection<ManagedEntity> vms, String vcUUID) {
        List<VmMapping> mapping = new ArrayList<VmMapping>();
        // this is done in order to prevent gathering of VMs whichc share identical mac addresses
        Map<String,VmMapping> overallMacsSet = new HashMap<String,VmMapping>();
        for (ManagedEntity me : vms) {

            Set<String> macs = new HashSet<String>();
            try { 

                VmMapping vmMapping = getVMFromManagedEntity(me, vcUUID);
                if (null == vmMapping.getGuestNicInfo()) {
                    continue;
                }

                for (int i=0; i<vmMapping.getGuestNicInfo().length ; i++) {
                    if (vmMapping.getGuestNicInfo()[i]==null)  {
                        log.debug("nic no." + i + " is null on " + vmMapping);
                        continue;
                    }
                    String mac = vmMapping.getGuestNicInfo()[i].getMacAddress();
                    if (mac==null || "00:00:00:00:00:00".equals(mac)) {
                        log.debug("no mac address / mac address is 00:00:00:00:00:00 on nic" + vmMapping.getGuestNicInfo()[i] + " of vm " + vmMapping);
                        continue;
                    }
                    mac = mac.toUpperCase();
                    macs.add(mac);
                    VmMapping dupMacVM = overallMacsSet.get(mac);
                    if (dupMacVM!=null) {
                        // remove the other VM with the duplicate mac from the response object, as this is illegal
                        mapping.remove(dupMacVM);
                        continue;
                    }else {
                        overallMacsSet.put(mac,vmMapping);
                    }
                }
                String macsString = "";
                for(String mac : macs) {
                    macsString += mac;
                    macsString += ";";
                }
                vmMapping.setMacs(macsString);
                mapping.add(vmMapping);

            } catch (Throwable e) {
                log.error(e);
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
            }catch(Throwable t) {
                log.error(t,t);
            }
        }
        if(null != toSave) {
            this.vcDao.save(toSave);
            try {
                platformManager.mapUUIDToPlatforms(authzSubjectManager.getOverlordPojo(), toSave);
            }catch(Throwable t) {
                log.error(t,t);
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
        if(me==null || me.length==0){
            if (log.isDebugEnabled()) {
                log.debug("no virtual machines were discovered on " + vcUUID);
            }
            return null;
        }
        activeVms = mapVMToMacAddresses(Arrays.asList(me), vcUUID);
        List<VmMapping> deletedVms = new ArrayList<VmMapping>();
        for (VmMapping mapping : vcDao.findVcUUID(si.getServiceContent().getAbout().getInstanceUuid())) {
            if (!activeVms.contains(mapping)) {
                deletedVms.add(mapping);
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
     * @see org.hyperic.hq.vm.VCManager#validateVCSettings(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean validateVCSettings(String url, String user, String password) throws RemoteException, MalformedURLException {
        new ServiceInstance(new URL(url), user, password, true);   
        return true;
    }

    public boolean connectionExists(String url, String user, String password) {
        VCConnection connection = new VCConnection(url, user, password);
        if (vcConnections.contains(connection)) {
            return true;
        }
        return false;
    }

    public boolean connectionExists(int index) {
        for(VCConnection connection : vcConnections) {
            if(connection.getIndex() == index) {
                return true;
            }
        }
        return false;
    }

    public void updateConnectionByIndex(String url, String user, String password, int index) throws ApplicationException {
        if (!connectionExists(index)) {
            throw new ApplicationException("There is no VC connection with index '" + index + "'");
        }
        VCConnection connection = null;
        for(VCConnection con : vcConnections) {
            if(con.getIndex() == index) {
                connection = con;
                break;
            }
        }
        connection.setPassword(password);
        connection.setUrl(url);
        connection.setUser(user);
        try{
            Properties props = configBoss.getConfig();
            props.setProperty(HQConstants.vCenterURL + "_" + index, url); 
            props.setProperty(HQConstants.vCenterUser + "_" + index, user);
            props.setProperty(HQConstants.vCenterPassword + "_" + index, password); 
            configBoss.setConfig(authzSubjectManager.getOverlordPojo(), props);
        }
        catch (Exception e) {
            log.error(e,e);
        }
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.vm.VCManager#registerVC(java.lang.String, java.lang.String, java.lang.String)
     */
    public void registerOrUpdateVC(String url, String user, String password) throws RemoteException, MalformedURLException, 
    ConfigPropertyException, ApplicationException {
        int i;     
        VCConnection connection = new VCConnection(url, user, password);
        if (vcConnections.contains(connection)) {
            i = getIndexOfVCInConfTable(url);
            for(VCConnection con : vcConnections) {
                if(con.equals(connection)) {
                    con.setPassword(password);
                    con.setUser(user);
                    break;
                }
            }       
        }
        else {
            i = getLastIndexOfVCInConfTable() + 1;
            connection.setIndex(i);
            vcConnections.add(connection);
            // create a scheduled 'sync task' for the vCenter 
            executor.scheduleWithFixedDelay(new VCSynchronizer(connection), 0, SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES);
        }
        Properties props = configBoss.getConfig();
        props.setProperty(HQConstants.vCenterURL + "_" + i, url); 
        props.setProperty(HQConstants.vCenterUser + "_" + i, user);
        props.setProperty(HQConstants.vCenterPassword + "_" + i, password); 
        configBoss.setConfig(authzSubjectManager.getOverlordPojo(), props);
    }

    private int getLastIndexOfVCInConfTable() {
        int index = -1;
        try{
            for(Object key : configBoss.getConfig().keySet()) {
                if (key.toString().startsWith(HQConstants.vCenterURL)) {
                    int value = Integer.valueOf(key.toString().substring(key.toString().lastIndexOf("_") + 1));
                    if (value > index) {
                        index = value;
                    }
                }
            }
        }catch(Throwable t){
            log.warn(t,t);
        }
        return index;
    }

    private int getIndexOfVCInConfTable(String url) {
        try{
            for(Object key : configBoss.getConfig().keySet()) {
                if (key.toString().startsWith(HQConstants.vCenterURL)) {
                    String propsUrl = (String) configBoss.getConfig().get(key);
                    if(propsUrl.equalsIgnoreCase(url)) {
                        return Integer.valueOf(key.toString().substring(key.toString().lastIndexOf("_") + 1));
                    }
                }
            }
        }catch(Throwable t){
            log.warn(t,t);
        }
        return -1;
    }

    /**
     * A worker class responsible for keeping the vCenter mapping up to date, 
     * each worker is responsible for one vCenter.
     */
    private class VCSynchronizer implements Runnable{

        private final VCConnection credantials;

        public VCSynchronizer(VCConnection credantials) {    
            this.credantials = credantials;
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
                doVCEventsScan(credantials);
            }catch(Throwable t){
                log.error(t,t);
            }finally{
                TransactionSynchronizationManager.unbindResource(sessionFactory);
                //Close the given Session
                SessionFactoryUtils.closeSession(session);
            }
        }
    }


}
