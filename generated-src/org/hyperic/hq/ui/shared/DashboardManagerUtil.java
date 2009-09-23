/*
 * Generated file - Do not edit!
 */
package org.hyperic.hq.ui.shared;

/**
 * Utility class for DashboardManager.
 */
public class DashboardManagerUtil
{
   /** Cached local home (EJBLocalHome). Uses lazy loading to obtain its value (loaded by getLocalHome() methods). */
   private static org.hyperic.hq.ui.shared.DashboardManagerLocalHome cachedLocalHome = null;

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
    * Obtain local home interface from default initial context
    * @return Local home interface for DashboardManager. Lookup using JNDI_NAME
    */
   public static org.hyperic.hq.ui.shared.DashboardManagerLocalHome getLocalHome() throws javax.naming.NamingException
   {
      if (cachedLocalHome == null) {
            cachedLocalHome = (org.hyperic.hq.ui.shared.DashboardManagerLocalHome) lookupHome(null, org.hyperic.hq.ui.shared.DashboardManagerLocalHome.JNDI_NAME, org.hyperic.hq.ui.shared.DashboardManagerLocalHome.class);
      }
      return cachedLocalHome;
   }

}

