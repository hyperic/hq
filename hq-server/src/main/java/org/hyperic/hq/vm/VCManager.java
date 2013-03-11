package org.hyperic.hq.vm;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

import org.hyperic.hq.common.ApplicationException;
import org.hyperic.util.ConfigPropertyException;


public interface VCManager {

    /**
     * @param url - the vCenter URL (https://address:ip/sdk)
     * @param user - the vCenter user
     * @param password - the vCenter password
     * @return true if the credentials are valid and a successful connection to the vCenter was established
     * @throws MalformedURLException 
     * @throws RemoteException 
     */
    boolean validateVCSettings(String url, String user, String password) throws RemoteException, MalformedURLException;
    
    /**
     * @param url - the vCenter URL (https://address:ip/sdk)
     * @param user - the vCenter user
     * @param password - the vCenter password
     * 
     * Call this method only after you have validated the credentials,
     * this method will start mapping the vCenter VM => mac addresses
     * @throws MalformedURLException 
     * @throws RemoteException 
     * @throws ConfigPropertyException 
     * @throws ApplicationException 
     */
    void registerOrUpdateVC(String url, String user, String password) throws RemoteException, MalformedURLException, ConfigPropertyException, ApplicationException;
    
    /**
     * @param macs - a list of mac addresses
     * @return - the VMID of the virtual machine with the given mac address
     */
    VMID getVMID(List<String> macs);
    
    Set<VCConnection> getActiveVCConnections();
    
    boolean connectionExists(String url, String user, String password);
    
    boolean connectionExists(int index);
    
    void updateConnectionByIndex(String url, String user, String password, int index) throws ApplicationException;
    
}
