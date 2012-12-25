package org.hyperic.hq.api.services.impl;

import java.net.URL;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.hyperic.hq.api.model.cloud.CloudConfiguration;
import org.hyperic.hq.api.model.cloud.DataCenter;
import org.hyperic.hq.api.model.cloud.DataCenterList;
import org.hyperic.hq.api.model.cloud.Host;
import org.hyperic.hq.api.model.cloud.HostsList;
import org.hyperic.hq.api.model.cloud.VirtualMachinesList;
import org.hyperic.hq.api.services.CloudProviderService;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;

import com.vmware.vim25.mo.ComputeResource;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

/**
 * Cloud provider REST API service implementation
 */
public class CloudProviderServiceImpl extends RestApiService  implements CloudProviderService{

    private static final String HOST_SYSTEM = "HostSystem";
    private static final String DATACENTER = "Datacenter";
    private static final String CLOUD_CONFIGURATION = "cloud_configuration";


    /* (non-Javadoc)
     * @see org.hyperic.hq.api.services.CloudProviderService#getDataCenters()
     */
    @GET
    @Path("/dc")
    public DataCenterList getDataCenters() throws Throwable {  
        ServiceInstance si = getServiceInstance();
        Folder rootFolder = si.getRootFolder();
        //get all the data centers from provider
        ManagedEntity[] mes = new InventoryNavigator(rootFolder).searchManagedEntities(DATACENTER);
        DataCenterList dcList = new DataCenterList();
        if(mes==null || mes.length ==0){
            si.getServerConnection().logout();
            return dcList;
        }

        for (ManagedEntity mEntity : mes) {
            Datacenter dc = (Datacenter) mEntity; 
            dcList.addDataCenter(new DataCenter(dc.getName()));
        }
        si.getServerConnection().logout();
        return dcList;
    }


    /* (non-Javadoc)
     * @see org.hyperic.hq.api.services.CloudProviderService#getHosts(java.lang.String)
     */
    @GET
    @Path("/dc/{dcName}")
    public HostsList getHosts(@PathParam("dcName") String dcName) throws Throwable {
        HostsList hostsList = new HostsList();
        ServiceInstance si = getServiceInstance();
        Folder rootFolder = si.getRootFolder();
        //find the data center based on the provided name
        ManagedEntity me = new InventoryNavigator(rootFolder).searchManagedEntity(DATACENTER, dcName);
        if(me==null){
            si.getServerConnection().logout();
            WebApplicationException webApplicationException = 
                    errorHandler.newWebApplicationException(Response.Status.NOT_FOUND, ExceptionToErrorCodeMapper.ErrorCode.CLOUD_RESOURCE_NOT_FOUND, dcName);     
            throw webApplicationException;
        }
        Datacenter dataCenter = (Datacenter)me;
        //iterate over the data center's hosts
        for (ManagedEntity mEntity : dataCenter.getHostFolder().getChildEntity()) {
            ComputeResource computer = (ComputeResource) mEntity;
            for (HostSystem hostSystem : computer.getHosts()) {
                hostsList.addHost(new Host(hostSystem.getName()));
            }
        }
        si.getServerConnection().logout();
        return hostsList;
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.api.services.CloudProviderService#getVms(java.lang.String, java.lang.String)
     */
    @GET
    @Path("/dc/{dcName}/host/{hostName}")
    public VirtualMachinesList getVms(@PathParam("dcName") String dcName, @PathParam("hostName") String hostName) throws Throwable {
        VirtualMachinesList vmsList = new VirtualMachinesList();
        ServiceInstance si = getServiceInstance();
        Folder rootFolder = si.getRootFolder();
        //find the host based on the provided hostname
        ManagedEntity me = new InventoryNavigator(rootFolder).searchManagedEntity(HOST_SYSTEM, hostName);
        if(me==null){
            si.getServerConnection().logout();
            WebApplicationException webApplicationException = 
                    errorHandler.newWebApplicationException(Response.Status.NOT_FOUND, ExceptionToErrorCodeMapper.ErrorCode.CLOUD_RESOURCE_NOT_FOUND, hostName);     
            throw webApplicationException;
        }
        HostSystem host = (HostSystem)me;
        //iterate over the host's virtual machines
        for (VirtualMachine vm : host.getVms()) {
            org.hyperic.hq.api.model.cloud.VirtualMachine virtualMachine = new org.hyperic.hq.api.model.cloud.VirtualMachine(vm.getName());
            virtualMachine.setIp(vm.getGuest().getIpAddress());
            vmsList.addVirtualMachine(virtualMachine);
        }
        si.getServerConnection().logout();
        return vmsList;
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.api.services.CloudProviderService#configureCloudProvider(org.hyperic.hq.api.model.cloud.CloudConfiguration)
     */
    @POST
    @Path("/configure")
    public void configureCloudProvider(CloudConfiguration cloudConfiguration) throws Throwable {
        //check that the configuration is correct
        try {
            new ServiceInstance(new URL(cloudConfiguration.getUrl()), cloudConfiguration.getUsername(), 
                    cloudConfiguration.getPassword(), true);
        }catch(Throwable t) {
            WebApplicationException webApplicationException = 
                    errorHandler.newWebApplicationException(Response.Status.NOT_ACCEPTABLE, ExceptionToErrorCodeMapper.ErrorCode.BAD_CLOUD_PROVIDER_CONFIGURATION);     
            throw webApplicationException;
        }
        //configuration is fine, save it in the session
        getSession().setAttribute(CLOUD_CONFIGURATION, cloudConfiguration);
    }

    /**
     * @return the provider configuration from the user's session
     */
    private CloudConfiguration getCloudConfiguration() {
        Object configuration = getSession().getAttribute(CLOUD_CONFIGURATION);
        if (null == configuration) {
            WebApplicationException webApplicationException = 
                    errorHandler.newWebApplicationException(Response.Status.PRECONDITION_FAILED, ExceptionToErrorCodeMapper.ErrorCode.CLOUD_PROVIDER_NOT_CONFIGURED);            
            throw webApplicationException;
        }
        return (CloudConfiguration) configuration;
    }

    /**
     * @return the vCenter service instance based on the cloud configuration object - make sure you close the instance after usage
     * @throws Throwable
     */
    private ServiceInstance getServiceInstance() throws Throwable {
        CloudConfiguration cloudConfiguration = getCloudConfiguration();
        ServiceInstance si = new ServiceInstance(new URL(cloudConfiguration.getUrl()), cloudConfiguration.getUsername(), 
                cloudConfiguration.getPassword(), true);
        return si;
    }

}
