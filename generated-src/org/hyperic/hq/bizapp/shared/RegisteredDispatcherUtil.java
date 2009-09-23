/*
 * Generated file - Do not edit!
 */
package org.hyperic.hq.bizapp.shared;

/**
 * Utility class for RegisteredDispatcher.
 */
public class RegisteredDispatcherUtil
{

   /** Cached queue (javax.jms.Queue). Uses lazy loading to obtain its value (loaded by getQueue() methods). */
   private static javax.jms.Queue cachedQueue = null;
   /** Cached connection factory. Uses lazy loading to obtain its value. */
   private static javax.jms.QueueConnectionFactory cachedConnectionFactory = null;

  private static final java.lang.String DESTINATION_JNDI_NAME="";
  private static final java.lang.String CONNECTION_FACTORY_JNDI_NAME="";

   /**
    * Obtain destination queue from default initial context
    * @return Destination JMS Queue for RegisteredDispatcher. Lookup using JNDI_NAME
    */
   public static javax.jms.Queue getQueue() throws javax.naming.NamingException
   {
      if (cachedQueue == null) {
         // Obtain initial context
         javax.naming.InitialContext initialContext = new javax.naming.InitialContext();
         try {
            java.lang.Object objRef = initialContext.lookup(DESTINATION_JNDI_NAME);
            cachedQueue = (javax.jms.Queue) objRef;
         } finally {
            initialContext.close();
         }
      }
      return cachedQueue;
   }

   /**
    * Obtain destination queue from parameterised initial context
    * @param environment Parameters to use for creating initial context
    * @return Destination JMS Queue for RegisteredDispatcher. Lookup using JNDI_NAME
    */
   public static javax.jms.Queue getQueue( java.util.Hashtable environment ) throws javax.naming.NamingException
   {
      // Obtain initial context
      javax.naming.InitialContext initialContext = new javax.naming.InitialContext(environment);
      try {
         java.lang.Object objRef = initialContext.lookup(DESTINATION_JNDI_NAME);
         return (javax.jms.Queue) objRef;
      } finally {
         initialContext.close();
      }
   }

   /**
    * Obtain destination queue from default initial context
    * @return Destination JMS Connection Factory for RegisteredDispatcher. Lookup using JNDI_NAME
    */
   public static javax.jms.QueueConnection getQueueConnection() throws javax.naming.NamingException, javax.jms.JMSException
   {
      if (cachedConnectionFactory == null) {
         // Obtain initial context
         javax.naming.InitialContext initialContext = new javax.naming.InitialContext();
         try {
            java.lang.Object objRef = initialContext.lookup(CONNECTION_FACTORY_JNDI_NAME);
            cachedConnectionFactory = (javax.jms.QueueConnectionFactory) objRef;
         } finally {
            initialContext.close();
         }
      }
      return cachedConnectionFactory.createQueueConnection();
   }

   /**
    * Obtain destination queue from parameterised initial context
    * @param environment Parameters to use for creating initial context
    * @return Destination JMS Connection Factory for RegisteredDispatcher. Lookup using JNDI_NAME
    */
   public static javax.jms.QueueConnection getQueueConnection( java.util.Hashtable environment ) throws javax.naming.NamingException, javax.jms.JMSException
   {
      // Obtain initial context
      javax.naming.InitialContext initialContext = new javax.naming.InitialContext(environment);
      try {
         java.lang.Object objRef = initialContext.lookup(CONNECTION_FACTORY_JNDI_NAME);
         return ((javax.jms.QueueConnectionFactory) objRef).createQueueConnection();
      } finally {
         initialContext.close();
      }
   }

}

