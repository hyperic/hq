package org.hyperic.hq.security.server.session;

import org.hibernate.SessionFactory;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;

public class HQCertificateDAO extends HibernateDAO<HQCertificate> {
    
    @Autowired
    public HQCertificateDAO(SessionFactory f) {
        super(HQCertificate.class, f);
    }

}
