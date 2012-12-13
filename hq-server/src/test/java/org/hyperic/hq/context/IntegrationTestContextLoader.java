/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2012], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.context;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.sigar.Sigar;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.concurrent.ExecutorConfigurationSupport;
import org.springframework.test.context.support.AbstractContextLoader;
import org.springframework.test.context.support.GenericXmlContextLoader;
import org.springframework.util.StringUtils;

public class IntegrationTestContextLoader extends AbstractContextLoader {
    protected static final Log logger = LogFactory.getLog(IntegrationTestContextLoader.class);
    
    private static Sigar sigar ; 
    private static Field platformBeanServerField ;
    protected Class<?> testClass ; 
    private static boolean initializedSysProps ; 
    
    private final ExternalizingGenericXmlContextLoader delegateLoader ;  
    
    public IntegrationTestContextLoader() { 
        super();
        this.delegateLoader = new ExternalizingGenericXmlContextLoader() ;  
    }//EOM 
    
    public final void setTestClass(final Class<?> testClass) { 
        this.testClass = testClass ; 
    }//EOM 
    
    public static final void configureSigar(final ApplicationContext context, final Log externalLogger) { 
        final Log log = (externalLogger == null ? logger : externalLogger) ;

        try {
            overrideProperties(log) ; 
            
            //Find the sigar libs on the test classpath
            final File sigarBin = new File(context.getResource("/libsigar-sparc64-solaris.so").getFile().getParent());
            log.info("Setting sigar path to : " + sigarBin.getAbsolutePath());
            System.setProperty("org.hyperic.sigar.path",sigarBin.getAbsolutePath());
            
            //ensure that the Sigar native libraries are loaded and remain in the classloader's context 
            sigar = new Sigar() ; 
            Sigar.load() ; 
            
        } catch (Throwable t) {
            log.error("Unable to initiailize sigar path",t);
            throw new SystemException(t) ; 
        }//EO catch block
    }//EOM 
    
    private static final void overrideProperties(final Log log) {
        if(initializedSysProps)  return ; 
        
        final String OVERRIDE_PREFIX =  "override." ;
        final int OVERRIDE_PREFIX_LENGTH = OVERRIDE_PREFIX.length() ;
        
        final Properties sysProps = System.getProperties() ; 
       
        final Map<String,String> newProps = new HashMap<String,String>() ; 
        
        String origSysPropName, overrideSysPropName, origSyspropVal, overrideVal ;
        int indexOfPrefix ; 
        
        for(Entry<Object,Object> syspropEntry : sysProps.entrySet()) { 
            overrideSysPropName = (String) syspropEntry.getKey() ; 
            if( (indexOfPrefix = overrideSysPropName.indexOf(OVERRIDE_PREFIX)) == -1 ) continue ; 
            //first search for an explicitly defined corresponding original sys prop 
            //and found skip the override.
            origSysPropName = overrideSysPropName.substring(OVERRIDE_PREFIX_LENGTH) ;
            origSyspropVal  = System.getProperty(origSysPropName) ;
            if(origSyspropVal != null) continue ; 
            //else create a new orig sys prop with the override value 
            overrideVal = (String) syspropEntry.getValue() ; 
            newProps.put(origSysPropName, overrideVal) ;
            
            log.info("An "+overrideSysPropName+" property value was provided : " + overrideVal + " setting in the " + origSysPropName + " system property") ;
        }//EO while there are more system properties
        
        for(Map.Entry<String,String> newPropEntry : newProps.entrySet())  {
            System.setProperty(newPropEntry.getKey(), newPropEntry.getValue()) ;
        }//EO while there are more new properties  
       
        initializedSysProps = true ;        
    }//EOM 
    
