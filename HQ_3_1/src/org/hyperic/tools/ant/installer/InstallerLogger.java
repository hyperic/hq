/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
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

package org.hyperic.tools.ant.installer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.Project;

import org.hyperic.util.JDK;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.EarlyExitException;
import org.hyperic.util.file.FileUtil;
import org.hyperic.util.file.WritableFile;
import org.hyperic.tools.ant.BasicLogger;

public class InstallerLogger implements BuildLogger {

    private static final String LINE_SEP =
        System.getProperty("line.separator");
    
    public static final String PROP_LOGFILE = "install.log";
    public static final String PROP_NOWRAP  = "install.nowrap";
    
    public static final String PREFIX = "^^^";
    public static final String[] MESSAGE_HANDLERS
        = { "org.hyperic.tools.ant.installer.MsgInfo",
            "org.hyperic.tools.ant.installer.MsgError",
            "org.hyperic.tools.ant.installer.MsgCompletion",
            "org.hyperic.tools.ant.installer.MsgInput",
            "org.hyperic.tools.ant.installer.MsgProgress" };
    public static final String DEBUG_HANDLER 
        = "org.hyperic.tools.ant.installer.MsgDebug";
    
    protected Map messageHandlers = null;
    
    /** PrintStream to write non-error messages to */
    protected PrintStream out;
    /** PrintStream to write error messages to */
    protected PrintStream err;
    
    protected InstallerMessageHandler currentHandler = null;
    protected Project project = null;
    protected WritableFile logfile = null;
    protected FileWriter logfileStream = null;
    
    /** Time of the start of the build */
    private long startTime = System.currentTimeMillis();
    private boolean isNoWrapMode;
    
    public InstallerLogger () {
        // Initialize -nowrap to true on windows, false otherwise
        if (JDK.IS_WIN32) {
            isNoWrapMode = true;
        } else {
            isNoWrapMode = false;
        }
    }
    
    protected void registerMessageHandler ( String msgWriterClass ) {
        InstallerMessageHandler handler;
        
        try {
            handler = (InstallerMessageHandler)
                Class.forName(msgWriterClass).newInstance();
            handler.setLogger(this);
            messageHandlers.put(generatePrefix(handler.getPrefix()), handler);
            
        } catch ( Exception e ) {
            err.println("ERROR registering message handler: " + e);
        }
    }
    
    /**
     * @param prefix The prefix of a specific message handler
     * @return The prefix that will be matched on - this is just the
     * prefix that is passed in, with the global PREFIX prepended, and a
     * colon (:) appended.
     */
    public String generatePrefix ( String prefix ) {
        return PREFIX + prefix + ":";
    }
    
    protected void initMessageHandlers () {
        if ( messageHandlers != null ) return;
        
        messageHandlers = new HashMap();
        for ( int i=0; i<MESSAGE_HANDLERS.length; i++ ) {
            registerMessageHandler(MESSAGE_HANDLERS[i]);
        }
        
        String nowrap = getProperty(PROP_NOWRAP);
        if ( nowrap != null ) {
            if ( Boolean.valueOf(nowrap).booleanValue() ) {
                isNoWrapMode = true;
            } else {
                //Override windows default of true if the option is
                // given on the command line.
                isNoWrapMode = false;
            }
        }


        registerMessageHandler(DEBUG_HANDLER);
        BasicLogger basicLogger = new BasicLogger();

        // Make sure raw log is in the same dir as regular log
        logfile = new WritableFile(logfile.getParentFile(),
                                   logfile.getName() + ".debug");
        basicLogger.setFile(logfile);
        basicLogger.setLevel("debug");
        basicLogger.register(project);

        // For debugging, uncomment the line below to
        // see all registered message handlers.
        // dumpHandlers();
        
        // Init the input handler here too
        if ( project != null ) {
            project.setInputHandler(new InstallerInputHandler(this));
        } else {
            err.println("Error setting input handler: project is null!");
        }
    }
    
    /** Does nothing - this logger doesn't care about levels */
    public void setMessageOutputLevel(int level) {}
    
