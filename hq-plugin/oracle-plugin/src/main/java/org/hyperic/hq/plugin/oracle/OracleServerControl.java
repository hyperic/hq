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

package org.hyperic.hq.plugin.oracle;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.pool.OracleDataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.ArrayUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.ExecuteWatchdog;
import org.hyperic.util.exec.PumpStreamHandler;
import org.hyperic.util.jdbc.DBUtil;




public class OracleServerControl extends  ControlPlugin{
    private transient Log log =  LogFactory.getLog(OracleServerControl.class);
    private static final String _logCtx = OracleServerControl.class.getName();    
    private static final String STARTUP_SCRIPT = "startup.sql";
    private static final String SHUTDOWN_SCRIPT = "shutdown.sql";
    private String startupScript = null; 
    private String shutdownScript = null;
    private static final String SCRIPT_PATH = "/pdk/work/scripts/oracle";
 
    private static final String INSUFFICIENT_PRIVILEGES_ERROR = "ORA-01031: insufficient privileges";
    
    private String getScriptFullPath(String scriptName, File dirName ) throws PluginException {
        File script = new File(dirName, scriptName);
        String scriptStr = null;
        log.debug("[configure] dirName =" + dirName.getAbsolutePath());
        log.debug("[configure] script =" + script.getAbsolutePath());
        if (script.exists()) {
            try {
                scriptStr = script.getCanonicalPath();
            } catch (IOException ex) {
                scriptStr = script.getAbsolutePath();
            }
        } else {
            throw new PluginException("startup script '" + script + "' NOT FOUND");
        }
        return scriptStr;

    }
    
    @Override
    public void configure(ConfigResponse config) throws PluginException {        
        super.configure(config);
        log.debug("[configure] config=" + config);
        setTimeout(180);
        File dirName = new File(System.getProperty(AgentConfig.AGENT_BUNDLE_HOME) + SCRIPT_PATH);
        log.debug("[configure] dirName =" + dirName.getAbsolutePath());
        startupScript = getScriptFullPath(STARTUP_SCRIPT, dirName);
        shutdownScript = getScriptFullPath(SHUTDOWN_SCRIPT, dirName);
        if (!isOracleConnected(false, config.getValue(OracleMeasurementPlugin.PROP_URL), false) ) {
            throw new PluginException("oracle not connected !!");
        }
    }

    @Override
    public List getActions() {        
        return Arrays.asList("start", "stop", "restart");
    }
    
    private String findSid(String url) {
        int index = url.lastIndexOf(':');
        int slashIndex = url.lastIndexOf('/');
        if (slashIndex > index) {
            // we have a service name and not a sid
            return null;
        }
        String sid = null;
        if ( index >-1 && index < url.length()-1) {
            // must have something after the ":"
            sid = url.substring(index+1);
        }
        log.debug("findSid in url=" + url + " found sid=<" + sid+">");
        return sid;
    }
      /*
     * extract oracle home from env
     */
    private String getOracleHomeFromEnv(List envVector) throws PluginException {
        if (envVector == null) {
            throw new PluginException("getOracleHomeFromEnv - null env ");
        }
        String oracleHome = null;
        for (Object val:envVector) {
            String str = (String)val;
            if (str.startsWith("ORACLE_HOME=")) {
                oracleHome = str.substring("ORACLE_HOME=".length());
                break;
            }
        }
        log.debug("oracleHome=" + oracleHome);
        if (oracleHome == null) {
            
            log.debug("didn't find ORACLE_HOME");
        }
        return oracleHome;
        
    }
    
    /*
     * assume we have oracle_home configured and that sqlplus is under ioracl_ehome/bin
     */
    private String getOracleCommandPath(String oracleCmd) throws PluginException {
        String cmd;
        String oracleHome = getOracleHomeFromEnv(Execute.getProcEnvironment());
        if (oracleHome != null) {
            File sqlCmdDir = new File(oracleHome, "bin");
            if (!sqlCmdDir.exists()) {
                throw new PluginException("couldn't find bin under:" + oracleHome);
            }
            File sqlCmdFile = new File(sqlCmdDir, oracleCmd);
            if (!sqlCmdFile.exists()) {
                throw new PluginException("couldn't find " + oracleCmd + " under:" + sqlCmdDir);
            }
            log.debug("[getOracleCommandPath] " + oracleCmd + "=" + sqlCmdFile);
            cmd = sqlCmdFile.getAbsolutePath();
        } else {
            log.debug("[getOracleCommandPath] didn't find oracle home - try to run just " + oracleCmd);
            cmd = oracleCmd;
        }
        return cmd;
    }
    