    private final void overrideProperties(final String[][] properties, final Log log) { 
        
        final int length = properties.length ;
        
        String sysPropName, overrideSysPropName,  syspropOrigVal, overrideVal ;  
        for(int i=0; i < length; i++) { 
            sysPropName = properties[i][0] ; 
            syspropOrigVal  = System.getProperty(sysPropName) ; 
            //if the org system property already exists then it was explicitly defined and should 
            //not be overriden 
            if(syspropOrigVal != null) continue ; 
            //else attempt to looukup the overrding property value and if found, define
            //a new system proeprty with the orig name and the overriding value 
            overrideSysPropName = properties[i][1];  
            overrideVal = System.getProperty(overrideSysPropName) ;
            if(overrideVal == null) continue ; 
            //else there is an override value and not explicitly defined orig value so override 
            log.info("An "+overrideSysPropName+" property value was provided : " + overrideVal + " setting in the " + sysPropName + " system property") ;
            
        }//EO while there are more properties to potentially override
        
    }//EOM 

    public ApplicationContext loadContext(final String... locations) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Loading ApplicationContext for locations [" +
                    StringUtils.arrayToCommaDelimitedString(locations) + "].");
        }//EO if logger is enabled 
        
        final GenericApplicationContext context = new ProxyingGenericApplicationContext();
        //verify sigar's resources existence & load native libraries 
        configureSigar(context, logger) ; 
        
        //clean previous application context (if exists) and create a new one 
        Bootstrap.setAppContext(context) ; 

        try{ 
            //initialize the application context 
            delegateLoader.createBeanDefinitionReader(context).loadBeanDefinitions(locations);
            AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
            context.refresh();
            context.registerShutdownHook();
            
            return context;
        }catch(Throwable t) { 
            logger.error("An Error had occured during the applicationContext creation, disposing!") ; 
            Bootstrap.dispose() ;
            throw (Exception) t ; 
        }//EO catch block 
    }//EOM 
    
    @Override
    public final String getResourceSuffix() {
        return this.delegateLoader.getResourceSuffix() ; 
    }//EOM 
    
    /**
     * Class used to expose the {@link GenericXmlContextLoader#createBeanDefinitionReader} method so that it<br> 
     * could be delegated to by the encapsulating class  
     * @author guy
     *
     */
    private static final class ExternalizingGenericXmlContextLoader extends GenericXmlContextLoader { 
        @Override
        public final BeanDefinitionReader createBeanDefinitionReader(GenericApplicationContext context) {
            return super.createBeanDefinitionReader(context);
        }
    }//EOC ExternalizingGenericXmlContextLoader
    
    private static final class DisposableApplicationContext extends GenericApplicationContext { 
        @Override
        public final void close() {
            if(!this.isActive()) { 
             // Destroy all cached singletons in the context's BeanFactory.
                this.destroyBeans();

                // Close the state of this context itself.
                this.closeBeanFactory();
                
                //close the parent as well 
                if(this.getParent() != null) ((ConfigurableApplicationContext)this.getParent()).close() ; 
            }//EO if not active 
            else { 
                super.close();
            }//EO else normal close 
        }//EOM 
        
    }//EOC DisposableApplicationContext
    
    /**
     * Wrapper around an {@link GenericApplicationContext} instance used to control its references.<br>
     * the {@link ProxyingGenericApplicationContext#close()} shall clear context resources and ensure that the<br>
     * underlying applicationContext instance could be garbage collected so as to prevent memroy leaks.
     *  
     * @author guy
     *
     */
    protected static final class ProxyingGenericApplicationContext extends GenericApplicationContext { 
       
        private AbstractApplicationContext delegate ; 
        private boolean isDelegateInstanceofGenericAppContext ; 
        
        public ProxyingGenericApplicationContext() {
            super() ; 
            this.delegate = new DisposableApplicationContext() ; 
            this.isDelegateInstanceofGenericAppContext = true ; 
        }//EOM
        
        public ProxyingGenericApplicationContext(AbstractApplicationContext appcontext) {
            super() ; 
            this.delegate = appcontext ; 
            this.isDelegateInstanceofGenericAppContext = (appcontext instanceof GenericApplicationContext) ; 
        }//EOM 

        @Override
        public void setParent(ApplicationContext parent) {
            this.delegate.setParent(parent);
        }//EOM 
        
        @Override
        public void setId(String id) {
            this.delegate.setId(id);
        }//EOM 

        @Override
        public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
            if(isDelegateInstanceofGenericAppContext) { 
                ((GenericApplicationContext)this.delegate).setAllowBeanDefinitionOverriding(allowBeanDefinitionOverriding);
            }//EO if delegate is generic application context  
        }//EOM 

        @Override
        public void setAllowCircularReferences(boolean allowCircularReferences) {
            if(isDelegateInstanceofGenericAppContext) { 
                ((GenericApplicationContext)this.delegate).setAllowCircularReferences(allowCircularReferences);
            }//EO if delegate is generic application context 
        }//EOM 
   
        @Override
        public void setResourceLoader(ResourceLoader resourceLoader) {
            if(isDelegateInstanceofGenericAppContext) { 
                ((GenericApplicationContext)this.delegate).setResourceLoader(resourceLoader);
            }//EO if delegate is generic application context 
        }//EOM 

        @Override
        public Resource getResource(String location) {
            return this.delegate.getResource(location);
        }//EOM 

        @Override
        public Resource[] getResources(String locationPattern) throws IOException {
            return this.delegate.getResources(locationPattern);
        }//EOM 

        @Override
        public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
                throws BeanDefinitionStoreException {
            if(isDelegateInstanceofGenericAppContext) { 
                ((GenericApplicationContext)this.delegate).registerBeanDefinition(beanName, beanDefinition);
            }//EO if delegate is generic application context 
        }//EOM 

        @Override
        public void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
            if(isDelegateInstanceofGenericAppContext) { 
                ((GenericApplicationContext)this.delegate).removeBeanDefinition(beanName);
            }//EO if delegate is generic application context 
        }

        @Override
        public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
            if(isDelegateInstanceofGenericAppContext) {   
                return ((GenericApplicationContext)this.delegate).getBeanDefinition(beanName);
            }//EO if delegate is generic application context
            else throw new UnsupportedOperationException();
        }//EOM 

        @Override
        public boolean isBeanNameInUse(String beanName) {
            if(isDelegateInstanceofGenericAppContext) { 
                return ((GenericApplicationContext)this.delegate).isBeanNameInUse(beanName);
            }//EO if delegate is generic application context
            else throw new UnsupportedOperationException();
        }//EOM 
 
        @Override
        public void registerAlias(String beanName, String alias) {
            if(isDelegateInstanceofGenericAppContext) { 
                ((GenericApplicationContext)this.delegate).registerAlias(beanName, alias);
            }//EO if delegate is generic application context
        }//EOM 

        @Override
        public void removeAlias(String alias) {
            if(isDelegateInstanceofGenericAppContext) { 
                ((GenericApplicationContext)this.delegate).removeAlias(alias);
            }//EO if delegate is generic application context
        }//EOM 

        @Override
        public boolean isAlias(String beanName) {
            if(isDelegateInstanceofGenericAppContext) { 
                return ((GenericApplicationContext)this.delegate).isAlias(beanName);
            }//EO if delegate is generic application context
            else throw new UnsupportedOperationException() ;
        }//EOM 

        @Override
        public String getId() {
            
            return this.delegate.getId();
        }//EOM 

        @Override
        public void setDisplayName(String displayName) {
            
            this.delegate.setDisplayName(displayName);
        }//EOM 

        @Override
        public String getDisplayName() {
            
            return this.delegate.getDisplayName();
        }//EOM 

        @Override
        public ApplicationContext getParent() {
            
            return this.delegate.getParent();
        }//EOM 

        @Override
        public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
            
            return this.delegate.getAutowireCapableBeanFactory();
        }//EOM 

        @Override
        public long getStartupDate() {
            
            return this.delegate.getStartupDate();
        }//EOM 

        @Override
        public void publishEvent(ApplicationEvent event) {
            
            this.delegate.publishEvent(event);
        }//EOM 

        @Override
        public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor beanFactoryPostProcessor) {
            
            this.delegate.addBeanFactoryPostProcessor(beanFactoryPostProcessor);
        }//EOM 

        @Override
        public List<BeanFactoryPostProcessor> getBeanFactoryPostProcessors() {
            
            return this.delegate.getBeanFactoryPostProcessors();
        }//EOM 

        @Override
        public void addApplicationListener(ApplicationListener listener) {
            
            this.delegate.addApplicationListener(listener);
        }//EOM 

        @Override
        public Collection<ApplicationListener> getApplicationListeners() {
            
            return this.delegate.getApplicationListeners();
        }//EOM 

        @Override
        public void refresh() throws BeansException, IllegalStateException {
            this.delegate.refresh();
        }//EOM 

        @Override
        public void registerShutdownHook() {
            this.delegate.registerShutdownHook();
        }//EOM 

        @Override
        public void destroy() {
            this.close() ; 
        }//EOM 

        /**
         * Severs any hard references between the delegate and client code so that the former could be garbage 
         * collected.<br>
         * Moreover, releases JMX resources otherwise designed to be freed during JVM shutdown.
         */
        @Override
        public void close() {
            if(this.delegate == null) { 
                return ; 
            }//EO if already closed 
            
            try{  
                Object scheduler = this.delegate.getBean("scheduler") ; 
                if(scheduler instanceof ExecutorConfigurationSupport) ((ExecutorConfigurationSupport) scheduler).shutdown() ;
            }catch(org.springframework.beans.factory.NoSuchBeanDefinitionException nsbde)  { 
                //swallow exception
            }catch(Throwable t) { 
                t.printStackTrace() ; 
            }//EO catch block 
            
            //Clear the JMX resources 
            this.resetJMXResources() ; 
                        
            this.delegate.close() ;
            
            this.delegate = null ;
            
            //"request" GC 
            final int iNoOfGCRequests = 3 ; 
            for(int i=0; i < iNoOfGCRequests; i++) { 
                System.gc() ;   
            }//EO while there are more tests 
        }//EOM 
        
        
        
        /*
         * Clears the ManagementFactory's platformBeanServerField member as well as the reference in 
         * the MBeanServerFactory. 
         */
        private final void resetJMXResources() {
            try{
                final String meanServerBeanName = "mbeanServer" ; 
                if(!this.containsBean(meanServerBeanName)) return ; 
                
                //release jmx resources if exist in the spring context scope 
                final MBeanServer mbeanServer = (MBeanServer) this.getBean(meanServerBeanName) ; 
                if(mbeanServer == null) return ; 
                //else 
                //stop the singleton service (done here instead of the (EE's HaServiceImpl as to minimize 
                //affected code for release)
                final ObjectName o = new ObjectName("hyperic.jmx:type=Service,name=EEHAService-HASingletonController");
                if(mbeanServer.isRegistered(o)) { 
                    System.out.println(mbeanServer.invoke(o, "stop", new Object[] {}, new String[] {})) ;
                }//EO if the singleton exists 
                
                if(platformBeanServerField == null) { 
                    platformBeanServerField = ManagementFactory.class.getDeclaredField("platformMBeanServer") ;
                    platformBeanServerField.setAccessible(true) ; 
                }//EO if the platformBeanServerField was not yet initialized
                
                //set the value to null 
                platformBeanServerField.set(null/*instance*/, null/*value*/) ; 
                
                //clear the mbean server cache from the mbean server factory 
                MBeanServerFactory.releaseMBeanServer(mbeanServer) ; 
            }catch(Throwable t) { 
                throw new SystemException(t) ; 
            }//EO catch block 
                
        }//EOM 
        
        @Override
        public boolean isActive() {
            
            return this.delegate.isActive();
        }//EOM 

        @Override
        public Object getBean(String name) throws BeansException {
            
            return this.delegate.getBean(name);
        }//EOM 

        @Override
        public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
            
            return this.delegate.getBean(name, requiredType);
        }//EOM 

        @Override
        public <T> T getBean(Class<T> requiredType) throws BeansException {
            
            return this.delegate.getBean(requiredType);
        }//EOM 

        @Override
        public Object getBean(String name, Object... args) throws BeansException {
            
            return this.delegate.getBean(name, args);
        }//EOM 

        @Override
        public boolean containsBean(String name) {
            
            return this.delegate.containsBean(name);
        }//EOM 

        @Override
        public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
            
            return this.delegate.isSingleton(name);
        }//EOM 

        @Override
        public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
            
            return this.delegate.isPrototype(name);
        }//EOM 

        @Override
        public boolean isTypeMatch(String name, Class targetType) throws NoSuchBeanDefinitionException {
            
            return this.delegate.isTypeMatch(name, targetType);
        }//EOM 

        @Override
        public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
            
            return this.delegate.getType(name);
        }//EOM 

        @Override
        public String[] getAliases(String name) {
            
            return this.delegate.getAliases(name);
        }//EOM 

        @Override
        public boolean containsBeanDefinition(String beanName) {
            
            return this.delegate.containsBeanDefinition(beanName);
        }//EOM 

        @Override
        public int getBeanDefinitionCount() {
            
            return this.delegate.getBeanDefinitionCount();
        }//EOM 

        @Override
        public String[] getBeanDefinitionNames() {
            
            return this.delegate.getBeanDefinitionNames();
        }//EOM 

        @Override
        public String[] getBeanNamesForType(Class type) {
            
            return this.delegate.getBeanNamesForType(type);
        }//EOM 

        @Override
        public String[] getBeanNamesForType(Class type, boolean includeNonSingletons, boolean allowEagerInit) {
            
            return this.delegate.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
        }//EOM 

        @Override
        public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
            
            return this.delegate.getBeansOfType(type);
        }//EOM 

        @Override
        public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
                throws BeansException {
            
            return this.delegate.getBeansOfType(type, includeNonSingletons, allowEagerInit);
        }//EOM 

        @Override
        public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
                throws BeansException {
            
            return this.delegate.getBeansWithAnnotation(annotationType);
        }//EOM 

        @Override
        public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) {
            
            return this.delegate.findAnnotationOnBean(beanName, annotationType);
        }//EOM 

        @Override
        public BeanFactory getParentBeanFactory() {
            
            return this.delegate.getParentBeanFactory();
        }//EOM 

        @Override
        public boolean containsLocalBean(String name) {
            
            return this.delegate.containsLocalBean(name);
        }//EOM 

        @Override
        public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
            
            return this.delegate.getMessage(code, args, defaultMessage, locale);
        }//EOM 

        @Override
        public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
            
            return this.delegate.getMessage(code, args, locale);
        }//EOM 

        @Override
        public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
            
            return this.delegate.getMessage(resolvable, locale);
        }//EOM 

        @Override
        public void start() {
            
            this.delegate.start();
        }//EOM 

        @Override
        public void stop() {
            
            this.delegate.stop();
        }//EOM 

        @Override
        public boolean isRunning() {
            
            return this.delegate.isRunning();
        }//EOM 

        @Override
        public String toString() {
            
            return this.delegate.toString();
        }//EOM 

        @Override
        public void setClassLoader(ClassLoader classLoader) {
            
            this.delegate.setClassLoader(classLoader);
        }//EOM 

        @Override
        public ClassLoader getClassLoader() {
            
            return this.delegate.getClassLoader();
        }//EOM 
        
    }//EOC ProxyingGenericApplicationContext
    
    public static void main(String[] args) throws Throwable {
        overrideProperties(logger) ;
    }//EOM 
}//EOC 
