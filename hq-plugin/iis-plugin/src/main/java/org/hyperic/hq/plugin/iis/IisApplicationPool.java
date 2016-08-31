package org.hyperic.hq.plugin.iis;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.ExecuteWatchdog;
import org.hyperic.util.exec.PumpStreamHandler;

public class IisApplicationPool {
	private static final Log _log = LogFactory.getLog(IisApplicationPool.class.getName());
	private static final String APPCMD = "C:/Windows/System32/inetsrv/appcmd.exe";
	private static final String WMIC = "C:/Windows/System32/";

	public String name;
	public String status;
	public String dotNetCLRVersion;
	public String managedPipelineMode;

	public static Map getApplicationPools() throws Win32Exception {

		if (new File(APPCMD).exists()) {
			try {
				return getApplicationPoolsViaAppCmd();
			} catch (Exception ex) {
				_log.error(APPCMD + ": " + ex, ex);
				throw new Win32Exception(ex.getMessage());
			}
		} else {
			return null;
		}
	}

	private static double getPerf(int porcessId) {
		final String[] cmd = { WMIC, "" };
		return 0;
	}

	private static Map getApplicationPoolsViaAppCmd() {

		final String[] cmd = { APPCMD, "list", "APPPOOL" };

		Map apppools = new HashMap();

		ByteArrayOutputStream output = executeCommandLine(cmd);

		if (output == null)
			return apppools;

		String appPools[] = output.toString().split("\\r\\n|\\n|\\r");

		Pattern pattern = Pattern.compile(".*\"([^\"]+)\".*");
		Pattern infoPattern = Pattern.compile(".*\\(MgdVersion:([^,]+),MgdMode:([^,]+),state:([^\\)]+)\\)");

		for (int i = 0; i < appPools.length; i++) {
			Matcher matcher = pattern.matcher(appPools[i]);

			IisApplicationPool info = new IisApplicationPool();

			if (matcher.matches()) {
				info.name = matcher.group(1);
			}

			matcher = infoPattern.matcher(appPools[i]);

			if (matcher.matches()) {
				info.dotNetCLRVersion = matcher.group(1);
				info.managedPipelineMode = matcher.group(2);
				info.status = matcher.group(3);
			}

			apppools.put(info.name, info);
		}

		return apppools;
	}

	private static ByteArrayOutputStream executeCommandLine(final String[] cmd) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		ExecuteWatchdog wdog = new ExecuteWatchdog(5 * 1000);
		Execute exec = new Execute(new PumpStreamHandler(output), wdog);

		exec.setCommandline(cmd);

		try {
			int exitStatus = exec.execute();
			if (exitStatus != 0 || wdog.killedProcess()) {
				_log.error(Arrays.asList(cmd) + ": " + output);
				output.close();
				return output;
			}
		} catch (Exception e) {
			_log.error(Arrays.asList(cmd) + ": " + e);
			try {
				output.close();
			} catch (IOException e1) {
				_log.error(Arrays.asList(cmd) + ": " + e1);
			}
			return null;
		}

		return output;
	}

	public static String getPidForApplicationName(String apppool_name) {
		final String[] cmd = { APPCMD, "list", "WP" };

		ByteArrayOutputStream output = executeCommandLine(cmd);

		if (output == null)
			return null;

		String[] lines = output.toString().split("\\r\\n|\\n|\\r");

		if (lines == null || lines.length == 0)
			return null;

		Pattern pattern = Pattern.compile("WP\\s\"(\\d+)\"\\s\\(applicationPool:" + apppool_name + "\\)");

		for (int i = 0; i < lines.length; i++) {
			Matcher matcher = pattern.matcher(lines[i]);

			if (matcher.matches())
				return matcher.group(1);
		}

		return null;
	}

}
