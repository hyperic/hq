package org.hyperic.hq.galerts.server.session;

import java.util.Collection;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.dao.HibernateDAO;

public class GalertDefDAO
    extends HibernateDAO
{
    public GalertDefDAO(DAOFactory f) {
        super(GalertDef.class, f);
    }

    public GalertDef findById(Integer id) {
        return (GalertDef)super.findById(id);
    }

    public void save(GalertDef def) {
        super.save(def);
    }

    void remove(GalertDef def) {
        super.remove(def);
    }
    
    void remove(GtriggerInfo t) {
        super.remove(t);
    }

    void save(GtriggerInfo t) {
        super.save(t);
    }

    Collection findAll(ResourceGroup g) {
        String sql = "from GalertDef d where d.group = :group";
        
        return getSession().createQuery(sql)
            .setParameter("group", g)
            .list();
    }
    
    int countByStrategy(ExecutionStrategyTypeInfo strat) {
        String sql = "select count(*) from GalertDef d " +
            "where d.strategyInfo.type = :type";
        
        return ((Integer)getSession().createQuery(sql)
            .setParameter("type", strat)
            .uniqueResult()).intValue();
    }
    /**
     * Return GalertDef like a "value" object, parallel to existing API.
     * This guarantees that the pojo has been loaded
     * @return this with the values loaded
     */
    GalertDef getGalertDefValue(GalertDef def) {
        initialize(def.getStrategySet());
        return def;
    }
}
