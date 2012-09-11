package org.hyperic.tools.dbmigrate;

public enum ValueHandlerType {
    
    STRING_NULL_CHAR_REPLACER { 
        public final String handleValue(final String value) {
            return value.replace('\0', '\n') ; 
        }//EOM
    };//EO inner class STRING_HANDLER
    
    public Object handleValue(final Object value) { return value ; }//EOM 
    public String handleValue(final String value) { return value ; }//EOM  
}//EOI ValueHandler