    /** Does nothing - this logger doesn't care about emacs mode */
    public void setEmacsMode(boolean emacsMode) {}
    
    public void setOutputPrintStream(PrintStream output) {
        this.out = new PrintStream(output, true);
    }
    public void setErrorPrintStream(PrintStream err) {
        this.err = new PrintStream(err, true);
    }
    
    /** Initializes the logfile */
    public void buildStarted(BuildEvent event) {
        
        startTime = System.currentTimeMillis();
        
        initLogging(event);
    }
    
    private void initLogging (BuildEvent event) {
        
        if ( project == null ) project = event.getProject();
        
        if ( project != null && logfile == null ) {
            
            String logfileName;
            
            // check a few places for the logfile property
            logfileName = getProperty(PROP_LOGFILE);
            if ( logfileName == null ) {
                // Not found, use hq-install.log.
                logfileName = "hq-install.log";
            }

            File originalFile = new File(logfileName);
            File originalDir = originalFile.getParentFile();
            logfile = FileUtil.findWritableFile(new File("."),
                                                logfileName,
                                                null,
                                                "HQ_tmp");

            if (logfile == null) {
                err.println(nowrap("WARNING: file is not writeable: " +
                                   logfileName +
                                   "\nINSTALL LOG WILL NOT BE SAVED TO DISK."));
                return;
            }

            String parentAbsPath = logfile.getParentFile().getAbsolutePath();
            if ( !logfile.getOriginalLocationWasUsed() ) {
                err.println(nowrap("WARNING: file was not writeable: " 
                            + originalFile.getAbsolutePath() 
                            + "\nLogs will be written to " 
                            + parentAbsPath
                            + " instead."));
            }
            if (!logfile.mkdirs()) {
                err.println(nowrap("ERROR: could not create log directory: " 
                            + parentAbsPath
                            + "\nINSTALL LOG WILL NOT BE SAVED TO DISK."));
                return;
            }

            try {
                logfileStream = new FileWriter(logfile.getAbsolutePath(), true);
            } catch ( IOException ioe ) {
                err.println("ERROR: could not open install.log: "
                            + logfileName + ": " + ioe);
                ioe.printStackTrace(err);
            }
            printStartMessageToLog();
        }
    }
    
    /** Closes the logfile */
    public void buildFinished(BuildEvent event) {
        
        // just in case this is an early bailout
        // for example, if cam-setup.xml is malformed...
        initMessageHandlers();
        
        handleMessage(event);
        if ( event.getException() != null ) {
            Throwable t = event.getException();
            String errMsg = t.getMessage();
            String errPrefix = generatePrefix((new MsgError()).getPrefix());
            String infoPrefix = generatePrefix((new MsgInfo()).getPrefix());
            if ( errMsg != null && !errMsg.trim().startsWith(errPrefix) ) {
                String location = "";
                boolean isEarlyExit = false;
                if ( t instanceof BuildException ) {
                    isEarlyExit = (t.getCause() instanceof EarlyExitException);
                    if ( !isEarlyExit ) {
                        location = "at "
                            + ((BuildException) t).getLocation() 
                            + ": ";
                    }
                }
                if ( isEarlyExit ) {
                    handleMessage(infoPrefix + t.getCause().getMessage());
                } else {
                    handleMessage(errPrefix
                                  + "FATAL EXCEPTION " + location
                                  + errMsg);
                }
            }
        }
        if ( currentHandler != null ) {
            currentHandler.endMessage();
        }
        if ( logfileStream != null ) {
            try {
                logfileStream.close();
            } catch ( IOException ioe ) {
                err.println("ERROR: could not close install.log: "
                            + logfile.getAbsolutePath() + ": " + ioe);
                ioe.printStackTrace(err);
            }
        }
    }
    
