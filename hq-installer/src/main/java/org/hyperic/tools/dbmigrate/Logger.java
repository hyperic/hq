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
package org.hyperic.tools.dbmigrate;

import java.io.PrintStream;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.listener.AnsiColorLogger;
import org.hyperic.tools.ant.BasicLogger;
import org.hyperic.tools.ant.installer.InstallerLogger;

/**
 * Proxy logger delegating to: 
 * - {@link PrintLoggerInterface} instance responsible for console output (OS dependent). 
 * - Tee file logger echoing the console output 
 * - Debug file logger logging in a an explicit debug level 
 */
public class Logger extends InstallerLogger{

    private PrintLoggerInterface delegateConsoleLogger ;
    private DefaultLogger delegateStandardFileLogger ; 
    private int outputLevel ; 

    public Logger() { 
        super() ;
        this.delegateConsoleLogger = (System.getProperty("os.name").indexOf("Win") == -1? new HQAnsiColorLogger() : new HQDefaultLogger()) ;  
    }//EOM
    
    @Override
    public final void setEmacsMode(final boolean emacsMode) {
        super.setEmacsMode(emacsMode);
        this.delegateConsoleLogger.setEmacsMode(emacsMode) ; 
    }//EOM 
    
    @Override
    public final void setErrorPrintStream(final PrintStream err) {
        super.setErrorPrintStream(err);
        this.delegateConsoleLogger.setErrorPrintStream(err) ; 
    }//EOM 
    
    @Override
    public final void setMessageOutputLevel(final int level) {
        this.outputLevel = level ;
        this.delegateConsoleLogger.setMessageOutputLevel(level) ; 
    }//EOM 
    
    @Override
    public final void setOutputPrintStream(final PrintStream output) {
        super.setOutputPrintStream(output);
        this.delegateConsoleLogger.setOutputPrintStream(output) ; 
    }//EOM 
    
    @Override
    public final void buildFinished(final BuildEvent event) {
        super.buildFinished(event);
        this.delegateConsoleLogger.buildFinished(event) ; 
    }//EOM 
    
    @Override
    public final void messageLogged(final BuildEvent event) {
        super.messageLogged(event);
    }//EOM 
    
    @Override
    protected final void printMessage(final String message) {
        this.logToFile(message) ;
    }//EOM 
    
    @Override
    protected final void logToFile(final String message) {
        super.logToFile(message) ; 
        this.delegateConsoleLogger.printMessage(message) ; 
    }//EOM
    
    @Override
    protected void initMessageHandlers() {
        if(messageHandlers != null) return ; 
        
        this.delegateStandardFileLogger = new DefaultLogger() { 
            public void messageLogged(BuildEvent event) {
                super.messageLogged(event) ;
            };
        };
        
        this.delegateStandardFileLogger.setOutputPrintStream(this.logfileStream) ; 
        this.delegateStandardFileLogger.setMessageOutputLevel(this.outputLevel) ;
        this.delegateStandardFileLogger.setErrorPrintStream(this.err) ; 
        this.delegateStandardFileLogger.setEmacsMode(false) ; 
        this.project.addBuildListener(this.delegateStandardFileLogger) ; 
        
        super.initMessageHandlers();
        
    }//EOM 
    
    @Override
    protected final BasicLogger newDelegateLogger() {
        
        return new BasicLogger() { 
            
            @Override
            public final void messageLogged(final BuildEvent event) {
                super.messageLogged(event);
                delegateConsoleLogger.messageLogged(event) ;
            }//EOM 
            
        };//EO BasicLogger
    }//EOM 
    
    private interface PrintLoggerInterface extends BuildLogger{ 
        void printMessage(String message) ; 
    }//EOM 
    
    private class HQAnsiColorLogger extends AnsiColorLogger implements PrintLoggerInterface{ 
        
        public void printMessage(String message) {
            super.printMessage(message, this.out, Project.MSG_WARN);
        }//EOM 
        
        
    }//EO inner class HQAnsiColorLogger
    
    private class HQDefaultLogger extends DefaultLogger implements PrintLoggerInterface{ 
    
       public void printMessage(String message) {
        super.printMessage(message, this.out, Project.MSG_WARN);
      }//EOM 
        
        
    }//EO inner class HQAnsiColorLogger
    
    
}//EOC 
