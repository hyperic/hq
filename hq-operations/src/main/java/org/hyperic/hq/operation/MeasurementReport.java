/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2009-2010], VMware, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
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
 */

package org.hyperic.hq.operation;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeasurementReport extends SecureOperation {

    private Map<Integer, Map<Integer, List<MetricValue>>> clientIDs = new HashMap<Integer, Map<Integer, List<MetricValue>>>();

    private List<DSNList> clientIdList;

    private List<SRN> srnList;
      
    /* legacy */
    private static final String PROP_CIDLIST = "cids";
    private static final String PROP_DSNIDLIST = "dsnidss";
    private static final String PROP_TSTAMPLIST = "tstamps";
    private static final String PROP_VALUELIST = "values";
    private static final String PROP_SRN_ENT_TYPE = "srnEntType";
    private static final String PROP_SRN_ENT_ID = "srnEntId";
    private static final String PROP_SRN_REVNO = "srnRevNo";
    private static final String PROP_AGENT_TOKEN = "agentToken";
    /* end legacy */

    @JsonCreator
    public MeasurementReport(@JsonProperty("agentToken") String agentToken, @JsonProperty("clientIdList") List<DSNList> clientIdList,
                             @JsonProperty("srnList") List<SRN> srnList) {
        super(agentToken);
        this.clientIdList = clientIdList;
        this.srnList = srnList;
    }

    @Override
    public String toString() {
        StringBuffer output = new StringBuffer("Measurement Report:\n");

        for (DSNList cid : clientIdList) {
            List<MetricValueList> dsns = cid.getDsns();
            System.out.println(cid.getClientId());
            for (MetricValueList mvl : dsns) {
                List<MetricValue> metricList = mvl.getMetricValues();
                System.out.println("\t" + mvl.getDsnId());

                for (MetricValue metric : metricList) {
                    output.append("Data point for CID=").append(cid.getClientId()).append(" DSN ID=").append(mvl.getDsnId())
                            .append(" Value=").append(metric.getValue()).append(" tStamp=").append(metric.getTimestamp()).append("\n");
                }
            }
        }
        return output.toString();
    }


    public static class MeasurementReportBuilder {

        private Map<Integer, Map<Integer, List<MetricValue>>> clientIDs = new HashMap<Integer, Map<Integer, List<MetricValue>>>();


        public void addMetric(final int clientID, final int dsnID, final MetricValue data) {
            Map<Integer, List<MetricValue>> dsnMap = clientIDs.get(clientID);
            if (dsnMap == null) {
                dsnMap = new HashMap<Integer, List<MetricValue>>();
                clientIDs.put(clientID, dsnMap);
            }

            List<MetricValue> valData = dsnMap.get(dsnID);
            if (valData == null) {
                valData = new ArrayList<MetricValue>();
                dsnMap.put(dsnID, valData);
            }

            valData.add(data);
        }

        public List<DSNList> buildMetricValues() {
            List<DSNList> cids = new ArrayList<DSNList>(clientIDs.size());

            for (Map.Entry<Integer, Map<Integer, List<MetricValue>>> clientID : this.clientIDs.entrySet()) {
                Integer clientId = clientID.getKey();
                Map<Integer, List<MetricValue>> dsnMap = clientID.getValue();
                /* new */
                List<MetricValueList> dsns = new ArrayList<MetricValueList>(dsnMap.size());

                for (Map.Entry<Integer, List<MetricValue>> o1 : dsnMap.entrySet()) {
                    Integer dsnID = o1.getKey();
                    List<MetricValue> metrics = o1.getValue();
                    List<MetricValue> data = new ArrayList<MetricValue>(metrics.size());

                    for (MetricValue metric : metrics) {
                        data.add(metric);
                    }

                    dsns.add(new MetricValueList(dsnID, data));
                }
                cids.add(new DSNList(clientId, dsns));
            }
            return cids;
        }
    }


    public static void dumpReport(List<DSNList> cids) {
        for (DSNList cid : cids) {
            List<MetricValueList> dsns = cid.getDsns();
            System.out.println(cid.getClientId());
            for (MetricValueList mvl : dsns) {
                List<MetricValue> metricList = mvl.getMetricValues();
                System.out.println("\t" + mvl.getDsnId());
                for (MetricValue metric : metricList) {
                    System.out.println("\t\t" + metric.getTimestamp() + " " + metric.getValue());
                }
            }
        }
    }
}
