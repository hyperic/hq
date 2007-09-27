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

import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

import org.hyperic.hq.bizapp.shared.AuthBoss;

import java.io.IOException;
import java.util.Hashtable;
import java.util.StringTokenizer;

public class ClientShell_login 
    extends ShellCommandBase 
    implements ClientShellAuthenticator
{
    private int          authToken; // Authentication token
    private Hashtable    namingEnv; // Naming provider environment
    private UserInfo     userInfo;  // userInfo given by 'login' command
    private ClientShell     shell;      

    public ClientShell_login(ClientShell shell){
        this.resetConnection();
        this.userInfo  = null;
        this.shell     = shell;
    }

    private void resetConnection(){
        this.authToken = 0;
        this.namingEnv = null;
    }

    private class UserInfo {
        String user, pword, server;
        int port;
    }

    private UserInfo parse(String str){
        StringTokenizer mainTok, userTok, serverTok;
        String port, userStr, serverStr;
        UserInfo res = new UserInfo();
        
        mainTok = new StringTokenizer(str, "@");
        if(mainTok.countTokens() != 2)
            throw new IllegalArgumentException();
        
        userStr = mainTok.nextToken();
        if(userStr.startsWith(":") || userStr.endsWith(":"))
            throw new IllegalArgumentException();
        
        userTok = new StringTokenizer(userStr, ":");
        if(userTok.countTokens() < 1)
            throw new IllegalArgumentException();
        
        res.user  = userTok.nextToken();
        res.pword = (userTok.countTokens() == 1 ? 
                     userTok.nextToken() : null);
        
        serverStr = mainTok.nextToken();
        if(serverStr.startsWith(":") || serverStr.endsWith(":"))
            throw new IllegalArgumentException();
        
        serverTok = new StringTokenizer(serverStr, ":");
        if(serverTok.countTokens() < 1)
            throw new IllegalArgumentException();
        
        res.server = serverTok.nextToken();
        port = (serverTok.countTokens() == 1 ? 
                serverTok.nextToken() : null);
        
        if(port != null){
            try {
                res.port = Integer.parseInt(port);
            } catch(NumberFormatException exc){
                throw new IllegalArgumentException();
            }
        } else {
            // new jnp port is non-default
            res.port = 2099;
        }
        return res;
    }

    public void processCommand(String[] args) 
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        UserInfo userInfo;
        String pword, syntax = "Syntax: user[:pword]@server[:port]";

        if(args.length != 1){
            throw new ShellCommandUsageException(syntax);
        }

        try {
            userInfo = this.parse(args[0]);
        } catch(IllegalArgumentException exc){
            throw new ShellCommandUsageException(syntax);
        }
        
        if(userInfo.pword == null){
            try {
                userInfo.pword = this.getShell().getHiddenInput("Password: ");
            } catch(IOException exc){
                throw new ShellCommandExecException("Failed to read password",
                                                    exc);
            }
        } 

        this.userInfo = userInfo;
        
        try {
            this.authenticate();
        } catch(ClientShellAuthenticationException exc){
            throw new ShellCommandExecException("Failed to authenticate", exc);
        }
    }

    public String getSyntaxArgs(){
        return "<username[:password]@server[:port]>";
    }

    public String getUsageShort(){
        return "Login to the " + ClientShell.PRODUCT + " system";
    }

    public String getUsageHelp(String args[]){
        return "    " + this.getUsageShort() + ".  This must be performed " +
            "before any commands\n" +
            "    can be invoked on the server.";
    }

    /*** 
     * The following routines function for the ClientShellAuthenticator interface
     ***/

    public int getAuthToken(){
        return this.authToken;
    }

    public Hashtable getNamingEnv() 
        throws ClientShellAuthenticationException
    {
        if(this.namingEnv != null)
            return this.namingEnv;

        throw new ClientShellAuthenticationException("You must use the '" +
                         this.getCommandName() + "' command before " +
                         "attempting this operation");
    }

    public void authenticate() 
        throws ClientShellAuthenticationException
    {
        int newToken = 0;

        this.resetConnection();

        if(this.userInfo == null){
            throw new ClientShellAuthenticationException("Authentication failed."+
                                                      "  Please 'login'");
        }

        this.namingEnv = ClientShell_login.createNamingEnv(this.userInfo.server, 
                                                        this.userInfo.port);

        try {
            AuthBoss authBoss;

            /* This is kinda kludgy -- setup the internal namingEnv, since
               getting the AuthBoss will call back into this object in order
               to get the naming env (to find the auth boss) */
            this.shell.getBossManager().resetBosses();
            authBoss = this.shell.getBossManager().getAuthBoss();
            newToken = authBoss.login(this.userInfo.user, 
                                      this.userInfo.pword);
        } catch(Exception exc){
            this.resetConnection();  // Set namingEnv back to null
            throw new ClientShellAuthenticationException("Authentication failed",
                                                      exc);
        }

        // set the prompt
        this.getShell().setPrompt(ClientShell.PROMPT + "::" +
                                  this.userInfo.user);
        
        this.getOutStream().println("Successfully logged in as " + 
                                    this.userInfo.user + " at " +
                                    this.userInfo.server + ":" + 
                                    (this.userInfo.port == 0 ? "default" :
                                     Integer.toString(this.userInfo.port)));

        if(this.shell.isDeveloper()){
            this.getOutStream().println("New auth token = " + newToken);
        }
        this.shell.setSession(new ClientShellUserSession(this.userInfo.server,
                                                      String.valueOf(this.userInfo.port),
                                                      this.userInfo.user,
                                                      this.userInfo.pword));

        this.authToken = newToken;
    }

    private static Hashtable createNamingEnv(String server, int port){
        Hashtable env = new Hashtable();
        String serverString;

        serverString = server + (port != 0 ? ":" + port : "");

        // now make the naming env
        // NOTE THIS IS JBOSS SPECIFIC
        env.put("java.naming.factory.initial", 
                "org.jnp.interfaces.NamingContextFactory");
        env.put("java.naming.provider.url", "jnp://" + serverString);
        env.put("java.naming.factory.url.pkgs", 
                "org.jboss.naming:org.jnp.interfaces");
        return env;
    }
}
