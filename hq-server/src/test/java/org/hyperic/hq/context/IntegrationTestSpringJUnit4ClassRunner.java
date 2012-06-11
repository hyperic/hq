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
