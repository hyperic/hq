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

package org.hyperic.hq.plugin.iis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xpath.XPathAPI;
import org.hyperic.sigar.win32.MetaBase;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.ExecuteWatchdog;
import org.hyperic.util.exec.PumpStreamHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class IisMetaBase {
	private static final Log _log = LogFactory.getLog(IisMetaBase.class.getName());
	private static final String IIS_MKEY = "/LM/W3SVC";
	private static final int MD_SSL_ACCESS_PERM = 6030;
	private static final int MD_ACCESS_SSL = 0x00000008;
	private static final String APPCMD = "C:/Windows/System32/inetsrv/appcmd.exe";

	String id;
	String ip;
	String hostname;
	String port;
	String path;
	boolean requireSSL = false;

	public String toString() {
		String s = "id: " + id + " " + ip + ":" + port;
		if (hostname != null) {
			s += ", Host: " + hostname;
		}
		return s;
	}

	public static Map getWebSites() throws Win32Exception {

		if (new File(APPCMD).exists()) {
			try {
				return getWebSitesViaAppCmd(); // IIS7
			} catch (Exception e) {
				_log.error(APPCMD + ": " + e, e);
				throw new Win32Exception(e.getMessage());
			}
		} else {
			return getWebSitesViaMetaBase();
		}
	}

//	public static Map getApplicationPools() throws Win32Exception {
//
//		if (new File(APPCMD).exists()) {
//			try {
//				return getApplicationPoolsViaAppCmd();
//			} catch (Exception ex) {
//				_log.error(APPCMD + ": " + ex, ex);
//				throw new Win32Exception(ex.getMessage());
//			}
//		} else {
//			return null;
//		}
//	}

	private static boolean parseBinding(IisMetaBase info, String entry) {
		if (entry == null) {
			return false;
		}
		int ix = entry.indexOf(":");
		if (ix == -1) {
			return false;
		}

		// binding format:
		// "listen ip:port:host header"
		info.ip = entry.substring(0, ix);

		entry = entry.substring(ix + 1);
		ix = entry.indexOf(":");
		info.port = entry.substring(0, ix);

		// if host header is defined, URLMetric
		// will add Host: header with this value.
		info.hostname = entry.substring(ix + 1);
		if ((info.hostname != null) && (info.hostname.length() == 0)) {
			info.hostname = null;
		}

		if ((info.ip == null) || (info.ip.length() == 0) || (info.ip.equals("*"))) {
			// not bound to a specific ip
			info.ip = "localhost";
		}

		return true;
	}

//	public static Map getApplicationPoolsViaAppCmd() {
//
//		final String[] cmd = { APPCMD, "list", "APPPOOL" };
//
//		Map apppools = new HashMap();
//
//		ByteArrayOutputStream output = executeCommandLine(cmd);
//
//		if (output == null)
//			return apppools;
//
//		String appPools[] = output.toString().split("\\r\\n|\\n|\\r");
//
//		Pattern pattern = Pattern.compile(".*\"([^\"]+)\".*");
//		Pattern infoPattern = Pattern.compile(".*\\(MgdVersion:([^,]+),MgdMode:([^,]+),state:([^\\)]+)\\)");
//
//		for (int i = 0; i < appPools.length; i++) {
//			Matcher matcher = pattern.matcher(appPools[i]);
//
//			IisApplicationPool info = new IisApplicationPool();
//
//			if (matcher.matches()) {
//				info.name = matcher.group(1);
//			}
//
//			matcher = infoPattern.matcher(appPools[i]);
//
//			if (matcher.matches()) {
//				info.dotNetCLRVersion = matcher.group(1);
//				info.managedPipelineMode = matcher.group(2);
//				info.status = matcher.group(3);
//			}
//			
//			apppools.put(info.name, info);
//		}
//
//		return apppools;
//	}

	// IIS7 does not use MetaBase
	private static Map getWebSitesViaAppCmd() throws Exception {

		final String[] cmd = { APPCMD, "list", "config", "-section:system.applicationHost/sites" };

		Map websites = new HashMap();

		ByteArrayOutputStream output = executeCommandLine(cmd);

		if (output == null)
			return websites;

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document config = db.parse(new ByteArrayInputStream(output.toString().getBytes("UTF-8")));

		NodeList sites = XPathAPI.selectNodeList(config, "//sites/site");

		for (int i = 0; i < sites.getLength(); i++) {
			Element site = (Element) sites.item(i);
			String name = site.getAttribute("name");

			IisMetaBase info = new IisMetaBase();
			info.id = site.getAttribute("id");

			String sitePath = "//site[@name=\"" + name + "\"]/";
			String bindPath = sitePath + "bindings/binding[1]";
			String docPath = sitePath + "application[1]/virtualDirectory[1]/@physicalPath";

			Element binding = (Element) XPathAPI.selectSingleNode(site, bindPath);

			if (binding == null) {
				_log.debug("No bindings defined for: " + name);
				continue;
			}
			String proto = binding.getAttribute("protocol");
			if (proto != null) {
				if ("https".equals(proto.toString().trim())) {
					info.requireSSL = true;
				}
			}
			String bindInfo = binding.getAttribute("bindingInformation");
			if (!parseBinding(info, bindInfo)) {
				_log.debug("Failed to parse bindingInformation=" + bindInfo + " for: " + name);
				continue;
			}
			Object docRoot = XPathAPI.eval(site, docPath);
			if (docRoot != null) {
				info.path = docRoot.toString();
			}
			_log.debug(name + "=" + info);
			websites.put(name, info);
		}

		return websites;
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
				return null;
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

	private static Map getWebSitesViaMetaBase() throws Win32Exception {
		String keys[];
		Map websites = new HashMap();
		MetaBase mb = new MetaBase();

		try {
			mb.OpenSubKey(IIS_MKEY);
			keys = mb.getSubKeyNames();
		} finally {
			mb.close();
		}

		for (int i = 0; i < keys.length; i++) {
			String key = keys[i];
			int id;
			if (!Character.isDigit(key.charAt(0))) {
				continue;
			}

			try {
				id = Integer.parseInt(key);
			} catch (NumberFormatException e) {
				continue;
			}

			String subkey = IIS_MKEY + "/" + id;
			MetaBase srv = null;
			try {
				srv = new MetaBase();
				srv.OpenSubKey(subkey);

				String[] bindings = null;

				IisMetaBase info = new IisMetaBase();

				// IIS 6.0+Windows 2003 has Administration website
				// that requires SSL by default.
				// Any Web Site can be configured to required ssl.
				try {
					int flags = srv.getIntValue(MD_SSL_ACCESS_PERM);
					info.requireSSL = (flags & MD_ACCESS_SSL) != 0;
					if (info.requireSSL) {
						bindings = srv.getMultiStringValue(MetaBase.MD_SECURE_BINDINGS);
					}
				} catch (Win32Exception e) {
				}

				if (bindings == null) {
					bindings = srv.getMultiStringValue(MetaBase.MD_SERVER_BINDINGS);
				}
				info.id = key;

				if (bindings.length == 0) {
					continue;
				}

				if (!parseBinding(info, bindings[0])) {
					continue;
				}
				String name = srv.getStringValue(MetaBase.MD_SERVER_COMMENT);

				websites.put(name, info);

				// XXX this is bogus, else locks the metabase
				// because OpenSubKey does not close the key
				// thats already open.
				srv.close();
				srv = null;
				srv = new MetaBase();
				srv.OpenSubKey(subkey + "/ROOT");
				String docroot = srv.getStringValue(3001);
				info.path = docroot;
			} catch (Win32Exception e) {
			} finally {
				if (srv != null) {
					srv.close();
				}
			}
		}

		return websites;
	}

	public static void main(String[] args) throws Exception {
		Map websites = IisMetaBase.getWebSites();
		System.out.println(websites);
	}
}
