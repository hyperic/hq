package org.hyperic.hq.plugin.zimbra.five;

import java.io.File;
import java.io.FilenameFilter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.sigar.SigarException;
import org.hyperic.util.config.ConfigResponse;

public class ZimbraServerDetector extends ServerDetector implements AutoServerDetector {

	private static final String PROCESS_NAME = "zmmailboxdmgr" ;
	private static final String PROCESS_DIR = "/libexec";
	private static final String ZMSTATS_DIR = "zmstat";
	private static final String PTQL_QUERY = "State.Name.eq={0}";
	private static final String PROCESS_PID_QUERY = "Pid.PidFile.eq={0}";
	private static final String PROCESS_EXEC = "Exe.Name.eq={0}";
	// private static final String PROCESS_CHILD_QUERY = "State.Ppid.eq={0,number,#}";

	private static Log log = LogFactory.getLog(ZimbraServerDetector.class);

	public List getServerResources(ConfigResponse platformConfig) throws PluginException {
		log.debug("getServerResources(" + platformConfig + ")");

		String args[]={PROCESS_NAME};
		long[] pids = getPids(MessageFormat.format(PTQL_QUERY, args));
		List list_servers = new ArrayList();
		for (int n = 0; n < pids.length; n++) {
			long pid = pids[n];
			try {
				String exe = getSigar().getProcExe(pid).getName();
				String path = exe.substring(0, exe.length() - (PROCESS_DIR + File.separator + PROCESS_NAME).length());
				log.debug("proc: (" + pid + ") " + exe + " (" + path + ")");
				if (new File(path, ZMSTATS_DIR).exists()) {
					ConfigResponse productConfig = new ConfigResponse();
					productConfig.setValue("installpath", path);

					ServerResource server = new ServerResource();
					server.setType(this);
					server.setName(getPlatformName() + " " + this.getName());
					server.setInstallPath(path);

					// server.setProductConfig(productConfig);
					setProductConfig(server, productConfig);

					server.setMeasurementConfig(new ConfigResponse());
					server.setControlConfig(new ConfigResponse());
					server.setIdentifier("zimbra " + path);

					list_servers.add(server);
				}
			} catch (SigarException e) {
				e.printStackTrace();
			}
		}

		return list_servers;
	}

	private final static String[][] SERVICES = { { "MySQL", "/db/mysql.pid" }, { "Postfix", "/data/postfix/spool/pid/master.pid" }, { "Log Watch", "/log/logswatch.pid" }, { "Logger MySQL", "/logger/db/mysql.pid" }, { "OpenLDAP", "/openldap/var/run/slapd.pid" }, { "Swatch", "/log/swatch.pid" }, { "MTA Config", "/log/zmmtaconfig.pid" }, { "memcached", "/log/memcached.pid" }, { "ClamAV", "/log/clamd.pid" }, { "Convertd Monitor", "/log/zmconvertdmon.pid" }, { "Jetty Process", "/log/zmmailboxd_java.pid" } };
	private final static String[][] MULTI_SERVICES = { { "AMaViS", "/log/amavisd.pid" }, { "HTTPD", "/log/httpd.pid" }, { "NGINX", "/log/nginx.pid" }, { "Cyrus SASL", "/cyrus-sasl/state/saslauthd.pid" } };
	private final static String[][] OTHER_SERVICES = { { "MTAQueue Stats", "/zmstat/mtaqueue.csv" }, { "PROC Stats", "/zmstat/proc.csv" }, { "VM Stats", "/zmstat/vm.csv" } };

