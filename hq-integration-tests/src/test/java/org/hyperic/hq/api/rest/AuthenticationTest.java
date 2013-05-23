package org.hyperic.hq.api.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.hyperic.hq.api.model.ResourceModel;
import org.hyperic.hq.api.model.Resources;
import org.hyperic.hq.api.model.resources.ResourceBatchResponse;
import org.hyperic.hq.api.resources.ResourceServiceTest;
import org.hyperic.hq.api.rest.AbstractRestTestDataPopulator.RestTestData;
import org.hyperic.hq.api.rest.RestTestCaseBase.ServiceBindingsIteration;
import org.hyperic.hq.api.services.ResourceService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.springframework.test.annotation.DirtiesContext;


@DirtiesContext
@ServiceBindingsIteration(AuthenticationTest.SERVICE_URL)
@RestTestData(serviceURL=AuthenticationTest.SERVICE_URL, serviceInterface=ResourceService.class) 
@org.hyperic.hq.api.rest.RestTestCaseBase.SecurityInfo(ignore=true)
public class AuthenticationTest extends RestTestCaseBase<ResourceService, EmptyRestTestDataPopulator<ResourceService>> {
    
    public static final String SERVICE_URL = ResourceServiceTest.CONTEXT_URL + "/rest/resource"  ;  
    
    @Rule 
    public RuleChain interceptorsChain = super.interceptorsChain ;     
    
    @SecurityInfo(ignore=true)
    @Test(expected=WebApplicationException.class)
    public final void testNoCredentials() throws Throwable{
        this.updateEmptyResources(new int[]{}) ;            
    }    
    
    @SecurityInfo(username="hqadmin",password="wrong_password")
    @Test(expected=WebApplicationException.class)
    public final void testWrongPassword() throws Throwable{ 
        this.updateEmptyResources(new int[]{}) ;
    }      
    
    @SecurityInfo(username="wrong_user",password="hqadmin")
    @Test(expected=WebApplicationException.class)
    public final void testWrongUsername() throws Throwable{ 
        this.updateEmptyResources(new int[]{}) ;
    }    
    
    @SecurityInfo(username="hqadmin",password="hqadmin")
    @Test
    public final void testAuthenticatedUser() throws Throwable{
        this.updateEmptyResources(new int[]{}) ;
    }      
    

    

    



    private void updateEmptyResources(int[] is) throws Throwable {        
        List<ResourceModel> resourceList = new ArrayList<ResourceModel>(0);
        final Resources resources = new Resources(resourceList );        
        ResourceBatchResponse response = null ;
        try{ 
            response = service.updateResources(resources) ;            
        }catch(Throwable t){ 
            t.printStackTrace() ; 
            throw t ; 
        }
    }    
}
