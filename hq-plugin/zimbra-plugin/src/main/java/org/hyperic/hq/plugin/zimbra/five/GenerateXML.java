/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.plugin.zimbra.five;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

public class GenerateXML {
	static String metricTag = "\t\t<metric name=\"{0}\" category=\"THROUGHPUT\" alias=\"{0}\" interval=\"60000\" indicator=\"false\" units=\"{1}\"/>\n";

	static String serviceTag = "<service name=\"{0} Stats\">\r\t\t<plugin type=\"collector\" class=\"org.hyperic.hq.plugin.zimbra.five.ZimbraCollector\"/>\r\t\t<config>\r\t\t\t<option name=\"{0}-stat-ptql\" default=\"Pid.PidFile.eq=%installpath%/zmstat/pid/zmstat-{0}.pid\" description=\"Sigar PTQL Process Query\"/>\r\t\t</config>\r\t\t<filter name=\"template\" value=\"zimbra-stats:statsfile=%installpath%/zmstat/{0}.csv:$'{'alias'}'\"/>\r\t\t<metric name=\"Availability\" template=\"sigar:Type=ProcState,Arg=%{0}-stat-ptql%:State\" indicator=\"true\"/>\n";

	public static void main(String[] arg) throws IOException {
		StringBuffer res = new StringBuffer();

		List percentage = Arrays.asList(ZimbraCollector.percentage_list);

		File dir = new File("logs");
		String[] files = dir.list(new CSVFilter());
		for (int n=0;n<files.length;n++) {
			String file=files[n];
			System.out.println("-->" + file);
			res.append(MessageFormat.format(serviceTag,file.split("\\.")));
			BufferedReader buffer = new BufferedReader(new FileReader(new File("logs", file)));
			String line = buffer.readLine();
			System.out.println("-->" + line);
			String[] metrics = line.split(",");
			System.out.println("-->" + Arrays.asList(metrics));
			for (int i=0;i<metrics.length;i++) {
				String metric=metrics[i];
				String type = percentage.contains(new File(file).getName()) ? "percentage" : "";
				String args[]={metric.trim(), type};
				res.append(MessageFormat.format(metricTag, args));
			}
			res.append("</service>\n");
		}
		System.out.println(res.toString());
	}

	private static class CSVFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			return name.endsWith("csv");
		}
	}
}
