/* **********************************************************************
/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2012], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.tools.ant.utils;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Input;

/**
 * Pauses the build and awaits for the user to presses on any key to continue<br/>
 * if the isQuietMode is set to true, the pause is skipped (nice to have to nohup/intetractive mode toggling)
 * 
 */
public class PauseTask extends Input{
    private String prompt = ", Press Return key to continue..." ;
    private String message ; 
    private boolean isQuietMode ; 
    
    /**
     * @param prompt custom user prompt to display while waiting 
     */
    public final void setPrompt(final String prompt) { 
        this.prompt = prompt ; 
    }//EOM 
    
    /**
     * @param isQuiteMode if true the task shall pause until the user presses on any key, else if false skips the pause 
     */
    public final void setQuiet(final boolean isQuiteMode) { 
        this.isQuietMode = isQuiteMode ; 
    }//EOM 
    
    @Override 
    public final void setMessage(final String message) {
        this.message = message ;
        super.setMessage(message) ; 
    }//EOM 
    
    @Override
    public final void execute() throws BuildException { 
        if(this.isQuietMode) { 
            if(message != null) this.log(message) ; 
        }else { 
            if(message != null) this.setMessage(this.message + prompt) ; 
            super.execute() ; 
        }//EO else if not quite mode 
    }//EOM 
    
}//EOC 
