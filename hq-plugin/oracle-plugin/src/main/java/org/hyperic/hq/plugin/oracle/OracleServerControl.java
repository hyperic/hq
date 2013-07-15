package org.hyperic.hq.plugin.oracle;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Vector;


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
    private String getOracleHomeFromEnv(Vector envVector) throws PluginException {
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
    private String getSqlPlusPath() throws PluginException {
        String sqlCmd = "sqlplus";
        String oracleHome = getOracleHomeFromEnv(Execute.getProcEnvironment());
        if (oracleHome == null) {
            // return "sqlplus and hope for the best"
            log.info("didn't find oracle home - try to run just sqlplus");
            return "sqlplus";
        }
        File sqlPlusDir = new File(oracleHome, "bin");
        if (!sqlPlusDir.exists()) {
            throw new PluginException("couldn't find bin under:" +oracleHome);
        }
        File sqlPlusFile = new File(sqlPlusDir.getAbsolutePath(), sqlCmd); 
        if (!sqlPlusFile.exists()) {
            throw new PluginException("couldn't find sqlplus under:" + sqlPlusDir.getAbsolutePath());
        }
        log.debug("sqlplus=" + sqlPlusFile.getAbsolutePath());
        return sqlPlusFile.getAbsolutePath();
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
    private List<String> getCommandParameters(String scriptName, String arg, String user) throws PluginException {
        List<String> cmd =  new ArrayList<String>(8);
           
        log.debug("script=" + scriptName);
        String fileName = "@" + scriptName;   

        String sqlCmd = getSqlPlusPath();
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
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {    
            ExecuteWatchdog watchdog = new ExecuteWatchdog(getTimeoutMillis());
            log.debug("script=" + script);               
            
            Execute ex = new Execute(new PumpStreamHandler(output), watchdog);            
            ex.setWorkingDirectory(new File(System.getProperty(AgentConfig.AGENT_BUNDLE_HOME)));           
            List<String> cmd = getCommandParameters(script, parameter, user);
            log.debug("working dir=" + System.getProperty(AgentConfig.AGENT_BUNDLE_HOME));
            log.debug("cmd=" + cmd);
            
            ex.setCommandline((String[]) cmd.toArray(new String[0]));
            updateSidInEnv(ex);
            
            int exitCode = ex.execute();
            log.debug("before getting output:" + exitCode);
            String outputStr = output.toString();
            log.debug("after execute:" + outputStr);
            boolean success = true;
            
            if ( (exitCode != 0) || (outputStr.indexOf(expectedOutput) == -1) ){                
                success = false;
                // parse output 
                setMessage(getErrorString(outputStr));                    
            }            

            // Check for watchdog timeout.  Note this does not work with scripts
            if (watchdog.killedProcess()) {
                String err = "Command did not complete within timeout of " + getTimeout() + " seconds";
                getLog().error(err);
                setMessage(err);
                success = false;
            }
            
            if (!success) {
                throw new PluginException(getMessage());
            }
            
            
        } catch (Exception err) {
                throw new PluginException("sqlplus action failed:" + err.getMessage(), err);
        }  
        finally {
            if (output != null) {
                try {
                    output.close();
                }catch(IOException e) {                   
                    log.error("failed to close output", e);
                }
            }
        }
        
    }

    private void startOracle(String user) throws PluginException {
          try {
              sqlPlusAction(startupScript, "ORACLE instance started.", null, null);
          }
          catch(PluginException ex) {
              if (isWin32()) {
                  // in windows can't run as another user without password
                  throw ex;
              }              
              String message = getMessage();
              if ( user != null && message != null && message.indexOf(INSUFFICIENT_PRIVILEGES_ERROR) != -1 ) {
                  log.info("sqlPlusAction failed with " + message + " trying to run with user=" + user);
                  sqlPlusAction(startupScript, "ORACLE instance started.", user, null);
              }
              else {
                  throw ex;
              }
          }
    }
    
    private void  stopOracle(String[] args) throws PluginException {
        log.debug("args=" + args);           
        String mode = getModeParameter(args);
        try{
             sqlPlusAction(shutdownScript, "ORACLE instance shut down.", null, mode);
        }
        catch(PluginException ex) {
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
        log.debug("args.length=" + args.length);
        if (args.length >0) {
            log.debug("args[0]:" + args[0]);
        }
        
        if (args.length > 1 ) {
            log.debug("args[1]:" + args[1]);
        }
        if (args == null || args.length < 1) {
            return "normal";
        }
        if (args[0].length() == 0) {
            return "normal";
        }
        return args[0];
    }   

    

    protected boolean isRunning() {
        OracleConnection conn = null ;        
        try {            
            String dbUrl = config.getValue(OracleMeasurementPlugin.PROP_URL);
            if (dbUrl == null) {
                throw new PluginException("no url is given");
            }
            conn = getOracleConnection(false, dbUrl, false);
            log.debug("isRunning returns true");
            return true;            
        }
        catch(PluginException e) {
            log.debug("isRunning - got exception: - returning false " + e.getMessage());
            log.debug("isRunning returns false");
            return false;
        }
        finally {
            if (conn != null) {
                DBUtil.closeConnection(_logCtx, conn);
            }
        }
    }
   
    
    private OracleConnection getOracleConnection(boolean addPerlimAuth, String url, boolean asSysDba) throws PluginException {
        OracleDataSource ds;
        try {
            String user = config.getValue(OracleMeasurementPlugin.PROP_USER);
            if (user == null) {
                log.info("No value for config property " + OracleMeasurementPlugin.PROP_USER);
                throw new PluginException("No value for config property " + OracleMeasurementPlugin.PROP_USER);
            }
            String password = config.getValue(OracleMeasurementPlugin.PROP_PASSWORD);
            if (password == null) {
                log.info("No value for config property " + OracleMeasurementPlugin.PROP_PASSWORD);
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
            OracleConnection conn = (OracleConnection)ds.getConnection();

            log.debug("getOracleConnection success");
            return conn;
        }catch(SQLException e) {            
            throw new PluginException(e.getMessage(), e);            
        }
    }


        
}
