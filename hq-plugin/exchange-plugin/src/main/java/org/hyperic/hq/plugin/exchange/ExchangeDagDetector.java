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
    
    public static String getDagName(String exchangeInstallDir) { 
        String[] command = new String[] { POWERSHELL_COMMAND, "-command",
                "\". '" +
                exchangeInstallDir +
                "\\bin\\RemoteExchange.ps1'; Connect-ExchangeServer -auto ; Get-DatabaseAvailabilityGroup | Format-List\""};
        String commandOutput = runCommand(command);
        log.debug("DAG after run command. Output: " + commandOutput);
        String dagName = getDagNameFromCommandOutput(commandOutput);
        return dagName;
    }

    private static String getDagNameFromCommandOutput(String commandOutput) {
        Pattern dagNamePattern = Pattern.compile("^Name\\s+:\\s+(.+)$",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher matcher = dagNamePattern.matcher(commandOutput);
        if (!matcher.find()) {
            log.debug("Didn't find DAG name");
            return null;
        }

        String dagName = matcher.group(1);
        log.debug("Found DAG name: " + dagName);
        return dagName;
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
