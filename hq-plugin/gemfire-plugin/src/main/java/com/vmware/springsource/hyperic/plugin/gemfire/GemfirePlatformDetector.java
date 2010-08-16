 package com.vmware.springsource.hyperic.plugin.gemfire;
 
 import org.apache.commons.logging.Log;
 import org.hyperic.hq.product.PlatformDetector;
 import org.hyperic.hq.product.PlatformResource;
 import org.hyperic.hq.product.PluginException;
 import org.hyperic.util.config.ConfigResponse;
 
 public class GemfirePlatformDetector extends PlatformDetector
 {
   private static final String DEF_URL = "service:jmx:rmi://localhost/jndi/rmi://:1099/jmxconnector";
 
   public PlatformResource getPlatformResource(ConfigResponse config)
     throws PluginException
   {
     getLog().debug("[getPlatformResource] config=" + config);
     config.setValue("jmx.url", "service:jmx:rmi://localhost/jndi/rmi://:1099/jmxconnector");
     PlatformResource res = super.getPlatformResource(config);
     res.setMeasurementConfig(new ConfigResponse());
     return res;
   }
 }