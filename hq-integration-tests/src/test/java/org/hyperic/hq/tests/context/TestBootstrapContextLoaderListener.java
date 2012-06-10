/* **********************************************************************
/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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
 *
 */ 	
package org.hyperic.hq.tests.context;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Hashtable;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.apache.xbean.spring.context.XmlWebApplicationContext;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.context.IntegrationTestContextLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import com.meterware.servletunit.ServletRunner;


public class TestBootstrapContextLoaderListener extends ContextLoaderListener{
	
	public TestBootstrapContextLoaderListener() { 
		super() ; 
	}//EOM 
	
	@Override
	protected final WebApplicationContext createWebApplicationContext(final ServletContext sc, final ApplicationContext parent) {
		this.injectConfigLocations(sc) ;
		return super.createWebApplicationContext(sc, parent);
	}//EOM 
	
	@Override
	protected final Class<?> determineContextClass(final ServletContext servletContext) {
		return DisposableApplicationContext.class ; 
	}//EOM 

	@Override
    protected void customizeContext(ServletContext servletContext, ConfigurableWebApplicationContext applicationContext) {
        Bootstrap.setAppContext(applicationContext) ;
		//configure sigar 
		IntegrationTestContextLoader.configureSigar(applicationContext, null) ; 
    }//EOM
	
	private final void injectConfigLocations(final ServletContext servletContext) { 
		try{
			//extract the locations from the bootstrap 
			final String[] arrSpringConfigLocations = Bootstrap.getSpringConfigLocations() ; 
			
			final Method getContextParamsMethod = servletContext.getClass().getDeclaredMethod("getContextParams") ;
			getContextParamsMethod.setAccessible(true)  ;
			final Hashtable contextParams = (Hashtable) getContextParamsMethod.invoke(servletContext) ;
			
			final StringBuilder builder = new StringBuilder() ; 
			final int iLength = arrSpringConfigLocations.length ; 
			for(int i=0; i < iLength; i++) { 
				builder.append(arrSpringConfigLocations[i]) ; 
				if(i < iLength-1) builder.append("\n") ; 
			}//EO while there are more spring config locations 
			
			contextParams.put("contextConfigLocation", builder.toString()) ; 
		}catch(Throwable t){ 
			throw (t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t)) ;  
		}//EO catch  block 
	}//EOM	
	
	private final void injectSpringConfigLocations(final String...arrSpringConfigLocations) { 
		try{ 
			final Field contextField = this.getClass().getSuperclass().getDeclaredField("_context") ;
			final Method getContextParamsMethod = contextField.get(this).getClass().getDeclaredMethod("getContextParams") ;
			final HashMap contextParams = (HashMap) getContextParamsMethod.invoke(contextField) ; 
			
			final StringBuilder builder = new StringBuilder() ; 
			final int iLength = arrSpringConfigLocations.length ; 
			for(int i=0; i < iLength; i++) { 
				builder.append(arrSpringConfigLocations[i]) ; 
				if(i < iLength-1) builder.append("\n") ; 
			}//EO while there are more spring config locations 
			
			contextParams.put("contextConfigLocation", builder.toString()) ; 
		}catch(Throwable t){ 
			throw (t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t)) ;  
		}//EO catch  block 
	}//EOM
	
	private static final class DisposableApplicationContext extends XmlWebApplicationContext { 
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
	
	@Override
	public void contextDestroyed(ServletContextEvent event) {
		super.contextDestroyed(event);
	}//EOM 
	
}//EOC 