    /**
     * Determine the message type for a given message.
     * @param msg The message to examine
     * @param msgbuf The buffer to use to store the actual message, with the 
     * prefix stripped.
     * @return The type of the message, one of the MSGTYPE_XXX constants.
     */
    protected InstallerMessageHandler getMessageHandler (String msg, 
                                                         StringBuffer msgbuf) {
        // ignore null messages
        if ( msg == null ) return null;
        
        msg = msg.trim();
        // ignore empty messages
        if ( msg.length() == 0 ) return null;
        
        int prefixPos = msg.indexOf(PREFIX);
        // ignore messages missing the global prefix
        if ( prefixPos == -1 ) return null;
        
        int colonPos = msg.indexOf(":");
        // ignore messages missing a colon
        if ( colonPos == -1 ) return null;
        
        // err.println("---> checking out: " + msg);
        String prefix = msg.substring(0, colonPos+1).trim();
        InstallerMessageHandler handler
            = (InstallerMessageHandler) messageHandlers.get(prefix);
        if ( handler == null ) {
            return null;
        }
        
        if ( msgbuf != null ) {
            String realMessage
                = msg.substring(prefix.length()).trim();
            if ( realMessage.startsWith("\\") ) {
                realMessage = realMessage.substring(1);
            }
            msgbuf.append(realMessage);
        }
        
        return handler;
    }
    
    public void targetStarted(BuildEvent event) {
        initLogging(event);
        initMessageHandlers();
        handleMessage(event);
    }
    public void targetFinished(BuildEvent event) {
        handleMessage(event);
    }
    public void taskStarted(BuildEvent event) {
        handleMessage(event);
    }
    public void taskFinished(BuildEvent event) {
        handleMessage(event);
    }
    
    public void messageLogged(BuildEvent event) {
        handleMessage(event);
    }
    
    public void handleMessage(BuildEvent event) {
        handleMessage(event.getMessage());
    }
    
    public void handleMessage(String message) {

        // If we're in win32 installer mode, remove all internal line breaks.
        message = nowrap(message);
        
        StringBuffer msgbuf = new StringBuffer();
        InstallerMessageHandler handler;
        handler = getMessageHandler(message, msgbuf);
        String msg = msgbuf.toString();
        
        // don't do anything with empty messages
        if ( msg.trim().length() == 0 ) return;

        if ( handler == null ) {
            // do not output unclassified messages
            // and do not update currentMessageType
            // err.println("no handler for: " + msg);
            handler = currentHandler;
        }
        
        if ( currentHandler == handler ) {
            handler.continueMessage(msg);
            
        } else {
            if ( currentHandler != null ) currentHandler.endMessage();
            handler.beginMessage(msg);
            currentHandler = handler;
        }
    }
    
    /**
     * Each message handler will callback to this method to perform actual
     * writes.
     */
    protected void printMessage ( String message ) {
        out.println(message);
        logToFile(message);
    }
    
    protected void logToFile ( String message ) {
        if ( logfileStream != null ) {
            try {
                logfileStream.write(message);
                logfileStream.write(LINE_SEP);
                logfileStream.flush();
            } catch ( IOException ioe ) {
                err.println("ERROR: could not write to log: " + ioe);
            }
        }
    }
    
    protected void printStartMessageToLog () {
        logToFile("====================================="
                  + "====================================");
        logToFile("Installer Started");
        logToFile("Current Date/Time: " + (new java.util.Date()).toString());
        logToFile("====================================="
                  + "====================================");
    }
    
    protected void printEndMessageToLog () {
        long duration = System.currentTimeMillis() - startTime;
        logToFile("====================================="
                  + "====================================");
        logToFile("Installer Completed");
        logToFile("Total Runtime: " + StringUtil.formatDuration(duration));
        logToFile("====================================="
                  + "====================================");
        logToFile("");
        logToFile("");
    }
    
    private String getProperty (String name) {
        String value = null;
        value = project.getProperty(name);
        if ( value == null ) {
            value = project.getUserProperty(name);
            if ( value == null ) {
                value = System.getProperty(name);
            }
        }
        return value;
    }

    private String nowrap(String msg) {
        if (msg == null) return null;
        if (isNoWrapMode) {
            String ns = msg;
            ns = StringUtil.replace(ns, "\n", "");
            ns = StringUtil.replace(ns, "\r", "");
            msg = ns;
        }
        return msg;
    }
}
