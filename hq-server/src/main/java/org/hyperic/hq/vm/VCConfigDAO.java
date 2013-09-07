package org.hyperic.hq.vm;

import org.hibernate.SessionFactory;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class VCConfigDAO extends HibernateDAO<VCConfig>{

    @Autowired
    public VCConfigDAO(SessionFactory f) {
        super(VCConfig.class, f);
    }
    
    public VCConfig getVCConnectionSetByUI() {
            String hql = "from VCConfig where setByUI=:setByUI";
            return (VCConfig) getSession().createQuery(hql)
                                             .setString("setByUI", "true")
                                            .uniqueResult();
    }
   

}
