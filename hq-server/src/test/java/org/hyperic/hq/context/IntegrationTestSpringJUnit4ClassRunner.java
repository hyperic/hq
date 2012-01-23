package org.hyperic.hq.context;

import org.junit.runners.model.InitializationError;
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
           throw (Exception) t ; 
        }//EO catch block 
        
        return oTestInstance ; 
    }//EOM 
    
    
    
}
