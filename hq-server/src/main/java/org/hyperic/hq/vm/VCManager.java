package org.hyperic.hq.vm;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

import org.hyperic.hq.common.ApplicationException;


public interface VCManager {

    
    /**
     * @param macs - a list of mac addresses
     * @return - the VMID of the virtual machine with the given mac address
     */
    VMID getVMID(List<String> macs);
    
    /**
     * @return a set of the existing vCenter configs
     */
    Set<VCConfig> getActiveVCConfings();
    
    /**
     * @param id - the vCenter config ID
     * @return - the vCenter config, null of not exists
     */
    VCConfig getVCConfig(int id);
    
    /**
     * @return - there should be only one (or zero) vCenter config that
     * was created via the UI, this method returns it or null if not found
     */
    VCConfig getVCConfigSetByUI();
    
    /**
     * @param url
     * @return true if there is an existing vCenter config with the provided URL
     */
    boolean vcConfigExistsByUrl(String url);
    
    /**
     * @param id
     * @return true if there is an existing vCenter config with the provided id
     */
    boolean vcConfigExists(int id);

    /**
     * @param id
     * @return true if there is an existing vCenter config with the provided id
     */
    boolean vcConfigExists(String id);
        
    /**
     * @param id - the id of the vCenter config to delete, this method
     * will also remove all the VM Mapping entries related to the deleted
     * vCenter config
     */
    void deleteVCConfig(String id);
    
    /**
     * @param id - the id of the vCenter config to delete, this method
     * will also remove all the VM Mapping entries related to the deleted
     * vCenter config
     */
    void deleteVCConfig(int id);

    /**
     * @param vc
     * @throws ApplicationException
     */
    void updateVCConfig(VCConfig vc) throws ApplicationException;



    /**
     * @param url - the vCenter SDK URL
     * @param user - the vCenter user
     * @param password - the vCenter password
     * @param setByUi - true if this vCenter config was created by the UI, false for the API
     * @return - the created vCenter config
     * @throws ApplicationException 
     */
    VCConfig addVCConfig(String url, String user, String password, boolean setByUi) throws ApplicationException;

    /**
     * @param url - the vCenter SDK URL
     * @param user - the vCenter user
     * @param password - the vCenter password
     * @return - the created vCenter config
     * @throws ApplicationException 
     */
    VCConfig addVCConfig(String url, String user, String password) throws ApplicationException;


    /**
     * @param url - the vCenter SDK URL
     * @param user - the vCenter user
     * @param password - the vCenter password
     * @throws ApplicationException
     */
    void updateVCConfig(String id, String url, String user, String password) throws ApplicationException;

    /**
     * @return
     */
    public String getActiveVCConfingsAsString();
    
}
