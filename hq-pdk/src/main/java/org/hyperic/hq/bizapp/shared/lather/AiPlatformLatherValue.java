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

package org.hyperic.hq.bizapp.shared.lather;

import org.hyperic.lather.LatherKeyNotFoundException;
import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;
import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerValue;

public class AiPlatformLatherValue extends AiLatherValue {

    private static final String PROP_AIIPVALUES = "AIIpValues";
    private static final String PROP_AISERVERVALUES = "AIServerValues";
    private static final String PROP_CTIME = "CTime";
    private static final String PROP_MTIME = "MTime";
    private static final String PROP_AGENTTOKEN = "agentToken";
    private static final String PROP_CERTDN = "certdn";
    private static final String PROP_CPUCOUNT = "cpuCount";
    private static final String PROP_DESCRIPTION = "description";
    private static final String PROP_DIFF = "diff";
    private static final String PROP_FQDN = "fqdn";
    private static final String PROP_ID = "id";
    private static final String PROP_IGNORED = "ignored";
    private static final String PROP_LOCATION = "location";
    private static final String PROP_NAME = "name";
    private static final String PROP_TYPENAME = "os"; //left as "os" for backcompat
    private static final String PROP_QUEUESTATUS = "queueStatus";
    private static final String PROP_CPROPS = "cprops";
    private static final String PROP_CONTROLCONFIG = "controlConfig";
    private static final String PROP_MEASUREMENTCONFIG = "measurementConfig";
    private static final String PROP_PRODUCTCONFIG = "productConfig";
    private static final String PROP_AUTO_APPROVE = "autoApprove";

    public AiPlatformLatherValue() {
        super();
    }

    public AiPlatformLatherValue(AIPlatformValue v) {
        this();

        if (v == null) {
            return;
        }

        byte[] customProperties = v.getCustomProperties();
        if (v.customPropertiesHasBeenSet() && customProperties != null) {
            this.setByteAValue(PROP_CPROPS, customProperties);
        }

        byte[] controlConfig = v.getControlConfig();
        if (v.controlConfigHasBeenSet() && controlConfig != null) {
            this.setByteAValue(PROP_CONTROLCONFIG, controlConfig);
        }

        byte[] measurementConfig = v.getMeasurementConfig();
        if (v.measurementConfigHasBeenSet() && measurementConfig != null) {
            this.setByteAValue(PROP_MEASUREMENTCONFIG, measurementConfig);
        }

        byte[] productConfig = v.getProductConfig();
        if (v.productConfigHasBeenSet() && productConfig != null) {
            this.setByteAValue(PROP_PRODUCTCONFIG, productConfig);
        }

        if (v.cTimeHasBeenSet() && v.getCTime() != null) {
            this.setDoubleValue(PROP_CTIME, (double) v.getCTime().longValue());
        }

        if (v.mTimeHasBeenSet() && v.getMTime() != null) {
            this.setDoubleValue(PROP_MTIME, (double) v.getMTime().longValue());
        }

        if (v.agentTokenHasBeenSet() && v.getAgentToken() != null) {
            this.setStringValue(PROP_AGENTTOKEN, v.getAgentToken());
        }

        if (v.certdnHasBeenSet() && v.getCertdn() != null) {
            this.setStringValue(PROP_CERTDN, v.getCertdn());
        }

        if (v.cpuCountHasBeenSet() && v.getCpuCount() != null) {
            this.setIntValue(PROP_CPUCOUNT, v.getCpuCount());
        }

        String description = v.getDescription();
        if (v.descriptionHasBeenSet() && description != null) {
            this.setStringValue(PROP_DESCRIPTION, description, 256);
        }

        if (v.diffHasBeenSet()) {
            this.setDoubleValue(PROP_DIFF, (double) v.getDiff());
        }

        String fqdn = v.getFqdn();
        if (v.fqdnHasBeenSet() && fqdn != null) {
            this.setStringValue(PROP_FQDN, fqdn);
        }

        Integer id = v.getId();
        if (v.idHasBeenSet() && id != null) {
            this.setIntValue(PROP_ID, id);
        }

        if (v.ignoredHasBeenSet()) {
            this.setIntValue(PROP_IGNORED, v.getIgnored() ? 1 : 0);
        }

        if (v.locationHasBeenSet() && v.getLocation() != null) {
            this.setStringValue(PROP_LOCATION, v.getLocation());
        }

        if (v.nameHasBeenSet() && v.getName() != null) {
            this.setStringValue(PROP_NAME, v.getName());
        }

        String platformTypeName = v.getPlatformTypeName();
        if (v.platformTypeNameHasBeenSet() && platformTypeName != null) {
            this.setStringValue(PROP_TYPENAME, platformTypeName);
        }

        if (v.queueStatusHasBeenSet()) {
            this.setIntValue(PROP_QUEUESTATUS, v.getQueueStatus());
        }

        if (v.getAIIpValues() != null) {
            AIIpValue[] ips = v.getAIIpValues();

            for (AIIpValue ip : ips) {
                this.addObjectToList(PROP_AIIPVALUES, new AiIpLatherValue(ip));
            }
        }

        if (v.getAIServerValues() != null) {
            AIServerValue[] srvs = v.getAIServerValues();

            for (AIServerValue svr : srvs) {
                this.addObjectToList(PROP_AISERVERVALUES, new AiServerLatherValue(svr));
            }
        }

        this.setIntValue(PROP_AUTO_APPROVE, v.isAutoApprove() ? 1 : 0);
    }

