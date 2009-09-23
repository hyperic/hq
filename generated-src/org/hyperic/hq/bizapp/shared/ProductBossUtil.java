/*
 * Generated file - Do not edit!
 */
package org.hyperic.hq.bizapp.shared;

/**
 * Utility class for ProductBoss.
 */
public class ProductBossUtil
{
   /** Cached remote home (EJBHome). Uses lazy loading to obtain its value (loaded by getHome() methods). */
   private static org.hyperic.hq.bizapp.shared.ProductBossHome cachedRemoteHome = null;
   /** Cached local home (EJBLocalHome). Uses lazy loading to obtain its value (loaded by getLocalHome() methods). */
   private static org.hyperic.hq.bizapp.shared.ProductBossLocalHome cachedLocalHome = null;

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
    * @return Home interface for ProductBoss. Lookup using JNDI_NAME
    */
   public static org.hyperic.hq.bizapp.shared.ProductBossHome getHome() throws javax.naming.NamingException
   {
      if (cachedRemoteHome == null) {
            cachedRemoteHome = (org.hyperic.hq.bizapp.shared.ProductBossHome) lookupHome(null, org.hyperic.hq.bizapp.shared.ProductBossHome.JNDI_NAME, org.hyperic.hq.bizapp.shared.ProductBossHome.class);
      }
      return cachedRemoteHome;
   }

   /**
    * Obtain remote home interface from parameterised initial context
    * @param environment Parameters to use for creating initial context
    * @return Home interface for ProductBoss. Lookup using JNDI_NAME
    */
   public static org.hyperic.hq.bizapp.shared.ProductBossHome getHome( java.util.Hashtable environment ) throws javax.naming.NamingException
   {
       return (org.hyperic.hq.bizapp.shared.ProductBossHome) lookupHome(environment, org.hyperic.hq.bizapp.shared.ProductBossHome.JNDI_NAME, org.hyperic.hq.bizapp.shared.ProductBossHome.class);
   }

   /**
    * Obtain local home interface from default initial context
    * @return Local home interface for ProductBoss. Lookup using JNDI_NAME
    */
   public static org.hyperic.hq.bizapp.shared.ProductBossLocalHome getLocalHome() throws javax.naming.NamingException
   {
      if (cachedLocalHome == null) {
            cachedLocalHome = (org.hyperic.hq.bizapp.shared.ProductBossLocalHome) lookupHome(null, org.hyperic.hq.bizapp.shared.ProductBossLocalHome.JNDI_NAME, org.hyperic.hq.bizapp.shared.ProductBossLocalHome.class);
      }
      return cachedLocalHome;
   }

}

