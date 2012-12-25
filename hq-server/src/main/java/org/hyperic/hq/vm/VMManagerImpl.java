package org.hyperic.hq.vm;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

@Service
//@Transactional
public class VMManagerImpl implements VMManager {
    private static final String HOST_SYSTEM = "HostSystem";
    private static final String DATACENTER = "Datacenter";
    private static final String CLOUD_CONFIGURATION = "cloud_configuration";
    
    public void collect(String url, String usr, String pass) throws RemoteException, MalformedURLException {
      ServiceInstance si = new ServiceInstance(new URL(url), usr, pass, true);
      List<VirtualMachine> vms = getAllVms(si,);
      saveDB(vms);
    }


//    public DataCenterList getDataCenters() throws Throwable {  
//        ServiceInstance si = getServiceInstance();
//        Folder rootFolder = si.getRootFolder();
//        //get all the data centers from provider
//        ManagedEntity[] mes = new InventoryNavigator(rootFolder).searchManagedEntities(DATACENTER);
//        DataCenterList dcList = new DataCenterList();
//        if(mes==null || mes.length ==0){
//            si.getServerConnection().logout();
//            return dcList;
//        }
//
//        for (ManagedEntity mEntity : mes) {
//            Datacenter dc = (Datacenter) mEntity; 
//            dcList.addDataCenter(new DataCenter(dc.getName()));
//        }
//        si.getServerConnection().logout();
//        return dcList;
//    }
//
//
//    public HostsList getHosts(String dcName) throws Throwable {
//        HostsList hostsList = new HostsList();
//        ServiceInstance si = getServiceInstance();
//        Folder rootFolder = si.getRootFolder();
//        //find the data center based on the provided name
//        ManagedEntity me = new InventoryNavigator(rootFolder).searchManagedEntity(DATACENTER, dcName);
//        if(me==null){
//            si.getServerConnection().logout();
//            WebApplicationException webApplicationException = 
//                    errorHandler.newWebApplicationException(Response.Status.NOT_FOUND, ExceptionToErrorCodeMapper.ErrorCode.CLOUD_RESOURCE_NOT_FOUND, dcName);     
//            throw webApplicationException;
//        }
//        Datacenter dataCenter = (Datacenter)me;
//        //iterate over the data center's hosts
//        for (ManagedEntity mEntity : dataCenter.getHostFolder().getChildEntity()) {
//            ComputeResource computer = (ComputeResource) mEntity;
//            for (HostSystem hostSystem : computer.getHosts()) {
//                hostsList.addHost(new Host(hostSystem.getName()));
//            }
//        }
//        si.getServerConnection().logout();
//        return hostsList;
//    }

    public List<VirtualMachine> getAllVms(ServiceInstance si, String dcName, String hostName) throws Throwable {
        List<VirtualMachine> vmsList = new ArrayList<VirtualMachine>();
        Folder rootFolder = si.getRootFolder();
        //find the host based on the provided hostname
        ManagedEntity me = new InventoryNavigator(rootFolder).searchManagedEntity(HOST_SYSTEM, hostName);
        if(me==null){
//            si.getServerConnection().logout();
//            WebApplicationException webApplicationException = 
//                    errorHandler.newWebApplicationException(, hostName);     
//            throw webApplicationException;
        }
        HostSystem host = (HostSystem)me;
        //iterate over the host's virtual machines
        for (VirtualMachine vm : host.getVms()) {
//            org.hyperic.hq.api.model.cloud.VirtualMachine virtualMachine = new org.hyperic.hq.api.model.cloud.VirtualMachine(vm.getName());
//            virtualMachine.setIp(vm.getGuest().getIpAddress());
            vmsList.add(vm);
        }
        si.getServerConnection().logout();
        return vmsList;
    }

//    public void configureCloudProvider(CloudConfiguration cloudConfiguration) throws Throwable {
//        //check that the configuration is correct
//        try {
//            new ServiceInstance(new URL(cloudConfiguration.getUrl()), cloudConfiguration.getUsername(), 
//                    cloudConfiguration.getPassword(), true);
//        }catch(Throwable t) {
//            WebApplicationException webApplicationException = 
//                    errorHandler.newWebApplicationException(Response.Status.NOT_ACCEPTABLE, ExceptionToErrorCodeMapper.ErrorCode.BAD_CLOUD_PROVIDER_CONFIGURATION);     
//            throw webApplicationException;
//        }
//        //configuration is fine, save it in the session
//        getSession().setAttribute(CLOUD_CONFIGURATION, cloudConfiguration);
//    }
//
//    private CloudConfiguration getCloudConfiguration() {
//        Object configuration = getSession().getAttribute(CLOUD_CONFIGURATION);
//        if (null == configuration) {
//            WebApplicationException webApplicationException = 
//                    errorHandler.newWebApplicationException(Response.Status.PRECONDITION_FAILED, ExceptionToErrorCodeMapper.ErrorCode.CLOUD_PROVIDER_NOT_CONFIGURED);            
//            throw webApplicationException;
//        }
//        return (CloudConfiguration) configuration;
//    }

}
