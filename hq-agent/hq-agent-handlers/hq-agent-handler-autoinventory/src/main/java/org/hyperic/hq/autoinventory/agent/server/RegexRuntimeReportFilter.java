/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2012], VMware, Inc.
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

package org.hyperic.hq.autoinventory.agent.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerExtValue;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.hq.autoinventory.CompositeRuntimeResourceReport;
import org.hyperic.hq.product.RuntimeResourceReport;

public class RegexRuntimeReportFilter implements RuntimeReportFilter {
    
    private static Log log =
        LogFactory.getLog(RegexRuntimeReportFilter.class.getName());
    AgentDaemon agent = null;
    private final String REGEXRUNTIMEFILTER = "regexruntimereportfilter";
    private final String RESOURCETYPESTRING = "resourcetype";
    private final String RESOURCENAMESTRING = "resourcename";
    private Pattern resourceTypePattern = null;
    private Pattern resourceNamePattern = null;

    public CompositeRuntimeResourceReport filterReport(CompositeRuntimeResourceReport r) {
        log.info("Starting RegexRuntimeReportFilter");

        agent = AgentDaemon.getMainInstance();

        Properties p = agent.getBootConfig().getBootProperties();

        String resourceTypeString = p.getProperty(REGEXRUNTIMEFILTER + "." + RESOURCETYPESTRING);
        String resourceNameString = p.getProperty(REGEXRUNTIMEFILTER + "." + RESOURCENAMESTRING);

        if (null != resourceTypeString || null != resourceNameString) { 
            RuntimeResourceReport[] _serverReports = r.getServerReports();
            List<RuntimeResourceReport> newServerReports = new ArrayList<RuntimeResourceReport>();
            if (resourceTypeString != null) {
                resourceTypePattern = Pattern.compile(resourceTypeString);
            } 
            if (resourceNameString != null) {
                resourceNamePattern = Pattern.compile(resourceNameString);
            } 

            List<RuntimeResourceReport> reportArray = new ArrayList<RuntimeResourceReport>(Arrays.asList(_serverReports));
            Iterator<RuntimeResourceReport> reportIterator = reportArray.iterator();
            while ( reportIterator.hasNext() ) {
                RuntimeResourceReport report = reportIterator.next();
                AIPlatformValue[] platforms = report.getAIPlatforms();
                List<AIPlatformValue> pArray = new ArrayList<AIPlatformValue>(Arrays.asList(platforms));
                Iterator<AIPlatformValue> pIterator = pArray.iterator();
                while ( pIterator.hasNext() ) {
                    AIPlatformValue platform = pIterator.next();
                    AIServerValue[] servers
                        = ((AIPlatformValue) platform).getAIServerValues();
                    if ((servers == null) || (servers.length == 0)) {
                        continue;
                    }

                    List<AIServerValue> serverArray = new ArrayList<AIServerValue>(Arrays.asList((AIServerValue[])servers));
                    Iterator<AIServerValue> serverIterator = serverArray.iterator();
                    while ( serverIterator.hasNext() ) {
                        AIServerExtValue server = (AIServerExtValue)serverIterator.next();
                        if (isResourceNameMatch(server.getName()) || 
                                isResourceTypeMatch(server.getServerTypeName())) {
                            log.debug("Skipping auto discovered Server because it matched regex. " + server.getName());
                            serverIterator.remove();
                            continue;
                        } 
                        List<AIServiceValue> serviceArray = new ArrayList<AIServiceValue>(server.getAIServiceValuesAsList());
                        Iterator<AIServiceValue> serviceIterator = serviceArray.iterator();
                        while ( serviceIterator.hasNext() ) {
                            AIServiceValue service = serviceIterator.next();
                            if (isResourceNameMatch(service.getName()) || 
                                    isResourceTypeMatch(service.getServiceTypeName())) {
                                log.info("Skipping auto discovered Service because it matched regex. " + service.getName());
                                serviceIterator.remove();
                            }
                        }
                        server.setAIServiceValues(serviceArray.toArray(new AIServiceValue[serviceArray.size()]));
                        platform.updateAIServerValue(server);
                    }
                }
                report.setAIPlatforms(pArray.toArray(new AIPlatformValue[pArray.size()]));
                newServerReports.add(report);
            }
            r.setServerReports(newServerReports.toArray(new RuntimeResourceReport[newServerReports.size()]));
            return r;
        } else {
            return r;
        }
    }

    boolean isResourceTypeMatch(String type) {
        if (resourceTypePattern != null) {
            return isMatch(resourceTypePattern, type);
        } else {
            return false;
        }
    }

    boolean isResourceNameMatch(String type) {
        if (resourceNamePattern != null) {
            return isMatch(resourceNamePattern, type);
        } else {
            return false;
        }
    }

    boolean isMatch(Pattern pattern, String string) {
        if (null == pattern || null == string) {
            return false;
        }
        try {
            Matcher matcher = pattern.matcher(string);
            return matcher.find();
        } catch (Exception e) {
            log.debug("Exception matching response: " +
                            e.getMessage(), e);
        }
        return false;
    }
}