    public AIPlatformValue getAIPlatformValue() {
        AIPlatformValue r = new AIPlatformValue();

        try {
            r.setCustomProperties(this.getByteAValue(PROP_CPROPS));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setControlConfig(this.getByteAValue(PROP_CONTROLCONFIG));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setMeasurementConfig(this.getByteAValue(PROP_MEASUREMENTCONFIG));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setProductConfig(this.getByteAValue(PROP_PRODUCTCONFIG));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setCTime((long) this.getDoubleValue(PROP_CTIME));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setMTime((long) this.getDoubleValue(PROP_MTIME));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setAgentToken(this.getStringValue(PROP_AGENTTOKEN));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setCertdn(this.getStringValue(PROP_CERTDN));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setCpuCount(this.getIntValue(PROP_CPUCOUNT));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setDescription(this.getStringValue(PROP_DESCRIPTION));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setDiff((long) this.getDoubleValue(PROP_DIFF));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setFqdn(this.getStringValue(PROP_FQDN));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setId(this.getIntValue(PROP_ID));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setIgnored(this.getIntValue(PROP_IGNORED) == 1);
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setLocation(this.getStringValue(PROP_LOCATION));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setName(this.getStringValue(PROP_NAME));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setPlatformTypeName(this.getStringValue(PROP_TYPENAME));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setQueueStatus(this.getIntValue(PROP_QUEUESTATUS));
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            LatherValue[] ips = (LatherValue[])this.getObjectList(PROP_AIIPVALUES);

            for (LatherValue ip : ips) {
                AiIpLatherValue ipVal = (AiIpLatherValue) ip;
                r.addAIIpValue(ipVal.getAIIpValue());
            }
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            LatherValue[] svrs =  (LatherValue[])this.getObjectList(PROP_AISERVERVALUES);

            for (LatherValue svr : svrs) {
                AiServerLatherValue svVal = (AiServerLatherValue) svr;
                r.addAIServerValue(svVal.getAIServerValue());
            }
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        try {
            r.setAutoApprove(this.getIntValue(PROP_AUTO_APPROVE) == 1);
        } catch (LatherKeyNotFoundException exc) { /* ignore */ }

        return r;
    }

    public void validate() throws LatherRemoteException {
    }
}
