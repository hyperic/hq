package org.hyperic.hq.api.model;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class RestApiConstants {
	
	private static Map<String,String> errorCodes ;
	
	static { 
		
		try{ 
			errorCodes = new HashMap<String,String>() ; 

			final String ERROR_CODE_PREFIX = "ERROR_" ; 
			final int prefixLength = ERROR_CODE_PREFIX.length() ; 
			
			final Field[] fields = RestApiConstants.class.getDeclaredFields() ;
			String fieldName ;
			for(Field field : fields) { 
				
				fieldName = field.getName() ; 
				if(fieldName.startsWith(ERROR_CODE_PREFIX)) { 
					errorCodes.put(fieldName.substring(prefixLength, fieldName.length()), (String)field.get(null)) ; 
				}//EO if an error code constant 
				
			}//EO while there are more fields 
			
		}catch(Throwable t)  {
			t.printStackTrace() ; 
		}//EO catch block 
		
	}//EO static block  
	
    private RestApiConstants() {
    }
    
    public static final String SCHEMA_NAMESPACE = "http://vmware.com/hyperic/hq/api/rest/v1";
    
    
    //ERROR CODES 
    public static final String ERROR_MISSING_ID = "1001" ; 
    
    
    public final String getErrorCode(final String definiition) { 
    	return errorCodes.get(definiition) ; 
    }//EOM 
    
}
