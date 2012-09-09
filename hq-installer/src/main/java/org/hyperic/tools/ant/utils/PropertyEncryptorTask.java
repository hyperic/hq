package org.hyperic.tools.ant.utils;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.hyperic.util.security.MarkedStringEncryptor;
import org.hyperic.util.security.SecurityUtil;

public class PropertyEncryptorTask extends Task{

    private String propName ; 
    private String encKey ; 
    private String destPropName ; 
    
    public final void setName(final String propName) { 
        this.propName = propName ; 
    }//EOM 
    
    public final void setEncKey(final String encKey) { 
        this.encKey = encKey ; 
    }//EOM 
    
    public final void setDestProperty(final String destPropName) { 
        this.destPropName = destPropName ; 
    }//EOM 
    
    @Override
    public final void execute() throws BuildException {
        final Project project = this.getProject() ; 
        final MarkedStringEncryptor encryptor = new MarkedStringEncryptor(SecurityUtil.DEFAULT_ENCRYPTION_ALGORITHM, this.encKey) ;
        final String origValue = project.getProperty(this.propName) ;
        if(origValue != null) { 
            final String manipualtedValue = this.manipulateValue(encryptor, origValue) ; 
            project.setUserProperty((this.destPropName == null ? this.propName : this.destPropName), manipualtedValue) ; 
        }//EO if there was a value 
    }//EOM 
    
    protected String manipulateValue(final MarkedStringEncryptor encryptor, final String origValue) { 
        return encryptor.encrypt(origValue)  ;  
    }//EOM 
    
    
    public static final class PropertyDecryptorTask extends PropertyEncryptorTask {
        @Override
        protected String manipulateValue(MarkedStringEncryptor encryptor, String origValue) {
            return encryptor.decrypt(origValue)  ;
        }//EOM 
    }//EOM 
    
}//EOC