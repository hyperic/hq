package org.hyperic.tools.dbmigrate;

import java.io.PrintStream;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.listener.AnsiColorLogger;
import org.hyperic.tools.ant.BasicLogger;
import org.hyperic.tools.ant.installer.InstallerLogger;

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
    
   public static void main(String[] args) throws Throwable {
       System.out.println(System.getProperty("os.name") ) ;        
   }//EOM 
   
    
}//EOC 