    private void updateSidInEnv(Execute ex) throws PluginException {
        String url = config.getValue(OracleMeasurementPlugin.PROP_URL);
        String sid = findSid(url);
        if (sid  != null) {                
            log.debug("updating sid=<" + sid + ">");
        }
        else {
            throw new PluginException("updateSidInEnv failed to find sid: + url=" + url);
        }

        String[] env = {"ORACLE_SID=" + sid};
        log.debug("env before upadte:" + ex.getEnvironment());
        env = (String[]) ArrayUtil.combine(env, ex.getEnvironment());

        ex.setEnvironment(env);
    }
    
    /*
     * sqlplus user/password@script as sysdba
     */
    private List<String> getSqlPlusParameters(String scriptName, String arg, String user) throws PluginException {
        List<String> cmd =  new ArrayList<String>(8);
           
        log.debug("script=" + scriptName);
        String fileName = "@" + scriptName;   

        String sqlCmd = getOracleCommandPath("sqlplus");
        String arg1   = "/";            
        String arg2 = "as";
        String arg3 = "sysdba";
        String arg4   = fileName;            


        if (user != null) {
            cmd.add("su");
            cmd.add("-");
            cmd.add(user);
            cmd.add("-c");
            // all the rest should be a single argument
            StringBuilder sb = new StringBuilder(sqlCmd.length()+fileName.length() + 15);
            sb.append(sqlCmd).append(" ").append(arg1).append(" ").append(arg2).append(" ").append(arg3).append(" ").append(arg4);
            if (arg != null) {
                sb.append(" ").append(arg);
            }
            cmd.add(sb.toString());
        }
        else {            
            cmd.add(sqlCmd);            
            cmd.add(arg1);
            cmd.add(arg2);
            cmd.add(arg3);
            cmd.add(arg4);
            if (arg != null) {
                cmd.add(arg);
            }           
        }
        return cmd;
    }
    
    /*
     * parse string search forline beginning with:  ORA-
     */
    private String getErrorString(String outputStr) {
        boolean found = false;
        String error = "";
        String[] lines = outputStr.split("\r?\n");
        for (String line:lines) {
            if (line.indexOf("ORA-") > -1 ) {
                error = line;
                found = true;
                break;
            }
        }
        if (!found) {
            error = outputStr;
        }                    
        return error;
    }
    
    
    
    private void sqlPlusAction(String script, String expectedOutput, String user, String parameter) throws PluginException {
        try {
            log.debug("script=" + script);
            List<String> cmd = getSqlPlusParameters(script, parameter, user);
            executeCommad(cmd, expectedOutput);
        } catch (Exception err) {
            throw new PluginException("sqlplus action failed:" + err.getMessage(), err);
        }
    }

    private void oradminAction(String action, String expectedOutput) throws PluginException {
        oradminAction(action, expectedOutput, new ArrayList<String>());
    }
    
    private void oradminAction(String action, String expectedOutput,List<String> extraArgs) throws PluginException {
        try {
            log.debug("[oradminAction] action=" + action);

            List<String> cmd = new ArrayList<String>();
            cmd.add(getOracleCommandPath("oradim"));
            cmd.add("-" + action);
            cmd.add("-SID");
            cmd.add(findSid(config.getValue(OracleMeasurementPlugin.PROP_URL)));
            cmd.addAll(extraArgs);
            executeCommad(cmd, expectedOutput);
        } catch (Exception err) {
            throw new PluginException("oradmin action failed:" + err.getMessage(), err);
        }
    }

    private void executeCommad(List<String> cmd, String expectedOutput) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        File pwd = new File(System.getProperty(AgentConfig.AGENT_BUNDLE_HOME));
        log.debug("[executeCommad] cmd=" + cmd);
        log.debug("[executeCommad] working dir=" + pwd);

