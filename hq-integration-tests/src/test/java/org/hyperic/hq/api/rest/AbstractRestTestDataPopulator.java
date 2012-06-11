package org.hyperic.hq.api.rest;

import java.lang.reflect.Array;

import org.hyperic.hq.api.rest.RestTestCaseBase.ServiceBindingType;
import org.hyperic.hq.test.TestHelper;
import org.hyperic.hq.tests.context.TestDataPopulator;
import org.springframework.beans.factory.annotation.Autowired;

import com.meterware.servletunit.ServletRunner;

public abstract class AbstractRestTestDataPopulator<T> extends TestHelper implements TestDataPopulator{ 

	@Autowired
	protected ServletRunner servletRunner ; 
	
    private T[] arrServices ; 
	
    private String serviceURL ; 
    private Class<T> serviceInterface ; 
    
    public AbstractRestTestDataPopulator(final Class<T> serviceInterface, final String serviceURL) { 
    	super() ; 
    	this.serviceInterface = serviceInterface ; 
    	this.serviceURL = serviceURL ; 
    }//EOM 
    
    @Override
	public void populate() throws Exception {
    	this.generateServices() ; 
    }//EOM 
    
    protected final void generateServices() { 
    	arrServices = (T[]) Array.newInstance(this.serviceInterface, 2); 
    	arrServices[0]  = RestTestCaseBase.generateServiceClient(this.serviceInterface, ServiceBindingType.XML, this.serviceURL, this.servletRunner) ;
    	arrServices[1] = RestTestCaseBase.generateServiceClient(this.serviceInterface, ServiceBindingType.JSON, this.serviceURL, this.servletRunner) ;
    }//EOM 
    
    public final T[] getServices() { 
    	return this.arrServices ; 
    }//EOM 
    
}//EOC 
