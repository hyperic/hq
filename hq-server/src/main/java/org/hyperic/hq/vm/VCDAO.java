package org.hyperic.hq.vm;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    public void save(List<VmMapping> macToUUIDs) {
        for(VmMapping macToUUID:macToUUIDs) {
            super.save(macToUUID);
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
            log.error("no IDs are recorded for " + mac);
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
            log.error("no IDs are recorded for " + mac);
            return null;
        }
        if (rs.size()>1) {
            throw new DupMacException("duplicate mac address " + mac + " in the VmMapping table");
        }
        
        VmMapping macToUUID = rs.iterator().next();
        return macToUUID;
    }
}
