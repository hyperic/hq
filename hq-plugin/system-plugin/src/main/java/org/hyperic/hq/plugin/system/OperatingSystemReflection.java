/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.plugin.system;


import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.OperatingSystem;


public class OperatingSystemReflection {
    protected static Log log = LogFactory.getLog("OperatingSystemReflection");    
    private static final String CLASS_NAME = "org.hyperic.hq.product.HypericOperatingSystem";
    
    private static Class<?> getHypericOperatingSystemClass()
    {
        try {
            Class<?> hypericOperatingSystemClass = Class.forName (CLASS_NAME);
            log.debug(CLASS_NAME + " found!");
            return hypericOperatingSystemClass;
        }
        catch (ClassNotFoundException exception) {       
            log.debug(CLASS_NAME + " not found!");
            return null;
        }
    }
    
    private static Object getStaticFieldValue(String fieldName) {
        Field f1;
        try {
            Class<?> hypericOperatingSystemClass  = getHypericOperatingSystemClass();
            if (hypericOperatingSystemClass == null) {
                return null;
            }
            f1 = hypericOperatingSystemClass.getDeclaredField(fieldName);
            Object o = f1.get(null);
            return o;
        }catch(Exception e) {
            log.error("failed to get field=" + fieldName, e);
            return null;
        }
    }
    
      
    private static Object invokeStaticMethodWithParam(String methodName, String param) {
        try {
            Class<?> hypericOperatingSystemClass  = getHypericOperatingSystemClass();
            if (hypericOperatingSystemClass == null) {
                return null;
            }

            Method method = hypericOperatingSystemClass.getMethod(methodName, String.class);            
            Object o = method.invoke(null, param);
            return o;            
        } catch (Exception e) {        
            log.error("invokeStaticMethodWithParam failed =" + methodName + " param=" + param, e);
            return null;
        }
    }

    
    private static Object invokeStaticMethod(String methodName, Class<?> hypericOperatingSystemClass) {
        try {
            Method method = hypericOperatingSystemClass.getMethod(methodName);            
            Object o = method.invoke(null);
            return o;
            
        } catch (Exception e) {
            log.error("invokeStaticMethod failed =" + methodName, e);
            return null;
        }
    }

        
    private static Object getInstance(Class<?> hypericOperatingSystemClass) {
        return invokeStaticMethod("getInstance", hypericOperatingSystemClass);
    }
    
    
    private static Object invokeObjectMethod(String methodName) {
        Class<?> hypericOperatingSystemClass  = getHypericOperatingSystemClass();
        if (hypericOperatingSystemClass == null) {
            return null;
        }

        Object o = getInstance(hypericOperatingSystemClass);
        if (o == null) {
            return null;
        }
        // now invoke non static methos
        try {            
            Method method = hypericOperatingSystemClass.getMethod(methodName);
            Object res = method.invoke(o);
            return res;
        }
        catch(Exception e) {
            log.error("failed to invoke method=" + methodName);
            return null;
        }
    }
    
    public static String getName() {        
        String name =  (String)invokeObjectMethod("getName");
        if (name == null) {
            return OperatingSystem.getInstance().getName();
        }
        return name;
    }
    
    public static String getDescription() {
       String res =  (String)invokeObjectMethod("getDescription");
       if (res == null) {
           res = OperatingSystem.getInstance().getDescription();
       }
       return res;
    }
    
    
    public static String getArch() {        
       String res =  (String)invokeObjectMethod("getArch");
       if (res == null) {
           res = OperatingSystem.getInstance().getArch();
       }
       return res;
    }
    
    public static String getVersion() {      
       String res =  (String)invokeObjectMethod("getVersion");
       if (res == null) {
           res = OperatingSystem.getInstance().getVersion();
       }
       return res;
    }
    
    public static String getVendor() {      
       String res =  (String)invokeObjectMethod("getVendor");
       if (res == null) {
           res = OperatingSystem.getInstance().getVendor();
       }
       return res;
    }
    
    public static String getVendorVersion() {        
        String res = (String)invokeObjectMethod("getVendorVersion");
        if (res == null) {
            res = OperatingSystem.getInstance().getVendorVersion();
        }
        return res;
    }
    
    public static boolean isWin32(String osName) {
        Object res = invokeStaticMethodWithParam("isWin32", osName);
        if (res == null) {
            return OperatingSystem.isWin32(osName);
        }
        return (Boolean)res;
    }

    
    public static boolean IS_WIN32() {     
         Object o = getStaticFieldValue("IS_WIN32");
         if ( o == null) {
             return OperatingSystem.IS_WIN32;
         } 
         return (Boolean)o;       
    }
    
    public static boolean IS_HYPER_V() {      
         Object o = getStaticFieldValue("IS_HYPER_V");
         if ( o == null) {
            return false;
         }
         return (Boolean)o;
    }

}
