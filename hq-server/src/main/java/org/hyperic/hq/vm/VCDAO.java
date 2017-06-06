package org.hyperic.hq.vm;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.NonUniqueResultException;
import org.hibernate.SessionFactory;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.hyperic.hq.vm.VmMapping;

@Repository
public class VCDAO extends HibernateDAO<VmMapping> {
    protected final Log log = LogFactory.getLog(VCDAO.class.getName());

    @Autowired
    protected VCDAO(SessionFactory f) {
        super(VmMapping.class, f);
    }
    
    public void remove(List<VmMapping> macToUUIDs) {
        for(VmMapping macToUUID:macToUUIDs) {
            this.remove(macToUUID);
        }
        getSession().flush();
    }

    /**
     * This method calls to HibernateDAO save method that uses Hibernate saveOrUpdate method.
     * In case of an update the method set the Hibernate DB object properties
     * 
     * @param macToUUIDs List of VMs to save or update
     * 
     */
    public void save(List<VmMapping> macToUUIDs) {
    	VmMapping dbVmMap = null;
    	
        for(VmMapping macToUUID:macToUUIDs) {
			try {
				dbVmMap = findVMByMoref(macToUUID.getVcUUID(), macToUUID.getMoId());
			} catch (NonUniqueResultException e) {
				log.warn("Found duplicate VM's MOREF entires in DB. Ignore from VM [" + macToUUID + "]", e);
				continue;
			}
			// Check if this is a new VM
        	if (null == dbVmMap){
        		// Verify that the MAC Address is unique
        		try {
        			dbVmMap = findVMByMac(macToUUID.getMacs());
					if (null == dbVmMap){
							dbVmMap = macToUUID;
					// mac exists with different MOREF fields (different VM)		
					} else{
						log.warn("The mac is already exist in VM [" + dbVmMap + "]\nIgnore from [" + macToUUID + "]");
						continue;
					}
				} catch (DupMacException e) {
					log.warn("Found duplicate MAC address.Ignore from VM [" + macToUUID + "]", e);
					continue;
				}
        	}else {
        		if (log.isDebugEnabled()){
        			log.debug("Update existing VM instance properties [" + dbVmMap + "]");
        		}
        		// Update the VM instance properties
        		dbVmMap.set_version_(macToUUID.get_version_());
        		dbVmMap.setName(macToUUID.getName());
        		dbVmMap.setGuestNicInfo(macToUUID.getGuestNicInfo());
        		dbVmMap.setMacs(macToUUID.getMacs());
        	}
        	if (log.isDebugEnabled()){
    			log.debug("Save VM [" + dbVmMap + "]");
    		}
        	super.save(dbVmMap);
        }
        getSession().flush();
    }
    
    @SuppressWarnings("unchecked")
    public List<VmMapping> findByVcUUID(String vcUUID) {
        String sql = "from VmMapping u where u.vcUUID = :vcUUID";
        return (List<VmMapping>) getSession().createQuery(sql).setString("vcUUID", vcUUID).list();         
    }
    
    @SuppressWarnings("unchecked")
    public List<VmMapping> getVMsFromOtherVcenters(String vcUUID) {
        String sql = "from VmMapping u where u.vcUUID <> :vcUUID";
        return (List<VmMapping>) getSession().createQuery(sql).setString("vcUUID", vcUUID).list();         
    }

    @SuppressWarnings("unchecked")
    public VMID findByMac(String mac) throws DupMacException {
        String sql = "from VmMapping u where u.macs like '%+mac+%'";
        sql = sql.replace("+mac+", mac);

        List<VmMapping> rs = getSession().createQuery(sql).list();
        if (rs.size()==0) {
        	if (log.isDebugEnabled()){
        		log.debug("findByMac: no IDs are recorded for " + mac);
        	}
            return null;
        }
        if (rs.size()>1) {
            throw new DupMacException("duplicate mac address " + mac + " in the VmMapping table");
        }
        
        VmMapping macToUUID = rs.iterator().next();
        return new VMID(macToUUID.getMoId(),macToUUID.getVcUUID());
    }
    
    @SuppressWarnings("unchecked")
    public VmMapping findVMByMac(String mac) throws DupMacException {
        String sql = "from VmMapping u where u.macs like '%+mac+%'";
        sql = sql.replace("+mac+", mac);

        List<VmMapping> rs = getSession().createQuery(sql).list();
        if (rs.size()==0) {
        	if (log.isDebugEnabled()){
        		log.debug("findVMByMac: no IDs are recorded for " + mac);
        	}
            return null;
        }
        if (rs.size()>1) {
            throw new DupMacException("duplicate mac address " + mac + " in the VmMapping table");
        }
        
        VmMapping macToUUID = rs.iterator().next();
        return macToUUID;
    }
    
    /**
     * Find a VM in EAM_VM_MAPPING table by vcUUID and moId
     * 
     * @param vcUUID
     * @param moId
     * @return
     * @throws DupMacException
     */
    public VmMapping findVMByMoref(String vcUUID, String moId) throws NonUniqueResultException {
    	 String sql = "from VmMapping u where u.vcUUID = :vcUUID and u.moId = :moId";
         
    	 return (VmMapping) getSession().createQuery(sql).setString("vcUUID", vcUUID).setString("moId", moId).uniqueResult();  
    }
}
