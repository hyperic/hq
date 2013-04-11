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

package org.hyperic.hq.plugin.mssql;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.product.Win32ControlPlugin;
import org.hyperic.hq.product.Win32MeasurementPlugin;
import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

public class MsSQLMeasurementPlugin
    extends Win32MeasurementPlugin {

    private static final String MSSQL_LOGIN_TIMEOUT = "mssql.login_timeout";
	private static final String ATTR_NAME_DATABASE_FREE_PERCENT = "Database Free Percent";
	private static final String ATTR_NAME_DATABASE_FREE_PERCENT_2000 = "Database Free Percent 2000";
	private static final String MDF_FREE_SPACE_PCT2005_SQL = "MDF_FreeSpacePct2005.sql";
	private static final String MDF_FREE_SPACE_PCT2000_SQL = "MDF_FreeSpacePct2000.sql";
	private static final String PROP_LOCK = "lock.name";
    private static final String PROP_CACHE = "cache.name";

    private static final String TOTAL_NAME = "_Total";

    static final String DEFAULT_SQLSERVER_METRIC_PREFIX = "SQLServer";
    static final String DEFAULT_SQLAGENT_METRIC_PREFIX = "SQLAgent";

    private String getServiceName(Metric metric) {

        // For the SQLServer: 
        // the sqlServerServiceName will be "MSSQLSERVER" (for default instance name)
        // or "MSSQL$<given_instance_name>" (for given instance name)

        // For the SQLAgent:
        // the service name will be "SQLSERVERAGENT" (for default instance name)
        // or "SQLAgent$<given_instance_name>" (for given instance name)
        Properties props = metric.getProperties();
        String sqlServiceName = props.getProperty(Win32ControlPlugin.PROP_SERVICENAME,
            MsSQLDetector.DEFAULT_SQLSERVER_SERVICE_NAME);
        
        return sqlServiceName;
    }


    protected String getDomainName(Metric metric) {        
        String fullPrefix ="";
        String serviceName = getServiceName(metric);

        if (serviceName.equalsIgnoreCase(MsSQLDetector.DEFAULT_SQLSERVER_SERVICE_NAME)) {
            // service name is "MSSQLSERVER"  so this is a default instance and  
            // the perfmon metric name will be prefixed by "SQLSERVER:"
            fullPrefix = DEFAULT_SQLSERVER_METRIC_PREFIX;  
        } else {
            if (serviceName.equalsIgnoreCase(MsSQLDetector.DEFAULT_SQLAGENT_SERVICE_NAME)) {
                // service name is "SQLSERVERAGENT"  so this is a default instance and  
                // the perfmon metric name will be prefixed by "SQLAgent:"
                fullPrefix = DEFAULT_SQLAGENT_METRIC_PREFIX; 
            }
            else{
                // service name is not one of the above so this is not a default instance
                // the perfmon metric name will be prefixed by the service name
                // i.e. something like "MSSQL$<instance_name>" or "SQLAgent$<instance_name>
                fullPrefix = serviceName; 
            }
        }

        return fullPrefix + ":" + metric.getDomainName();
    }

    protected double adjustValue(Metric metric, double value) {
        if (metric.getAttributeName().startsWith("Percent")) {
            value /= 100;
        }

        return value;
    }

    public String translate(String template, ConfigResponse config) {
        String[] props = { MsSQLDetector.PROP_DB, PROP_LOCK, PROP_CACHE, };

        // parse the template-config
        template = super.translate(template, config);

        for (int i = 0; i < props.length; i++) {
            String prop = props[i];

            if (template.indexOf(prop) > 0) {
                String value = config.getValue(prop, TOTAL_NAME);
                return StringUtil.replace(template, "${" + prop + "}", value);
            }
        }

        return template;
    }

    private static int getServiceStatus(String name) {
        Service svc = null;
        try {
            svc = new Service(name);
            return svc.getStatus();
        } catch (Win32Exception e) {
            return Service.SERVICE_STOPPED;
        } finally {
            if (svc != null) {
                svc.close();
            }
        }
    }

    private String getServerName(Metric metric) {
    	String serverName = metric.getObjectProperty("ServerName");
    	getLog().debug("ServerName from config=" + serverName);
        // there is bug causing the default not to be set for sqlserver_name
    	if (serverName == null || "".equals(serverName) || "%sqlserver_name%".equals(serverName)){
    		serverName = "localhost";
    		getLog().debug("Setting serverName to default=" + serverName);
    	}
    	return serverName;    		
    }

    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException,
        MetricUnreachableException {

        // This metric requires SQL query, not available via perflib
        if (metric.getAttributeName().startsWith(ATTR_NAME_DATABASE_FREE_PERCENT)) {
            // Ignore the SQL Server services, we only want db instances.
            if (metric.getDomainName().endsWith("_Total")) {
                return MetricValue.NONE;
            } else if (ATTR_NAME_DATABASE_FREE_PERCENT_2000.equals(metric.getAttributeName())){
                return getUnallocatedSpace(metric, MDF_FREE_SPACE_PCT2000_SQL);
            } else {
            	return getUnallocatedSpace(metric, MDF_FREE_SPACE_PCT2005_SQL);
            }
        }
        String name = getServiceName(metric);
        if (getServiceStatus(name) != Service.SERVICE_STOPPED) {
            return getValueCompat(metric);
        }
        // XXX should not have to do this, but pdh.dll seems to cache last
        // value in some environments
        if (metric.isAvail() || ("Availability").equals(metric.getObjectProperty("Type"))) // XXX

        {
            return new MetricValue(Metric.AVAIL_DOWN);
        } else if (getTypeInfo().getType() == TypeInfo.TYPE_SERVER) {
            throw new MetricUnreachableException(metric.toString());
        } else {
        	getLog().error("Unable to retrieve value for: " + metric.getAttributeName());
            return MetricValue.NONE; 
        }
    }
    
    private List<String> getScript(Metric metric, String scriptName){
    	String dbNameWithQuotes = "\"" + metric.getDomainName() + "\"";
    	String serverName = getServerName(metric);
    	String username = metric.getObjectProperty("User");
    	String password = metric.getObjectProperty("Password");
        String pdkWorkDir = "\"" + ProductPluginManager.getPdkWorkDir();
        String sqlScript = pdkWorkDir + "/scripts/mssql/"+scriptName + "\"";
        String outputPath = pdkWorkDir + "/scripts/mssql/MDF_FreeSpacePct.out\"";
        String serverNameWithQuotes = "\"" + serverName + "\"";
    	List<String> scriptPropertiesList = new ArrayList<String>();
    	if (MDF_FREE_SPACE_PCT2005_SQL.equals(scriptName)){
    		scriptPropertiesList.add("sqlcmd");
        	
    	} else {
    		scriptPropertiesList.add("osql");
    		scriptPropertiesList.add("-n");
    	}
    	scriptPropertiesList.add("-S");
    	scriptPropertiesList.add(serverNameWithQuotes);
     	scriptPropertiesList.add("-d");
    	scriptPropertiesList.add(dbNameWithQuotes);
    	scriptPropertiesList.add("-s");
    	scriptPropertiesList.add(",");
    	scriptPropertiesList.add("-i");
    	scriptPropertiesList.add(sqlScript);
    	scriptPropertiesList.add("-l");
    	scriptPropertiesList.add(System.getProperty(MSSQL_LOGIN_TIMEOUT, "5"));
    	scriptPropertiesList.add("-h-1");
    	scriptPropertiesList.add("-w");
    	scriptPropertiesList.add("300");
    	
    	if (getLog().isDebugEnabled()){
        	getLog().debug("Script Properties = " + scriptPropertiesList);
        }
    	
		/* If the user specifies the username and password then it is it will use sql authentication.
		   Otherwise, it will use the trusted connection user to access the db. 
		*/
		if (username != null && !"%user%".equals(username)
				&& password != null && !"%password%".equals(password)) {
			getLog().debug(
					"Adding username to script properties: -U " + username);
			scriptPropertiesList.add("-U");
			scriptPropertiesList.add(username);

			getLog().debug(
					"Adding password to script properties: -P *******");
			scriptPropertiesList.add("-P");
			scriptPropertiesList.add(password);
		} else {
			// -E means it is a trusted connection
			getLog().debug("Setting as trusted connection on script properties: -E");
			scriptPropertiesList.add("-E");
		}
		return scriptPropertiesList;
    }

    /**
     * Runs a sqlcmd command piped to an output file, then parses the output
     * file to get database free percent. Not a particularly elegant way to get
     * the data, but it works.
     * 
     * @param dbName - Name of the SQL Server database
     * @param serverName - Name of the SQL Server database instance (config
     *        option defaults to localhost)
     * @return
     * @throws MetricNotFoundException
     */
    private synchronized MetricValue getUnallocatedSpace(Metric metric, String scriptName)
        throws MetricNotFoundException {
        final String dbName = metric.getDomainName();
		final String serverName = getServerName(metric);
		List<String> scriptPropertiesList = getScript(metric, scriptName);
		try {
			Process proc = new ProcessBuilder(
					scriptPropertiesList
							.toArray(new String[scriptPropertiesList.size()]))
					.start();
			StreamHandler stdout = new StreamHandler("StreamHandler-Input", proc.getInputStream(),
					getLog().isDebugEnabled()) {
				private Map<String, String> processMap = new HashMap<String, String>();
				@Override
				protected void processString(String line){
					String[] lineSplit = line.split(",");
					if (lineSplit.length == 2){
						if (dbName.equals(lineSplit[0].trim())){
							getLog().debug("Database name found: " + dbName + " Value="+lineSplit[1].trim());
							processMap.put(lineSplit[0].trim(), lineSplit[1].trim());
						}
					} else {
						getLog().debug("Unknown formatting from script output:" + line);
					}
				}
				
				public Object getResult(){
					return processMap;
				}
			};
			StreamHandler stderr = new StreamHandler("StreamHandler-Error", proc.getErrorStream(),
					getLog().isDebugEnabled());
			stdout.start();
			stderr.start();

			proc.waitFor();
			
			if (!((String)stderr.getResult()).equals("")){
				throw new MetricNotFoundException("Unable to exec process: " + stderr.getResult());
			}
			
			if (stdout.hasError()){
				throw new MetricNotFoundException("Error processing metric script output:" + stdout.getErrorString());
			} else {
				String freePercent = ((Map<String, String>) stdout.getResult())
						.get(dbName);
				if (freePercent != null) {
					MetricValue metricVal = new MetricValue(
							Double.parseDouble(freePercent));
					getLog().debug("Database Free Out Percent: " + freePercent);
					return metricVal;
				} else {
					return MetricValue.NONE;
				}
			}
		} catch (IOException e) {
			getLog().debug("Unable to exec process:", e);
			throw new MetricNotFoundException("Unable to exec process: " + e);
		} catch (InterruptedException e){
			//ignore
		}
		return MetricValue.NONE;
    }
		 
	private class StreamHandler extends Thread {
		private InputStream inputStream;
		private boolean verbose;
		private StringBuilder stringBuilder = new StringBuilder();
		private boolean hasError = false;
		private String errorString = null;

		public StreamHandler(String threadName, InputStream inputStream, boolean verbose) {
			super(threadName);
			this.inputStream = inputStream;
			this.verbose = verbose;
		}

		@Override
		public void run() {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new InputStreamReader(
						inputStream));
				String line = br.readLine();
				while (line != null) {
					if (verbose) {
						getLog().debug(line);
					}
					processString(line);
					line = br.readLine();
				}
			} catch (IOException e) {
				getLog().debug("Exception reading stream: " , e);
				errorString = e.getMessage();			
				hasError = true;
			} finally {
				if (br != null){
					try {
						br.close();
					} catch (Exception e){
						//ignore
					}
				}
			}
		}
		
		protected void processString(String line){
			stringBuilder.append(line+"\n");
		}
		
		public String getErrorString(){
			return errorString;
		}
		
		public boolean hasError(){
			return hasError;
		}
		
		public Object getResult(){
			return stringBuilder.toString();
		}
	}
	
    private MetricValue getValueCompat(Metric metric)
            throws PluginException,
                   MetricNotFoundException,
                   MetricUnreachableException {
            
            String domain = getDomainName(metric);
            String attr   = getAttributeName(metric);

            StringBuffer name = new StringBuffer();

            if (domain.charAt(0) != '\\') {
                name.append("\\");
            }
            name.append(domain);
            if (domain.charAt(domain.length()-1) != '\\') {
                name.append("\\");
            }
            name.append(attr);

            String typeProp = metric.getObjectProperty("Type");
            boolean isFormatted=false, isAvail=false;
            
            if (("Formatted").equals(typeProp)) {
                isFormatted = true;
            }
            else if (("Availability").equals(typeProp)) {
                isAvail = true;
            }
            
            return getPdhValue(metric, name.toString(),
                               isAvail, isFormatted);
        }

    private MetricValue getPdhValue(Metric metric,
            String counter,
            boolean isAvail,
            boolean isFormatted)
                    throws MetricNotFoundException {

            Pdh pdh = null;

            try {
                pdh = new Pdh();
                double value;
                //default is raw
                if (isFormatted) {
                    value = pdh.getFormattedValue(counter);    
                }
                else {
                    value = pdh.getRawValue(counter);    
                }

                if (isAvail) {
                    return new MetricValue(Metric.AVAIL_UP);
                }
                return new MetricValue(adjustValue(metric, value),
                        System.currentTimeMillis());
            } catch (Win32Exception e) {
                throw new MetricNotFoundException(counter);
            } finally {
                if (pdh != null) {
                    try { pdh.close(); } catch (Win32Exception e) {}
                }
            }
    }
}