	protected List discoverServices(ConfigResponse config) throws PluginException {
		log.debug("discoverServices(" + config + ")");
		List services = new ArrayList();

		for (int n = 0; n < OTHER_SERVICES.length; n++) {
			String[] serviceSata = OTHER_SERVICES[n];
			File csvFile = new File(config.getValue("installpath"), serviceSata[1]);
			if (csvFile.exists()) {
				ServiceResource service = new ServiceResource();
				service.setServiceName(serviceSata[0]);
				service.setType(this, serviceSata[0]);
				service.setDescription(csvFile.getAbsolutePath());

				ConfigResponse props = new ConfigResponse();
				setProductConfig(service, props);

				service.setMeasurementConfig(new ConfigResponse());
				service.setCustomProperties(new ConfigResponse());

				services.add(service);
			} else {
				log.info("'" + serviceSata[0] + "(" + serviceSata[1] + ")' not found");
			}
		}

		for (int n = 0; n < SERVICES.length; n++) {
			String[] serviceSata = SERVICES[n];
			File pidFile = new File(config.getValue("installpath"), serviceSata[1]);
			if (pidFile.exists()) {
				ServiceResource service = new ServiceResource();
				service.setServiceName(serviceSata[0]);
				service.setType(this, serviceSata[0]);
				service.setDescription(pidFile.getAbsolutePath());

				ConfigResponse props = new ConfigResponse();
				String args[] = { pidFile.getAbsolutePath() };
				props.setValue("process.query", MessageFormat.format(PROCESS_PID_QUERY, args));
				setProductConfig(service, props);

				service.setMeasurementConfig(new ConfigResponse());
				service.setCustomProperties(new ConfigResponse());

				services.add(service);
			} else {
				log.info("'" + serviceSata[0] + "(" + serviceSata[1] + ")' not found");
			}
		}

		for (int n = 0; n < MULTI_SERVICES.length; n++) {
			String[] serviceSata = MULTI_SERVICES[n];
			File pidFile = new File(config.getValue("installpath"), serviceSata[1]);
			log.debug("pidFile='" + pidFile + "'");
			String args[] = { pidFile.getAbsolutePath() };
			long[] pids = getPids(MessageFormat.format(PROCESS_PID_QUERY, args));
			if (pids.length > 0) {
				long pid = pids[0];
				String exec = ServerDetector.getProcExe(pid);
				log.debug("'" + serviceSata[0] + "' exec='" + exec + "'");

				ServiceResource service = new ServiceResource();
				service.setServiceName(serviceSata[0]);
				service.setType(this, serviceSata[0]);
				service.setDescription(pidFile.getAbsolutePath());

				ConfigResponse props = new ConfigResponse();
				String[] args2 = { exec };
				props.setValue("process.query", MessageFormat.format(PROCESS_EXEC, args2));
				setProductConfig(service, props);

				service.setMeasurementConfig(new ConfigResponse());
				service.setCustomProperties(new ConfigResponse());

				services.add(service);
			} else {
				log.info("'" + serviceSata[0] + "(" + serviceSata[1] + ")' not found");
			}
		}

		long[] pids = getPids("State.Name.eq=java,Args.*.eq=com.zimbra.cs.convertd.TransformationServer");
		if (pids.length > 0) {
			ServiceResource service = new ServiceResource();
			service.setServiceName("Convertd");
			service.setType(this, "Convertd");

			ConfigResponse props = new ConfigResponse();
			setProductConfig(service, props);

			service.setMeasurementConfig(new ConfigResponse());
			service.setCustomProperties(new ConfigResponse());

			services.add(service);
		}

		// Stats Process
		File dir_pids = new File(config.getValue("installpath"), "/zmstat/pid");
		String[] pids_files = dir_pids.list(new PIDFilter());
		for (int n = 0; n < pids_files.length; n++) {
			String pid_file = pids_files[n];
			services.add(cerateStatService(pid_file));
		}

		return services;
	}

	private static Pattern p = Pattern.compile("zmstat-(.*).pid");

	private ServiceResource cerateStatService(String file) {
		log.debug("cerateStatService('" + file + "')");
		Matcher m = p.matcher(file);
		String name = file;
		if (m.matches())
			name = m.group();
		ServiceResource servicio = new ServiceResource();
		servicio.setServiceName(name);
		servicio.setType(this, "Stats process");
		servicio.setDescription(file);

		ConfigResponse props = new ConfigResponse();
		props.setValue("pid-file", file);
		setProductConfig(servicio, props);

		servicio.setMeasurementConfig(new ConfigResponse());
		servicio.setCustomProperties(new ConfigResponse());
		return servicio;

	}

	private static class PIDFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			return name.endsWith(".pid");
		}
	}

}
