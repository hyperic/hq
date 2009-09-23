/*
 * Generated file - Do not edit!
 */
package org.hyperic.hq.escalation.shared;

/**
 * Utility class for EscalationManager.
 */
public class EscalationManagerUtil
{
   /** Cached local home (EJBLocalHome). Uses lazy loading to obtain its value (loaded by getLocalHome() methods). */
   private static org.hyperic.hq.escalation.shared.EscalationManagerLocalHome cachedLocalHome = null;

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
    * @return Local home interface for EscalationManager. Lookup using JNDI_NAME
    */
   public static org.hyperic.hq.escalation.shared.EscalationManagerLocalHome getLocalHome() throws javax.naming.NamingException
   {
      if (cachedLocalHome == null) {
            cachedLocalHome = (org.hyperic.hq.escalation.shared.EscalationManagerLocalHome) lookupHome(null, org.hyperic.hq.escalation.shared.EscalationManagerLocalHome.JNDI_NAME, org.hyperic.hq.escalation.shared.EscalationManagerLocalHome.class);
      }
      return cachedLocalHome;
   }

}

