/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2012], VMWare, Inc.
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
package org.hyperic.hq.context;

import java.lang.reflect.Field;

import org.junit.runners.model.InitializationError;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Ensures the disposal of a spring context on failure so as to release resources.  
 * @author guy
 *
 */
public class IntegrationTestSpringJUnit4ClassRunner extends SpringJUnit4ClassRunner{

    public IntegrationTestSpringJUnit4ClassRunner(Class<?> clazz) throws InitializationError{
        super(clazz) ; 
        
    }//EOM
    
    @Override
    protected final Object createTest() throws Exception {
        Object oTestInstance = null ;  
        try{ 
            oTestInstance = super.createTest();
        }catch(Throwable t) { 
        	
            //dispose of the Bootstrap context 
           Bootstrap.dispose() ; 
           throw (t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t)) ; 
        }//EO catch block 
        
        return oTestInstance ; 
    }//EOM 
    
    protected TestContextManager createTestContextManager(Class<?> clazz) {
		return new TestContextManagerWrapper(clazz, getDefaultContextLoaderClassName(clazz)) ;  
	}//EOM 
    
    private class TestContextManagerWrapper extends TestContextManager { 
    	
    	public TestContextManagerWrapper(Class<?> testClass, String defaultContextLoaderClassName) {
    		super(testClass, defaultContextLoaderClassName)  ;
    		this.injectTestClassToContextLoader(testClass) ; 
    	}//EOM 
    	
    	private final void injectTestClassToContextLoader(final Class<?> testClass) { 
    		try{ 
    			final TestContext testContext = this.getTestContext() ; 
    			final Field contextLoaderField = testContext.getClass().getDeclaredField("contextLoader") ;
    			contextLoaderField.setAccessible(true) ;
    			final ContextLoader contextLoader = (ContextLoader) contextLoaderField.get(testContext) ;
    			if(contextLoader instanceof IntegrationTestContextLoader) ((IntegrationTestContextLoader)contextLoader).setTestClass(testClass) ; 
    		}catch(Throwable t){ 
    			throw (t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t)) ;
    		}//EO catch block 
    	}//EOM 
    }//EO inner class 
    
    
    
}//EOC 
