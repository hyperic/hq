package org.hyperic.hq.plugin.exchange;

import java.io.ByteArrayOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.ExecuteWatchdog;
import org.hyperic.util.exec.PumpStreamHandler;

public class ExchangeDagDetector {
    
  private static final Log log = LogFactory.getLog(ExchangeDagDetector.class);
    private static final String POWERSHELL_COMMAND = "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";
    
    public static String getDagName(String exchangeInstallDir,String platformName) { 
        String[] command = new String[] { POWERSHELL_COMMAND, "-command",
                "\". '" +
                exchangeInstallDir +
                "\\bin\\RemoteExchange.ps1'; Connect-ExchangeServer -auto ; Get-DatabaseAvailabilityGroup\""};
        String commandOutput = runCommand(command);
        log.debug("DAG after run command. Output: " + commandOutput);
        
        String dagName = getDagNameFromCommandOutput(commandOutput, platformName);
        return dagName;
    }

    private static String getDagNameFromCommandOutput(String commandOutput, String platformName) {
        Pattern dagNamePattern = Pattern.compile("^(\\S+)\\s+\\{(.*)\\}",
                Pattern.MULTILINE);
        Matcher matcher = dagNamePattern.matcher(commandOutput);
        while(matcher.find()) {
            String dagName = matcher.group(1);
            String platforms = matcher.group(2);

            log.debug("Dag name: " + dagName + " Platforms: " + platforms);
            
            String[] platformsArray = platforms.split(",");
            for(String platform : platformsArray) {
                if(platformName.equalsIgnoreCase(platform.trim())) {
                    log.debug("Found DAG name: " + dagName);
                    return dagName;
                }
            }
        }
        
        log.debug("Didn't find DAG name");        
        return null;
    }

    private static String runCommand(String[] command) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        final PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(output);
        
        ExecuteWatchdog wdog = 
                new ExecuteWatchdog(20 * 1000);

        Execute exec = new Execute(pumpStreamHandler,wdog);
        exec.setCommandline(command);
        log.debug("Running: " + exec.getCommandLineString());
        try {
            exec.execute();
        } catch (Exception e) {
            log.debug("Fail to run command: " + exec.getCommandLineString() + " " + e.getMessage());
            return null;
        }
        String out = output.toString().trim();
        return out;
    }
}
