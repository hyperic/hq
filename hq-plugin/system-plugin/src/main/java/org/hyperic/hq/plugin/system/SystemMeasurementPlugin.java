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

package org.hyperic.hq.plugin.system;

import java.util.Properties;

import org.hyperic.sigar.FileInfo;
import org.hyperic.sigar.NetFlags;

import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.SigarMeasurementPlugin;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.ptql.ProcessQuery;
import org.hyperic.sigar.ptql.ProcessQueryFactory;

public class SystemMeasurementPlugin
extends SigarMeasurementPlugin
{

	private static final String _3A = "%3A";
	private static final String NETWORK_SERVER_INTERFACE = "NetworkServer Interface";

	private void reportError(Metric metric, Exception e) {
		getLog().error(metric + ": " + e.getMessage(), e);
		getManager().reportEvent(metric,
				System.currentTimeMillis(),
				LogTrackPlugin.LOGLEVEL_ERROR,
				"system", e.getMessage());
	}

	public MetricValue getValue(Metric metric) 
			throws PluginException, MetricNotFoundException, MetricUnreachableException{
		
		String name = metric.getObjectName();
		//Virtual network interfaces have only availability data so if we will try to get other kind
		//of metrics for them via Sigar an exception will get thrown.
		//Jira issue [HHQ-5569]
		if (name.contains(NETWORK_SERVER_INTERFACE) && name.contains(_3A) && !metric.getCategory().
				equals(MeasurementConstants.CAT_AVAILABILITY)) {
			return new MetricValue(Double.NaN);
		}
		String domain = metric.getDomainName();

		Properties props = metric.getObjectProperties();
		String type = props.getProperty("Type");
		boolean isFsUsage = type.endsWith("FileSystemUsage");

		if (domain.equals("sigar.ext")) {
			if (type.equals("CpuInfo")) {
				return new MetricValue(getNumCpus());
			}
			else if (type.equals("DiskUsage")) {
				//back-compat w/ old template
				String arg = props.getProperty("Arg");
				return new MetricValue(getDiskUsage(arg));
			} else if (type.equals("ChildProcesses")) {
				String arg = props.getProperty("Arg");
				return new MetricValue(getChildProcessCount(arg));
			}
		}
		//else better be "system.avail"

		if (type.equals("Platform")) {
			return new MetricValue(Metric.AVAIL_UP);
		}

		if (type.equals("NetIfconfig")) {
			double avail;

			int value = (int)super.getValue(metric).getValue();
			if (((value & NetFlags.IFF_UP) > 0) && ((value & NetFlags.IFF_RUNNING) == NetFlags.IFF_RUNNING)) {
				avail = Metric.AVAIL_UP;
			} else if ((value & NetFlags.IFF_UP) > 0 ) {
				avail = Metric.AVAIL_WARN;
			} else {
				avail = Metric.AVAIL_DOWN;
			}

			return new MetricValue(avail);
		}

		if (isFsUsage) {
			double avail;
			try {
				super.getValue(metric).getValue();
				avail = Metric.AVAIL_UP;
			} catch (Exception e) {
				avail = Metric.AVAIL_DOWN;
				reportError(metric, e);
			}

			return new MetricValue(avail);
		}

		if (type.equals("FileInfo") ||
				type.equals("DirStats"))
		{
			String attr = metric.getAttributeName();
			double avail;

			try {
				double val =
						super.getValue(metric).getValue();
				avail = Metric.AVAIL_UP;

				//for directories, verify the type.
				if (attr.equals("Type") &&
						(val != FileInfo.TYPE_DIR))
				{
					avail = Metric.AVAIL_DOWN;
				}
			} catch (MetricNotFoundException e) {
				//SigarFileNotFoundException
				avail = Metric.AVAIL_DOWN;
			}

			return new MetricValue(avail);
		}

		if (type.equals("CpuPercList")) {
			try {
				super.getValue(metric);
				// If we can get the metric, return avail up
				return new MetricValue(Metric.AVAIL_UP);
			} catch (Exception e) {
				return new MetricValue(Metric.AVAIL_DOWN);
			}
		}

		if (type.equals("MultiProcCpu")) {
			double val = super.getValue(metric).getValue();
			if (val >= 1) {
				return new MetricValue(Metric.AVAIL_UP);
			}
			else {
				return new MetricValue(Metric.AVAIL_DOWN);
			}
		}
		throw new MetricNotFoundException(metric.toString());
	}

	private double getChildProcessCount(String arg)
			throws MetricNotFoundException {

		try {
			Sigar s = getSigar();
			long processIds[] = s.getProcList();
			ProcessQuery query = ProcessQueryFactory.getInstance().getQuery(arg);
			long parentPid = query.findProcess(s);

			double count = 0;
			for (long pid : processIds) {
				ProcState state;
				try {
					state = s.getProcState(pid);
					if (parentPid == state.getPpid()) {
						count++;
					}
				} catch (SigarException e) {
					//ok, Process likely went away
				}
			}
			return count;
		} catch (Exception e) {
			throw new MetricNotFoundException(e.getMessage(), e);
		}
	}

	private double getNumCpus()
			throws MetricNotFoundException {

		try {
			return (double)getSigar().getCpuInfoList().length;
		} catch (Exception e) {
			throw new MetricNotFoundException(e.getMessage(), e);
		}
	}

	private long getDiskUsage(String arg)
			throws MetricNotFoundException {

		try {
			return getSigar().getDirUsage(arg).getDiskUsage();
		} catch (Exception e) {
			throw new MetricNotFoundException(e.getMessage(), e);
		}
	}
}
