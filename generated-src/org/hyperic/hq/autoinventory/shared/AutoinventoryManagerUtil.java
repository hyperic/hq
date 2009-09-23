/*
 * Generated file - Do not edit!
 */
package org.hyperic.hq.autoinventory.shared;

/**
 * Utility class for AutoinventoryManager.
 */
public class AutoinventoryManagerUtil
{
   /** Cached remote home (EJBHome). Uses lazy loading to obtain its value (loaded by getHome() methods). */
   private static org.hyperic.hq.autoinventory.shared.AutoinventoryManagerHome cachedRemoteHome = null;
   /** Cached local home (EJBLocalHome). Uses lazy loading to obtain its value (loaded by getLocalHome() methods). */
   private static org.hyperic.hq.autoinventory.shared.AutoinventoryManagerLocalHome cachedLocalHome = null;

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
    * @return Home interface for AutoinventoryManager. Lookup using JNDI_NAME
    */
   public static org.hyperic.hq.autoinventory.shared.AutoinventoryManagerHome getHome() throws javax.naming.NamingException
   {
      if (cachedRemoteHome == null) {
            cachedRemoteHome = (org.hyperic.hq.autoinventory.shared.AutoinventoryManagerHome) lookupHome(null, org.hyperic.hq.autoinventory.shared.AutoinventoryManagerHome.JNDI_NAME, org.hyperic.hq.autoinventory.shared.AutoinventoryManagerHome.class);
      }
      return cachedRemoteHome;
   }

   /**
    * Obtain remote home interface from parameterised initial context
    * @param environment Parameters to use for creating initial context
    * @return Home interface for AutoinventoryManager. Lookup using JNDI_NAME
    */
   public static org.hyperic.hq.autoinventory.shared.AutoinventoryManagerHome getHome( java.util.Hashtable environment ) throws javax.naming.NamingException
   {
       return (org.hyperic.hq.autoinventory.shared.AutoinventoryManagerHome) lookupHome(environment, org.hyperic.hq.autoinventory.shared.AutoinventoryManagerHome.JNDI_NAME, org.hyperic.hq.autoinventory.shared.AutoinventoryManagerHome.class);
   }

   /**
    * Obtain local home interface from default initial context
    * @return Local home interface for AutoinventoryManager. Lookup using JNDI_NAME
    */
   public static org.hyperic.hq.autoinventory.shared.AutoinventoryManagerLocalHome getLocalHome() throws javax.naming.NamingException
   {
      if (cachedLocalHome == null) {
            cachedLocalHome = (org.hyperic.hq.autoinventory.shared.AutoinventoryManagerLocalHome) lookupHome(null, org.hyperic.hq.autoinventory.shared.AutoinventoryManagerLocalHome.JNDI_NAME, org.hyperic.hq.autoinventory.shared.AutoinventoryManagerLocalHome.class);
      }
      return cachedLocalHome;
   }

}

