package org.hyperic.hq.plugin.exchange;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ExchangeDagDetector {
    
  private static final Log log = LogFactory.getLog(ExchangeDagDetector.class);
    
    public static String getDagName(String exchangeInstallDir,String platformName, int timeout) { 
        String[] command = new String[] { ExchangeUtils.POWERSHELL_COMMAND, "-command",
                "\". '" +
                exchangeInstallDir +
                "\\bin\\RemoteExchange.ps1'; Connect-ExchangeServer -auto ; Get-DatabaseAvailabilityGroup\""};
        String commandOutput = ExchangeUtils.runCommand(command,timeout);
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
}
