/*
 * Generated file - Do not edit!
 */
package org.hyperic.hq.bizapp.shared;

/**
 * Utility class for UpdateBoss.
 */
public class UpdateBossUtil
{
   /** Cached remote home (EJBHome). Uses lazy loading to obtain its value (loaded by getHome() methods). */
   private static org.hyperic.hq.bizapp.shared.UpdateBossHome cachedRemoteHome = null;
   /** Cached local home (EJBLocalHome). Uses lazy loading to obtain its value (loaded by getLocalHome() methods). */
   private static org.hyperic.hq.bizapp.shared.UpdateBossLocalHome cachedLocalHome = null;

   private static Object lookupHome(java.util.Hashtable environment, String jndiName, Class narrowTo) throws javax.naming.NamingException {
      // Obtain initial context
      javax.naming.InitialContext initialContext = new javax.naming.InitialContext(environment);
      try {
         Object objRef = initialContext.lookup(jndiName);
         // only narrow if necessary
         if (java.rmi.Remote.class.isAssignableFrom(narrowTo))
            return javax.rmi.PortableRemoteObject.narrow(objRef, narrowTo);
         else
            return objRef;
      } finally {
         initialContext.close();
      }
   }

   // Home interface lookup methods

   /**
    * Obtain remote home interface from default initial context
    * @return Home interface for UpdateBoss. Lookup using JNDI_NAME
    */
   public static org.hyperic.hq.bizapp.shared.UpdateBossHome getHome() throws javax.naming.NamingException
   {
      if (cachedRemoteHome == null) {
            cachedRemoteHome = (org.hyperic.hq.bizapp.shared.UpdateBossHome) lookupHome(null, org.hyperic.hq.bizapp.shared.UpdateBossHome.JNDI_NAME, org.hyperic.hq.bizapp.shared.UpdateBossHome.class);
      }
      return cachedRemoteHome;
   }

   /**
    * Obtain remote home interface from parameterised initial context
    * @param environment Parameters to use for creating initial context
    * @return Home interface for UpdateBoss. Lookup using JNDI_NAME
    */
   public static org.hyperic.hq.bizapp.shared.UpdateBossHome getHome( java.util.Hashtable environment ) throws javax.naming.NamingException
   {
       return (org.hyperic.hq.bizapp.shared.UpdateBossHome) lookupHome(environment, org.hyperic.hq.bizapp.shared.UpdateBossHome.JNDI_NAME, org.hyperic.hq.bizapp.shared.UpdateBossHome.class);
   }

   /**
    * Obtain local home interface from default initial context
    * @return Local home interface for UpdateBoss. Lookup using JNDI_NAME
    */
   public static org.hyperic.hq.bizapp.shared.UpdateBossLocalHome getLocalHome() throws javax.naming.NamingException
   {
      if (cachedLocalHome == null) {
            cachedLocalHome = (org.hyperic.hq.bizapp.shared.UpdateBossLocalHome) lookupHome(null, org.hyperic.hq.bizapp.shared.UpdateBossLocalHome.JNDI_NAME, org.hyperic.hq.bizapp.shared.UpdateBossLocalHome.class);
      }
      return cachedLocalHome;
   }

}

