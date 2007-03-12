package org.hyperic.hq.bizapp.server.session;

import java.util.Collection;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.dao.HibernateDAO;

public class UpdateStatusDAO 
    extends HibernateDAO
{
    public UpdateStatusDAO(DAOFactory f) {
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
