package org.hyperic.hq.vm;

import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class VCDAO extends HibernateDAO<MacToUUID> {
    protected final Log log = LogFactory.getLog(VCDAO.class.getName());

    @Autowired
    protected VCDAO(SessionFactory f) {
        super(MacToUUID.class, f);
    }
    
    public void remove(List<MacToUUID> macToUUIDs) {
        for(MacToUUID macToUUID:macToUUIDs) {
            this.remove(macToUUID);
        }
        getSession().flush();
    }

    public void save(List<MacToUUID> macToUUIDs) {
        for(MacToUUID macToUUID:macToUUIDs) {
            super.save(macToUUID);
        }
        getSession().flush();
    }
    
    @SuppressWarnings("unchecked")
    public VMID findByMac(String mac) throws DupMacException {
        String sql = "from MacToUUID u where u.mac = :mac ";

        List<MacToUUID> rs = getSession().createQuery(sql).setString("mac", mac).list();
        if (rs.size()==0) {
            log.error("no IDs are recorded for " + mac);
            return null;
        }
        if (rs.size()>1) {
            throw new DupMacException("duplicate mac address " + mac + " in the UUID table");
        }
        
        MacToUUID macToUUID = rs.iterator().next();
        return new VMID(macToUUID.getMORef(),macToUUID.getVcUUID());
    }
}
