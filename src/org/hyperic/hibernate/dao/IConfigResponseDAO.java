package org.hyperic.hibernate.dao;

import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.shared.ConfigResponsePK;

/**
 * Local home interface for ConfigResponseDB.
 */
public interface IConfigResponseDAO
{
   public ConfigResponseDB findByPlatformId(Integer id);

   public ConfigResponseDB findByServerId(Integer id);

   public ConfigResponseDB findByServiceId(Integer id);

   public ConfigResponseDB findByPrimaryKey(ConfigResponsePK pk);
}
