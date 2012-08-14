package org.hyperic.tools.ant.utils;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Input;

public class PauseTask extends Input{
    private String prompt = ", Press Return key to continue..." ;
    private String message ; 
    private boolean isQuiteMode ; 
    
    public final void setPrompt(final String prompt) { 
        this.prompt = prompt ; 
    }//EOM 
    
    public final void setQuite(final boolean isQuiteMode) { 
        this.isQuiteMode = isQuiteMode ; 
    }//EOM 
    
    @Override 
    public final void setMessage(final String message) {
        this.message = message ;
        super.setMessage(message) ; 
    }//EOM 
    
    @Override
    public final void execute() throws BuildException { 
        if(this.isQuiteMode) { 
            if(message != null) this.log(message) ; 
        }else { 
            if(message != null) this.setMessage(this.message + prompt) ; 
            super.execute() ; 
        }//EO else if not quite mode 
    }//EOM 
    
}//EOC 
