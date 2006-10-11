package org.hyperic.hibernate.dao;

import org.hyperic.hq.appdef.CpropKey;

/**
 * Finders for CPropKey. (from EJB LocalHome interface)
 */
public interface ICPropKeyDAO
{
   public CpropKey create(int param1,
                          int param2,
                          java.lang.String param3,
                          java.lang.String param4);

    public java.util.Collection findByAppdefType(int appdefType, int appdefId);

    public CpropKey findByKey(int appdefType,
                              int appdefTypeId,
                              String key);
    
    public CpropKey findByPrimaryKey(org.hyperic.hq.appdef.shared.CPropKeyPK pk);
}
