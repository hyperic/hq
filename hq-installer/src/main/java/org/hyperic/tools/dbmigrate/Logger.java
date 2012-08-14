package org.hyperic.tools.dbmigrate;

import java.io.PrintStream;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.listener.AnsiColorLogger;
import org.hyperic.tools.ant.BasicLogger;
import org.hyperic.tools.ant.installer.InstallerLogger;

public class Logger extends InstallerLogger{

    private HQAnsiColorLogger delegateConsoleLogger ;
    private DefaultLogger delegateStandardFileLogger ; 
    private int outputLevel ; 

    public Logger() { 
        super() ;
        this.delegateConsoleLogger = new HQAnsiColorLogger() ; 
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
    
    private class HQAnsiColorLogger extends AnsiColorLogger { 
        
        public void printMessage(String message) {
            super.printMessage(message, this.out, Project.MSG_WARN);
        }//EOM 
        
    }//EO inner class HQAnsiColorLogger
    
   /* private class ProxyFilesLogger implements BuildLogger{
        
        private List<BuildLogger> delegates = new ArrayList<BuildLogger>() ;

        public final void buildStarted(BuildEvent event) {
            for(BuildLogger delegate : this.delegates) { 
                delegate.buildStarted(event) ; 
            }//EO while there are more logger delegates 
        }//EOM 

        public final void buildFinished(final BuildEvent event) {
            for(BuildLogger delegate : this.delegates) { 
                delegate.buildFinished(event) ; 
            }//EO while there are more logger delegates 
        }//EOM

        public final void targetStarted(final BuildEvent event) {
            for(BuildLogger delegate : this.delegates) { 
                delegate.targetStarted(event) ; 
            }//EO while there are more logger delegates 
        }//EOM

        public final void targetFinished(final BuildEvent event) {
            for(BuildLogger delegate : this.delegates) { 
                delegate.targetFinished(event) ; 
            }//EO while there are more logger delegates 
        }//EOM

        public final void taskStarted(final BuildEvent event) {
            for(BuildLogger delegate : this.delegates) { 
                delegate.taskStarted(event) ; 
            }//EO while there are more logger delegates 
        }//EOM

        public final void taskFinished(final BuildEvent event) {
            for(BuildLogger delegate : this.delegates) { 
                delegate.taskFinished(event) ; 
            }//EO while there are more logger delegates 
        }//EOM

        public final void messageLogged(final BuildEvent event) {
            for(BuildLogger delegate : this.delegates) { 
                delegate.messageLogged(event) ; 
            }//EO while there are more logger delegates 
        }//EOM

        public final void setMessageOutputLevel(final int level) { throw new UnsupportedOperationException() ; }//EOM 
        public final void setOutputPrintStream(final PrintStream output) { throw new UnsupportedOperationException() ; }//EOM
        public final void setEmacsMode(final boolean emacsMode) { throw new UnsupportedOperationException() ; }//EOM
        public final void setErrorPrintStream(final PrintStream err) { throw new UnsupportedOperationException() ; }//EOM
        
    }//EO class ProxyFilesLogger 
    */
    
}//EOC 
