package org.hyperic.hq.vm;

import java.util.List;


public interface VCManager {

    /**
     * @param url - the vCenter URL (https://address:ip/sdk)
     * @param user - the vCenter user
     * @param password - the vCenter password
     * @return true if the credentials are valid and a successful connection to the vCenter was established
     */
    boolean validateVCSettings(String url, String user, String password);
    
    /**
     * @param url - the vCenter URL (https://address:ip/sdk)
     * @param user - the vCenter user
     * @param password - the vCenter password
     * 
     * Call this method only after you have validated the credentials,
     * this method will start mapping the vCenter VM => mac addresses
     */
    void registerVC(String url, String user, String password);
    
    /**
     * @param macs - a list of mac addresses
     * @return - the VMID of the virtual machine with the given mac address
     */
    VMID getVMID(List<String> macs);
}
