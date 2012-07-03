package org.hyperic.hq.api.rest;

public class EmptyRestTestDataPopulator<T> extends AbstractRestTestDataPopulator<T>{

    public EmptyRestTestDataPopulator(){}//EOM 
    
    public EmptyRestTestDataPopulator(Class<T> serviceInterface, String serviceURL) {
        super(serviceInterface, serviceURL);        
    }//EOM 

    public void destroy() throws Exception {}//EOM

}