        try {
            ExecuteWatchdog watchdog = new ExecuteWatchdog(getTimeoutMillis());
            Execute ex = new Execute(new PumpStreamHandler(output), watchdog);
            ex.setWorkingDirectory(pwd);
            ex.setCommandline((String[]) cmd.toArray(new String[0]));
            updateSidInEnv(ex);

            int exitCode = ex.execute();
            log.debug("[executeCommad] before getting output:" + exitCode);
            String outputStr = output.toString();
            log.debug("[executeCommad] after execute:" + outputStr);
            boolean success = true;

            String err = null;
            if (exitCode != 0) {
                err = getErrorString(outputStr);
            } else if ((expectedOutput != null) && !outputStr.contains(expectedOutput)) {
                err = getErrorString(outputStr);
            }

            if (watchdog.killedProcess()) {
                err = "Command did not complete within timeout of " + getTimeout() + " seconds";
            }
            
            if (err !=null) {
                getLog().debug("[executeCommad] error: "+err);
                setMessage(err);
                success = false;
            }

            if (!success) {
                throw new PluginException(getMessage());
            }
        } finally {
            try {
                output.close();
            } catch (IOException e) {
                log.error("failed to close output", e);
            }
        }
    }
   
    private void startOracle(String user) throws PluginException {
        if (isWin32()) {
            oradminAction("STARTUP", null);
        } else {
            startOracleUnix(user);
        }
        startListeners();
    }
    
    private void startOracleUnix(String user) throws PluginException {
        try {
            sqlPlusAction(startupScript, "ORACLE instance started.", null, null);
        } catch (PluginException ex) {
            String message = getMessage();
            if (user != null && message != null && message.indexOf(INSUFFICIENT_PRIVILEGES_ERROR) != -1) {
                log.debug("sqlPlusAction failed with " + message + " trying to run with user=" + user);
                sqlPlusAction(startupScript, "ORACLE instance started.", user, null);
            } else {
                throw ex;
            }
        }
    }

    private void  stopOracle(String[] args) throws PluginException {
        log.debug("args=" + args);           
        String mode = getModeParameter(args);
        try{
            if (isWin32()) {
                List<String> extraArgs = Arrays.asList(new String[]{"-SHUTMODE", mode});
                oradminAction("SHUTDOWN", null, extraArgs);
            } else {
                sqlPlusAction(shutdownScript, "ORACLE instance shut down.", null, mode);
            }
        } catch(PluginException ex) {
            if (isWin32()) {
               // user not supported for windows
               throw ex;
            }
            String user = getUser(args, 1);
            String message = getMessage();
            if ( user != null && message != null && message.indexOf(INSUFFICIENT_PRIVILEGES_ERROR) != -1 ) {
                 log.debug("sqlPlusAction failed with " + message + " trying to run with user=" + user);
                 sqlPlusAction(shutdownScript, "ORACLE instance shut down.", user, mode);
            }    
            else {
                 throw ex;
            }
        }
     }
 
          
        
    @Override
    public void doAction(String action, String[] args) throws PluginException {
          log.debug("[doAction] action=" + action);
          log.debug("[doAction] args=" + Arrays.asList(args));
          log.debug("[doAction] url=" + config.getValue(OracleMeasurementPlugin.PROP_URL));
          log.debug("[doAction] listeners=" + config.getValue(OracleMeasurementPlugin.PROP_LISTENERS));

          if (action.equals("start")) {
              log.debug("before start");
              startOracle(getUser(args, 0));            
          }
          else if (action.equals("stop")) {
              log.debug("before stop");              
              stopOracle(args);
          }          
          else if (action.equals("restart")) {
              log.debug("before restart");
              if (isRunning()) {
                  stopOracle(args);
                  waitForState(STATE_STOPPED);
              }              
              startOracle(getUser(args, 1));              
          }
          else { 
            throw new PluginException("Action '" + action + "' not supported");
          }   
          setResult(RESULT_SUCCESS);
          setMessage("OK");
    }
    
  
    
    private String getUser(String[] args, int index) {    
        if (args == null || args.length <= index) {
            return null;
        }
        if (args[index].length() > 0)
            return args[index];
        
        return null;
    }
    
    private String getModeParameter(String[] args) {
        log.debug("args=" + ((args!=null) ? Arrays.asList(args) : "null"));
        if (args == null || args.length < 1) {
            return "normal";
        }
        return args[0];
    }   

    

    @Override
    protected boolean isRunning() {
        OracleConnection conn = null ;        
        try {            
            String dbUrl = config.getValue(OracleMeasurementPlugin.PROP_URL);
            if (dbUrl == null) {
                throw new PluginException("no url is given");
            }
            boolean isConnected =  isOracleConnected(false, dbUrl, false);
            log.debug("isRunning returns:" +  isConnected);
            return isConnected;            
        }
        catch(PluginException e) {
            log.debug("isRunning - got exception: - returning false " + e.getMessage());
            log.debug("isRunning returns false");
            return false;
        }
        
    }
   
    
    private boolean isOracleConnected(boolean addPerlimAuth, String url, boolean asSysDba) throws PluginException {
        OracleDataSource ds;
        OracleConnection conn = null;
        try {
            String user = config.getValue(OracleMeasurementPlugin.PROP_USER);
            if (user == null) {
                log.debug("No value for config property " + OracleMeasurementPlugin.PROP_USER);
                throw new PluginException("No value for config property " + OracleMeasurementPlugin.PROP_USER);
            }
            String password = config.getValue(OracleMeasurementPlugin.PROP_PASSWORD);
            if (password == null) {
                log.debug("No value for config property " + OracleMeasurementPlugin.PROP_PASSWORD);
                throw new PluginException("No value for config property " + OracleMeasurementPlugin.PROP_PASSWORD);
            }
            log.debug("getOracleConnection user=<" + user +"> password=<"  + password + ">");
            
      
            ds = new OracleDataSource();
            Properties prop = new Properties();
            prop.setProperty("user", user);
            prop.setProperty("password", password);
                                    
            if (asSysDba) {
                prop.setProperty("internal_logon","sysdba");
            }
            if (addPerlimAuth) {
                prop.setProperty("prelim_auth","true");
            }
            ds.setConnectionProperties(prop);
            //String dbUrl =  "jdbc:oracle:thin:@//localhost:1521:nlayers";  
            ds.setURL(url);            
            log.debug("getOracleConnection before   ");
            conn = (OracleConnection)ds.getConnection();

            log.debug("getOracleConnection success");
            return true;
        }catch(SQLException e) {            
            throw new PluginException(e.getMessage(), e);            
        }
        finally {
            if (conn  != null) {
                DBUtil.closeConnection(_logCtx, conn);
            }
        }
    }

    private void startListeners() throws PluginException {
        List<String> listeners = Arrays.asList(config.getValue(OracleMeasurementPlugin.PROP_LISTENERS).split(","));
        if (listeners.isEmpty()) {
            listeners.add("");
        }

        for (String listener : listeners) {
            if (listenerIsDown(listener)) {
                lsnrctlCommand("start", listener);
            }
        }
    }

    private boolean listenerIsDown(String listener) {
        boolean down = false;
        try {
            lsnrctlCommand("status", listener);
        } catch (PluginException ex) {
            log.debug("[listenerIsDown] error=", ex);
            down = true;
        }

        log.debug("[listenerIsDown] listener '" + listener + "' down=" + down);
        return down;
    }

    private void lsnrctlCommand(String command, String listener) throws PluginException {
        List<String> cmd = new ArrayList<String>();
        cmd.add(getOracleCommandPath("lsnrctl"));
        cmd.add(command);
        if (listener.length() > 0) {
            cmd.add(listener);
        }
        executeCommad(cmd);
    }

    /**
     * Execute command a return the output if the return code is 0.
     *
     * @param cmd command and arguments
     * @return command output
     * @throws PluginException if the command return code is not 0
     */
    private String executeCommad(List<String> cmd) throws PluginException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        File pwd = new File(System.getProperty(AgentConfig.AGENT_BUNDLE_HOME));
        log.debug("[executeCommad] cmd=" + cmd);
        log.debug("[executeCommad] working dir=" + pwd);
        String outputStr;
        int exitCode;

        ExecuteWatchdog watchdog = new ExecuteWatchdog(getTimeoutMillis());

        try {
            Execute ex = new Execute(new PumpStreamHandler(output), watchdog);
            ex.setWorkingDirectory(pwd);
            ex.setCommandline((String[]) cmd.toArray(new String[0]));
            updateSidInEnv(ex);

            exitCode = ex.execute();
            log.debug("[executeCommad] exitCode:" + exitCode);
            outputStr = output.toString();
            log.debug("[executeCommad] outputStr:" + outputStr);
        } catch (Exception ex) {
            throw new PluginException(ex);
        } finally {
            try {
                output.close();
            } catch (IOException e) {
                log.debug("failed to close output", e);
            }
        }

        if (exitCode != 0) {
            throw new PluginException(outputStr);
        }

        if (watchdog.killedProcess()) {
            throw new PluginException("Command did not complete within timeout of " + getTimeout() + " seconds");
        }

        return outputStr;
    }
}
