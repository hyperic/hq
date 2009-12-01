/*
 * Generated file - Do not edit!
 */
package org.hyperic.hq.product.server.mbean;

/**
 * MBean interface.
 */
public interface ProductPluginDeployerMBean extends org.jboss.deployment.SubDeployerMBean {

   /**
    * This is called when the full server startup has occurred, and you get the "Started in 30s:935ms" message. We load all startup classes, then initialize the plugins. Currently this is necesssary, since startup classes need to initialize the application (creating callbacks, etc.), and plugins can't hit the app until that's been done. Unfortunately, it also means that any startup listeners that depend on plugins loaded through the deployer won't work. So far that doesn't seem to be a problem, but if it ends up being one, we can split the plugin loading into more stages so that everyone has access to everyone.
    */
  void handleNotification(javax.management.Notification n,java.lang.Object o) ;

  org.hyperic.hq.product.ProductPluginManager getProductPluginManager() ;

  void setPluginDir(java.lang.String name) ;

  java.lang.String getPluginDir() ;

  java.util.ArrayList getRegisteredPluginNames(java.lang.String type) throws org.hyperic.hq.product.PluginException;

  java.util.ArrayList getRegisteredPluginNames() throws org.hyperic.hq.product.PluginException;

  int getProductPluginCount() throws org.hyperic.hq.product.PluginException;

  int getMeasurementPluginCount() throws org.hyperic.hq.product.PluginException;

  int getControlPluginCount() throws org.hyperic.hq.product.PluginException;

  int getAutoInventoryPluginCount() throws org.hyperic.hq.product.PluginException;

  int getLogTrackPluginCount() throws org.hyperic.hq.product.PluginException;

  int getConfigTrackPluginCount() throws org.hyperic.hq.product.PluginException;

  void setProperty(java.lang.String name,java.lang.String value) ;

  java.lang.String getProperty(java.lang.String name) ;

  org.hyperic.hq.product.PluginInfo getPluginInfo(java.lang.String name) throws org.hyperic.hq.product.PluginException;

  boolean isReady() ;

  void stop() ;

}
