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

package org.hyperic.hq.bizapp.client.shell;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.ejb.FinderException;
import javax.ejb.ObjectNotFoundException;

import org.hyperic.sigar.Sigar;

import org.apache.log4j.PropertyConfigurator;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.util.PropertyUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EarlyExitException;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InteractiveResponseBuilder;
import org.hyperic.util.config.InteractiveResponseBuilder_IOHandler;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.paramParser.ParserRetriever;
import org.hyperic.util.shell.ShellBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandHandler;
import org.hyperic.util.shell.ShellCommandInitException;
import org.hyperic.util.shell.ShellCommandUsageException;
import org.hyperic.util.shell.ShellSignalHandler;

/**
 * Provides a command-line interface to the server.
 */
public class ClientShell 
    extends ShellBase
    implements InteractiveResponseBuilder_IOHandler {

    public static final String PRODUCT = "HQ";
    public static final String PROMPT = "hq-shell";

    private static final String PROP_IS_DEVELOPER = "developer.commands";

    protected ClientShellAuthenticator  authenticator;
    protected ClientShellBossManager    bossManager;
    private ParserRetriever             parserRetriever;
    private PrintStream                 out;
    private PrintStream                 err;
    private Properties                  shellProperties;
    private HashMap                     hiddenCommands;
    private ClientShellUserSession      session;
    private InteractiveResponseBuilder  schemaBuilder;
    private BufferedReader in = null;
    
    public ClientShell(){
        this.authenticator   = null;
        this.bossManager     = null;
        this.out             = null;
        this.err             = null;
        this.session         = null;
        this.shellProperties = new Properties();
        this.hiddenCommands  = new HashMap();
        this.schemaBuilder   = new InteractiveResponseBuilder(this);
        this.in = new BufferedReader(new InputStreamReader(System.in));
    }

    public void init(String applicationName, PrintStream out, PrintStream err){
        super.init(applicationName, out, err);

        System.setProperty("log4j.logger.org.jnp.interfaces.NamingContext",
                           "ERROR");
        
        PropertyConfigurator.configure(System.getProperties());

        this.out = out;
        this.err = err;
    }

    public void registerCommands () throws ShellCommandInitException {
        ClientShell_login loginCommand = new ClientShell_login(this);
        ClientShellEntityFetcher entityFetcher;

        this.authenticator   = loginCommand;
        this.bossManager     = new ClientShellBossManager(this.authenticator);
        entityFetcher        = new ClientShellEntityFetcher(this.bossManager,
                                                         this.authenticator);
        this.parserRetriever = new ClientShellParserRetriever(entityFetcher);
            
        // Register command handlers
        registerCommandHandler("alert",        new ClientShell_alert(this));
        registerCommandHandler("cache",        new ClientShell_cache(this));
        registerCommandHandler("get",          new ClientShell_get(this));
        registerCommandHandler("login",        loginCommand);
        registerCommandHandler("metric",       new ClientShell_metric(this));
        registerCommandHandler("resource",     new ClientShell_resource(this));
        registerCommandHandler("set",          new ClientShell_set(this));
        registerCommandHandler("scheduler",    new ClientShell_scheduler(this));
        registerCommandHandler("time",         new ClientShell_time(this));
        registerCommandHandler("transfer",     new ClientShell_transfer(this));
        registerCommandHandler("trigger",      new ClientShell_trigger(this));
        registerCommandHandler("vacuum",       new ClientShell_vacuum(this));
        registerCommandHandler("version",      new ClientShell_version(this));
        registerCommandHandler("livedata",     new ClientShell_livedata(this));

        this.setHandlerHidden(".",            true);
        this.setHandlerHidden("exit",         true);
        this.setHandlerHidden("scheduler",    true);
        this.setHandlerHidden("transfer",     true);
        this.setHandlerHidden("trigger",      true);
        this.setHandlerHidden("vacuum",       true);
        this.setHandlerHidden("cache",        true);
        this.setHandlerHidden("livedata",     true);
    }

    public ParserRetriever getParserRetriever(){
        return this.parserRetriever;
    }

    public ClientShellBossManager getBossManager(){
        return this.bossManager;
    }

    public ClientShellAuthenticator getAuthenticator(){
        return this.authenticator;
    }

    public void processCommand(ShellCommandHandler handler, String[] args)
        throws ShellCommandUsageException, ShellCommandExecException
    {
        while(true){
            try {
                handler.processCommand(args);
            } catch(ShellCommandExecException exc){
                SessionTimeoutException stExc;
                SessionNotFoundException snfExc;
                ClientShellAuthenticationException authExc;
                AppdefEntityNotFoundException aenfExc;
                FinderException fExc;
                PermissionException pExc;

                if(this.isDeveloper()){
                    this.err.println(exc.toString());
                    exc.printStackTrace(this.err);
                }

                stExc = (SessionTimeoutException) 
                    exc.getExceptionOfType(SessionTimeoutException.class);
                snfExc = (SessionNotFoundException) 
                    exc.getExceptionOfType(SessionNotFoundException.class);
                authExc = (ClientShellAuthenticationException) 
                 exc.getExceptionOfType(ClientShellAuthenticationException.class);
                fExc = (FinderException)
                    exc.getExceptionOfType(ObjectNotFoundException.class);
                pExc = (PermissionException)
                    exc.getExceptionOfType(PermissionException.class);
                aenfExc = (AppdefEntityNotFoundException)
                   exc.getExceptionOfType(AppdefEntityNotFoundException.class);

                if(aenfExc != null){
                    int aTyp = aenfExc.getAppdefType();

                    this.out.println("Unable to find " +
                                     AppdefEntityConstants.typeToString(aTyp) +
                                     ": " + aenfExc.getMessage());
                } else if(fExc != null) { 
                    this.out.println("Error finding resource: " +
                                     fExc.getMessage());
                } else if(pExc != null) {
                    this.out.println("Permission denied: " +
                                     pExc.getMessage());
                } else if(authExc != null) {
                    this.out.println(authExc.getMessage());
                } else if(stExc != null || snfExc != null){
                    this.out.println("Session timed out.  Reauthenticating");
                    try {
                        this.authenticator.authenticate();
                    } catch(ClientShellAuthenticationException aExc){
                        String msg = "Failed to reauthenticate";
                        
                        throw new ShellCommandExecException(msg, aExc);
                    }
                    continue;
                } else {
                    throw exc;
                }
            }
            break;
        }
    }

    public ConfigResponse processConfigSchema(ConfigSchema schema)
        throws EncodingException, IOException, InvalidOptionException, 
               EarlyExitException 
    {
        return this.schemaBuilder.processConfigSchema(schema);
    }

    /**
     * Process a configuration schema.
     * @param schema The schema to process.
     * @return The filled-out ConfigResponse.
     */
    public ConfigResponse processConfigSchema(ConfigSchema schema,
                                              ConfigResponse defaults) 
        throws EncodingException, IOException, InvalidOptionException,
               EarlyExitException
    {
        return this.schemaBuilder.processConfigSchema(schema, defaults);
    }

    public void setHandlerHidden(String handlerName, boolean isHidden){
        if(this.getHandler(handlerName) == null){
            throw new IllegalArgumentException("Unknown handler: " +
                                               handlerName);
        }
        
        this.hiddenCommands.put(handlerName, 
                                isHidden ? Boolean.TRUE : Boolean.FALSE);

    }

    private boolean handlerIsHidden(String handlerName){
        Boolean hidden = (Boolean)this.hiddenCommands.get(handlerName);
        return hidden != null && hidden.booleanValue();
    }

    public Iterator getCommandNameIterator() {
        ArrayList keyArray;
        String[] keys;
        
        keyArray = new ArrayList();
        for(Iterator i = super.getCommandNameIterator(); i.hasNext(); ){
            String keyName = (String)i.next();
            
            if(!this.handlerIsHidden(keyName) || this.isDeveloper()){
                keyArray.add(keyName);
            }
        }

        keys = (String[])keyArray.toArray(new String[0]);
        Arrays.sort(keys);
        return Arrays.asList(keys).iterator();
    }

    public boolean isDeveloper(){
        return getProperty(PROP_IS_DEVELOPER) != null;
    }
    
    public ClientShellUserSession getSession() {
        if(this.session == null) {
            throw new IllegalStateException("User must be logged in in order to have a session");
        }
        return this.session;
    }
    
    public void setSession(ClientShellUserSession sess) {
        this.session = sess;
    }

    protected void loadProperties()
        throws IOException
    {
        String home = System.getProperty("user.home");
        File dir = new File(home, ".hq");
        File rcfile = new File(dir, "shellrc");
        File propsfile = new File(dir, "shell.properties");

        if (!dir.exists()) {
            dir.mkdir();
            getOutStream().println("Created " +
                                   dir.getAbsolutePath());
        }
        if (rcfile.createNewFile()) {
            getOutStream().println("Created " + 
                                   rcfile.getAbsolutePath());
        }
     
        readRCFile(rcfile, false);
        if (propsfile.createNewFile()) {
            getOutStream().println("Created " +
                                   propsfile.getAbsolutePath());
        }
            
        this.shellProperties = 
            PropertyUtil.loadProperties(propsfile.getAbsolutePath());
    }

    protected void storeProperties()
        throws IOException
    {
        String home = System.getProperty("user.home");
        File dir = new File(home, ".hq");
        File propsFile = new File(dir, "shell.properties");

        final String header = 
            "HQ Shell Properties (autogenerated - Do not edit!)";
        PropertyUtil.storeProperties(propsFile.getAbsolutePath(),
                                     this.shellProperties, header);
    }

    public void setProperty(String key, String value) {
        this.shellProperties.setProperty(key, value);
    }

    public String getProperty(String key) {
        String value = this.shellProperties.getProperty(key);
        if (value == null) {
            // Fall back to system property.
            value = System.getProperty(key);
        }
        return value;
    }

    public void removeProperty(String key) {
        this.shellProperties.remove(key);
    }

    public static void main ( String[] args ) {
        ClientShell shell = null;

        try {
            shell = new ClientShell();
            shell.init(PROMPT, System.out, System.err);
            shell.setPrompt(PROMPT + "::not-logged-in");
            shell.registerCommands();

            try {
                shell.loadProperties();
            } catch(IOException ex){
            }

            ShellSignalHandler.install(shell, "INT"); //catch ctrl-c
            shell.run();
        } catch ( Exception e ) {
            System.err.println("Unexpected exception: " + e);
            e.printStackTrace();
        } finally {
            try {
                shell.storeProperties();
            } catch (IOException e) {
                System.err.println("Unable to store shell properties: " + e);
            }
            shell.shutdown();
        }
    }

    public String getInput ( String prompt ) throws EOFException, IOException {
        return super.getInput(prompt, true);
    }

    /**
     * ConfigSchema responses do not get written to the command history.
     * @see org.hyperic.util.config.InteractiveResponseBuilder.IOHandler#handleInput
     */
    public String handleInput ( String prompt ) 
        throws EOFException, IOException 
    {
        this.out.print(prompt);
        //XXX should use getInput(prompt, false), but the massive sized
        //prompts w/ newlines and tabs and stuff are breaking/confusing Getline.
        return this.in.readLine();
    }

    /**
     * @see org.hyperic.util.config.InteractiveResponseBuilder.IOHandler#handleHiddenInput
     */
    public String handleHiddenInput ( String prompt ) 
        throws EOFException, IOException 
    {
        return Sigar.getPassword(prompt);
    }

    /** @see org.hyperic.util.config.InteractiveResponseBuilder.IOHandler#errOutput */
    public void errOutput ( String msg ) {
        sendToErrStream(msg);
    }

    /**
     * Parse a String containing one or more integers delimited by
     * a comma.
     * @param string containing comma separated list of integers.
     * @return int array.
     * */
    public static int[] commaSepStrToIntArr(String csIntStr) {
        // If the string is null or empty return emtpy array.
        if (csIntStr == null ||csIntStr.length() == 0) {
            return new int[0];
        }
        
        ArrayList arrayList = new ArrayList();
        StringTokenizer st  = new StringTokenizer(csIntStr, ",");
        while (st.hasMoreTokens()) {
            arrayList.add(st.nextToken());
        }
        int[] csIntArr = new int[arrayList.size()];
        for (int i=0;i<arrayList.size();i++) {
            csIntArr[i] = Integer.parseInt((String)arrayList.get(i));
        }
        return csIntArr;
    }
}
