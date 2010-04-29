package org.hyperic.hq.bizapp.server.session;

import java.util.Collection;

import org.hibernate.SessionFactory;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
@Repository
public class UpdateStatusDAO
    extends HibernateDAO
{
    @Autowired
    public UpdateStatusDAO(SessionFactory f) {
        super(UpdateStatus.class, f);
    }

    UpdateStatus get() {
        Collection vals = findAll();

        if (vals.isEmpty()) {
            return null;
        }

        return (UpdateStatus)vals.iterator().next();
    }

    void save(UpdateStatus status) {
        super.save(status);
    }
}
