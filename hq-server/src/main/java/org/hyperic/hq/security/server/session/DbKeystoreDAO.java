package org.hyperic.hq.security.server.session;

import org.hibernate.SessionFactory;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class DbKeystoreDAO extends HibernateDAO<KeystoreEntryImpl> {

    @Autowired
    protected DbKeystoreDAO(SessionFactory f) {
        super(KeystoreEntryImpl.class, f);
    }
    
    @Override
    protected boolean cacheFindAll() {
        return true;
    }

}
