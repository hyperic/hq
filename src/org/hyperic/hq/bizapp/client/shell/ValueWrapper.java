/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

package org.hyperic.hq.bizapp.client.shell;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import org.hyperic.hq.appdef.CpropKey;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQueueConstants;
import org.hyperic.hq.appdef.shared.AgentValue;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.ApplicationValue;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceTypeValue;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.autoinventory.shared.AIHistoryValue;
import org.hyperic.hq.autoinventory.shared.AIScheduleValue;
import org.hyperic.hq.bizapp.shared.uibeans.GroupMetricDisplaySummary;
import org.hyperic.hq.events.shared.AlertValue;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.measurement.shared.BaselineValue;
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;
import org.hyperic.hq.measurement.shared.MeasurementTemplateValue;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.StringUtil;

/**
 * An object which wraps various Value objects for the purpose of
 * making it easy to access them all in a similar fashion.  This
 * removes a bit of the compile-time safety that one has by doing direct
 * calls, but is a good tradeoff in flexibility.
 */
public class ValueWrapper {
    static final int TYPE_PLATFORM            = 1;
    static final int TYPE_SERVER              = 2;
    static final int TYPE_SERVICE             = 3;
    static final int TYPE_APPLICATION         = 4;
    static final int TYPE_PLATFORMTYPE        = 5;
    static final int TYPE_SERVERTYPE          = 6;
    static final int TYPE_SERVICETYPE         = 7;
    static final int TYPE_DERIVEDMEASUREMENT  = 8;
    static final int TYPE_MEASUREMENT         = 9;
    static final int TYPE_BASELINE            = 10;
    static final int TYPE_MEASUREMENTTEMPLATE = 11;
    static final int TYPE_ALERT               = 13;
    static final int TYPE_AIPLATFORM          = 14;
    static final int TYPE_GROUP               = 15;
    static final int TYPE_AGENT               = 17;
    static final int TYPE_AISCHEDULE          = 18;
    static final int TYPE_AIHISTORY           = 19;
    static final int TYPE_CPROPKEY            = 21;
    static final int TYPE_STRING              = 22;
    static final int TYPE_GROUP_METRICS       = 23;

    // Attributes for the various value objects
    public static final int ATTR_ACTION           = 1;
    public static final int ATTR_ACTUAL           = ATTR_ACTION + 1;
    public static final int ATTR_BUSINESSCONTACT  = ATTR_ACTUAL + 1;     
    public static final int ATTR_COMMENT          = ATTR_BUSINESSCONTACT + 1;
    public static final int ATTR_CPUCOUNT         = ATTR_COMMENT + 1;
    public static final int ATTR_CTIME            = ATTR_CPUCOUNT + 1;
    public static final int ATTR_DATA             = ATTR_CTIME + 1;
    public static final int ATTR_DATE_SCHEDULED   = ATTR_DATA + 1;
    public static final int ATTR_DESCRIPTION      = ATTR_DATE_SCHEDULED + 1;
    public static final int ATTR_DETAIL           = ATTR_DESCRIPTION + 1;
    public static final int ATTR_DURATION         = ATTR_DETAIL + 1;
    public static final int ATTR_ENABLED          = ATTR_DURATION + 1;
    public static final int ATTR_ENDDATE          = ATTR_ENABLED + 1;
    public static final int ATTR_ENDTIME          = ATTR_ENDDATE + 1;
    public static final int ATTR_ENGCONTACT       = ATTR_ENDTIME + 1;
    public static final int ATTR_ENTITY_ID        = ATTR_ENGCONTACT + 1;
    public static final int ATTR_ENTITY_TYPE      = ATTR_ENTITY_ID + 1;
    public static final int ATTR_MESSAGE          = ATTR_ENTITY_TYPE + 1;
    public static final int ATTR_FILESOURCE       = ATTR_MESSAGE + 1;
    public static final int ATTR_FILEDEST         = ATTR_FILESOURCE + 1;
    public static final int ATTR_FQDN             = ATTR_FILEDEST + 1;
    public static final int ATTR_ID               = ATTR_FQDN + 1;
    public static final int ATTR_INSTALLPATH      = ATTR_ID + 1;
    public static final int ATTR_INSTANCEID       = ATTR_INSTALLPATH + 1;
    public static final int ATTR_INTERVAL         = ATTR_INSTANCEID + 1;
    public static final int ATTR_LOCATION         = ATTR_INTERVAL + 1;
    public static final int ATTR_MAXEXPECTED      = ATTR_LOCATION + 1;
    public static final int ATTR_MEAN             = ATTR_MAXEXPECTED + 1;
    public static final int ATTR_MINEXPECTED      = ATTR_MEAN + 1;
    public static final int ATTR_MODIFIEDBY       = ATTR_MINEXPECTED + 1;
    public static final int ATTR_MTIME            = ATTR_MODIFIEDBY + 1;
    public static final int ATTR_NAME             = ATTR_MTIME + 1;
    public static final int ATTR_NEXTFIRE         = ATTR_NAME + 1;
    public static final int ATTR_OPSCONTACT       = ATTR_NEXTFIRE + 1;
    public static final int ATTR_OWNER            = ATTR_OPSCONTACT + 1;
    public static final int ATTR_PLUGIN           = ATTR_OWNER + 1;
    public static final int ATTR_SCHEDULED        = ATTR_PLUGIN + 1;
    public static final int ATTR_SCHEDULE_STRING  = ATTR_SCHEDULED + 1;
    public static final int ATTR_STARTDATE        = ATTR_SCHEDULE_STRING + 1;
    public static final int ATTR_STARTTIME        = ATTR_STARTDATE + 1;
    public static final int ATTR_STATUS           = ATTR_STARTTIME + 1;
    public static final int ATTR_SUBJECT          = ATTR_STATUS + 1;
    public static final int ATTR_TEMPLATE         = ATTR_SUBJECT + 1;
    public static final int ATTR_TEMPLATENAME     = ATTR_TEMPLATE + 1;
    public static final int ATTR_TEMPLATEALIAS    = ATTR_TEMPLATENAME + 1;
    public static final int ATTR_TIME             = ATTR_TEMPLATEALIAS + 1;
    public static final int ATTR_TYPE             = ATTR_TIME + 1;
    public static final int ATTR_TYPENAME         = ATTR_TYPE + 1;
    public static final int ATTR_VERSION          = ATTR_TYPENAME + 1;
    public static final int ATTR_QUEUESTATUS      = ATTR_VERSION + 1;
    public static final int ATTR_DIFF             = ATTR_QUEUESTATUS + 1;
    public static final int ATTR_IGNORED          = ATTR_DIFF + 1;
    public static final int ATTR_CERTDN           = ATTR_IGNORED + 1;
    public static final int ATTR_GROUPTYPE        = ATTR_CERTDN + 1;
    public static final int ATTR_ADDRESS          = ATTR_GROUPTYPE + 1;
    public static final int ATTR_AUTHTOKEN        = ATTR_ADDRESS + 1;
    public static final int ATTR_REPLACETOKENS    = ATTR_AUTHTOKEN + 1;
    public static final int ATTR_MESSAGEORDESC    = ATTR_REPLACETOKENS + 1;
    public static final int ATTR_PARENTID         = ATTR_MESSAGEORDESC + 1;
    public static final int ATTR_TEMPLATEUNITS    = ATTR_PARENTID + 1;
    public static final int ATTR_COMPUTETIME      = ATTR_TEMPLATEUNITS + 1;
    public static final int ATTR_USERENTERED      = ATTR_COMPUTETIME + 1;
    public static final int ATTR_CATEGORY         = ATTR_USERENTERED + 1;
    public static final int ATTR_ACTIVE_MEMBERS   = ATTR_CATEGORY + 1;
    public static final int ATTR_TOTAL_MEMBERS    = ATTR_ACTIVE_MEMBERS + 1;
    public static final int ATTR_AGENT_MTIME      = ATTR_TOTAL_MEMBERS + 1;
    public static final int ATTR_AGENT_VERSION    = ATTR_AGENT_MTIME + 1;

    private PlatformValue            platformValue;
    private ServerValue              serverValue;
    private ServiceValue             serviceValue;
    private ApplicationValue         applicationValue;
    private ServerTypeValue          serverTypeValue;
    private ServiceTypeValue         serviceTypeValue;
    private PlatformTypeValue        platformTypeValue;
    private DerivedMeasurementValue  derivedMeasurementValue;
    private MetricValue              measurementValue;
    private BaselineValue            baselineValue;
    private MeasurementTemplateValue measurementTemplateValue;
    private AlertValue               alertValue;
    private AIPlatformValue          aiplatformValue;
    private AppdefGroupValue         groupValue;
    private AgentValue               agentValue;
    private AIScheduleValue          aischeduleValue;
    private AIHistoryValue           aihistoryValue;
    private CpropKey                 cpropKeyValue;
    private String                   simpleString;
    private GroupMetricDisplaySummary groupMetric;

    protected int type;
    protected Object auxData;

    public ValueWrapper(){
    }

    public ValueWrapper(Object val){
        this.init(val);
    }

    public void setWrapee(Object wrapee){
        this.init(wrapee);
    }

    public void setWrapee(Object wrapee, Object auxData) {
        init(wrapee);
        setAuxData(auxData);
    }

    public void setAuxData(Object auxData){
        this.auxData = auxData;
    }

    public void init(Object val){
        if(val instanceof PlatformValue){
            this.platformValue = (PlatformValue)val;
            this.type = TYPE_PLATFORM;
        } else if(val instanceof ServerValue){
            this.serverValue = (ServerValue)val;
            this.type = TYPE_SERVER;
        } else if(val instanceof ServiceValue){
            this.serviceValue = (ServiceValue)val;
            this.type = TYPE_SERVICE;
        } else if(val instanceof ApplicationValue){
            this.applicationValue = (ApplicationValue)val;
            this.type = TYPE_APPLICATION;
        } else if(val instanceof ServerTypeValue){
            this.serverTypeValue = (ServerTypeValue)val;
            this.type = TYPE_SERVERTYPE;
        } else if(val instanceof ServiceTypeValue){
            this.serviceTypeValue = (ServiceTypeValue)val;
            this.type = TYPE_SERVICETYPE;
        } else if(val instanceof PlatformTypeValue){
            this.platformTypeValue = (PlatformTypeValue)val;
            this.type = TYPE_PLATFORMTYPE;
        } else if(val instanceof DerivedMeasurementValue){
            this.derivedMeasurementValue = (DerivedMeasurementValue)val;
            this.type = TYPE_DERIVEDMEASUREMENT;
        } else if(val instanceof MetricValue){
            this.measurementValue = (MetricValue)val;
            this.type = TYPE_MEASUREMENT;
        } else if(val instanceof BaselineValue){
            this.baselineValue = (BaselineValue)val;
            this.type = TYPE_BASELINE;
        } else if(val instanceof MeasurementTemplateValue){
            this.measurementTemplateValue = (MeasurementTemplateValue)val;
            this.type = TYPE_MEASUREMENTTEMPLATE;
        } else if (val instanceof AlertValue) {
            this.alertValue = (AlertValue)val;
            this.type = TYPE_ALERT;
        } else if (val instanceof AIPlatformValue) {
            this.aiplatformValue = (AIPlatformValue)val;
            this.type = TYPE_AIPLATFORM;
        } else if (val instanceof AIScheduleValue) {
            this.aischeduleValue = (AIScheduleValue)val;
            this.type = TYPE_AISCHEDULE;
        } else if (val instanceof AIHistoryValue) {
            this.aihistoryValue = (AIHistoryValue)val;
            this.type = TYPE_AIHISTORY;
        } else if (val instanceof AppdefGroupValue) {
            this.groupValue = (AppdefGroupValue)val;
            this.type= TYPE_GROUP;
        } else if (val instanceof AgentValue) {
            this.agentValue = (AgentValue)val;
            this.type = TYPE_AGENT;
        } else if (val instanceof CpropKey) {
            this.cpropKeyValue = (CpropKey)val;
            this.type = TYPE_CPROPKEY;
        } else if (val instanceof String) {
            this.simpleString = (String)val;
            this.type = TYPE_STRING;
        } else if (val instanceof GroupMetricDisplaySummary) {
            this.groupMetric = (GroupMetricDisplaySummary)val;
            this.type = TYPE_GROUP_METRICS;
        } else
            throw new IllegalArgumentException("Unknown value type:"
                                               + val.getClass().getName());
    }

    public Object getValue(int key){
        Object res;
 
        switch(this.type){
        case TYPE_PLATFORM:
            res = this.platformGet(key);
            break;
        case TYPE_SERVER:
            res = this.serverGet(key);
            break;
        case TYPE_SERVICE:
            res = this.serviceGet(key);
            break;
        case TYPE_APPLICATION:
            res = this.applicationGet(key);
            break;
        case TYPE_GROUP:
            res = this.groupGet(key);
            break;
        case TYPE_SERVERTYPE:
            res = this.serverTypeGet(key);
            break;
        case TYPE_SERVICETYPE:
            res = this.serviceTypeGet(key);
            break;
        case TYPE_PLATFORMTYPE:
            res = this.platformTypeGet(key);
            break;
        case TYPE_DERIVEDMEASUREMENT:
            res = this.derivedMeasurementGet(key);
            break;
        case TYPE_MEASUREMENT:
            res = this.measurementGet(key);
            break;
        case TYPE_BASELINE:
            res = this.baselineGet(key);
            break;
        case TYPE_MEASUREMENTTEMPLATE:
            res = this.measurementTemplateGet(key);
            break;
        case TYPE_ALERT:
            res = this.alertGet(key);
            break;
        case TYPE_AIPLATFORM:
            res = this.aiplatformGet(key);
            break;
        case TYPE_AISCHEDULE:
            res = this.aischeduleGet(key);
            break;
        case TYPE_AIHISTORY:
            res = this.aihistoryGet(key);
            break;
        case TYPE_AGENT:
            res = this.agentGet(key);
            break;
        case TYPE_CPROPKEY:
            res = this.cpropKeyGet(key);
            break;
        case TYPE_STRING:
            res = simpleString;
            break;
        case TYPE_GROUP_METRICS:
            res = this.groupMetricGet(key);
            break;
        default:
            throw new IllegalArgumentException("This should never happen");
        }
        return res;
    }

    String get(int key){
        Object res;

        if((res = this.getValue(key)) == null)
            return null;

        if (res instanceof Object[]) {
            // Use java util to convert
            res = Arrays.asList((Object[]) res);
        }

        return res.toString();
    }

    Integer getInt(int key){
        Object res;
        
        if((res = this.getValue(key)) == null)
            return null;

        if(res instanceof Integer)
            return (Integer)res;
        return Integer.valueOf(res.toString());
    }

    Long getLong(int key){
        Object res;
        
        if((res = this.getValue(key)) == null)
            return null;

        if(res instanceof Date)
            return new Long(((Date)res).getTime());

        if(res instanceof Long)
            return (Long)res;
        return Long.valueOf(res.toString());
    }

    Double getDouble(int key){
        Object res;
        
        if((res = this.getValue(key)) == null)
            return null;

        if(res instanceof Double)
            return (Double)res;
        return Double.valueOf(res.toString());
    }

    String getLongDate(int key){
        DateFormat dateFmt;
        Date date;
        Long val;

        dateFmt = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                                                 DateFormat.MEDIUM);
        val  = this.getLong(key);
        if (val == null)
            return null;

        date = new Date(val.longValue());
        return dateFmt.format(date);
    }

    protected String getDuration(long duration){
        return StringUtil.formatDuration(duration);
    }

    private Object platformGet(int key){
        switch(key){
        case ATTR_DESCRIPTION:
            return this.platformValue.getDescription();
        case ATTR_COMMENT:
            return this.platformValue.getCommentText();
        case ATTR_MODIFIEDBY:
            return this.platformValue.getModifiedBy();
        case ATTR_OWNER:
            return this.platformValue.getOwner();
        case ATTR_LOCATION:
            return this.platformValue.getLocation();
        case ATTR_CPUCOUNT:
            return this.platformValue.getCpuCount();
        case ATTR_FQDN:
            return this.platformValue.getFqdn();
        case ATTR_NAME:
            return this.platformValue.getName();
        case ATTR_ID:
            return this.platformValue.getId();
        case ATTR_MTIME:
            return this.platformValue.getMTime();
        case ATTR_CTIME:
            return this.platformValue.getCTime();
        case ATTR_TYPENAME:
            try {
                String name = null;
                PlatformValue pv= this.platformValue;
                PlatformTypeValue ptv = pv.getPlatformType();
                name = ptv.getName();
                return name;
            } catch ( RuntimeException e ) {
                throw e;
            }

        case ATTR_ADDRESS:
            return this.platformValue.getAgent().getAddress() + ":" +
                this.platformValue.getAgent().getPort();
        case ATTR_AGENT_VERSION:
            String version = this.platformValue.getAgent().getVersion();
            if (version == null)
                return "UNKNOWN";//Backwards compat
            return version;
        case ATTR_AGENT_MTIME:
            return this.platformValue.getAgent().getMTime();
        }
        throw new IllegalArgumentException("Unknown platform attribute: "+key);
    }

    private Object serverGet(int key){
        switch(key){
        case ATTR_NAME:
            return this.serverValue.getName();
        case ATTR_DESCRIPTION:
            return this.serverValue.getDescription();
        case ATTR_MODIFIEDBY:
            return this.serverValue.getModifiedBy();
        case ATTR_OWNER:
            return this.serverValue.getOwner();
        case ATTR_LOCATION:
            return this.serverValue.getLocation();
        case ATTR_INSTALLPATH:
            return this.serverValue.getInstallPath();
        case ATTR_ID:
            return this.serverValue.getId();
        case ATTR_MTIME:
            return this.serverValue.getMTime();
        case ATTR_CTIME:
            return this.serverValue.getCTime();
        case ATTR_TYPENAME:
            return this.serverValue.getServerType().getName();
        }
        throw new IllegalArgumentException("Unknown server attribute: "+key);
    }

    private Object serviceGet(int key){
        switch(key){
        case ATTR_DESCRIPTION:
            return this.serviceValue.getDescription();
        case ATTR_MODIFIEDBY:
            return this.serviceValue.getModifiedBy();
        case ATTR_OWNER:
            return this.serviceValue.getOwner();
        case ATTR_LOCATION:
            return this.serviceValue.getLocation();
        case ATTR_NAME:
            return this.serviceValue.getName();
        case ATTR_ID:
            return this.serviceValue.getId();
        case ATTR_MTIME:
            return this.serviceValue.getMTime();
        case ATTR_CTIME:
            return this.serviceValue.getCTime();
        case ATTR_TYPENAME:
            return this.serviceValue.getServiceType().getName();
        case ATTR_PARENTID:
            return this.serviceValue.getParentId();
        }
        throw new IllegalArgumentException("Unknown service attribute: "+key);
    }

    private Object applicationGet(int key){
        switch(key){
        case ATTR_NAME:
            return this.applicationValue.getName();
        case ATTR_DESCRIPTION:
            return this.applicationValue.getDescription();
        case ATTR_MODIFIEDBY:
            return this.applicationValue.getModifiedBy();
        case ATTR_OWNER:
            return this.applicationValue.getOwner();
        case ATTR_LOCATION:
            return this.applicationValue.getLocation();
        case ATTR_ENGCONTACT:
            return this.applicationValue.getEngContact();
        case ATTR_OPSCONTACT:
            return this.applicationValue.getOpsContact();
        case ATTR_BUSINESSCONTACT:
            return this.applicationValue.getBusinessContact();
        case ATTR_ID:
            return this.applicationValue.getId();
        case ATTR_MTIME:
            return this.applicationValue.getMTime();
        case ATTR_CTIME:
            return this.applicationValue.getCTime();
        }
        throw new IllegalArgumentException("Unknown application attribute: " +
                                           key);
    }

    private Object groupGet(int key) {
        switch (key) {
        case ATTR_ID:
            return this.groupValue.getId();
        case ATTR_NAME:
            return this.groupValue.getName();
        case ATTR_TYPENAME:
            return this.groupValue.getGroupTypeLabel();
        case ATTR_DESCRIPTION:
            return this.groupValue.getDescription();
        case ATTR_MODIFIEDBY:
            return this.groupValue.getModifiedBy();
        case ATTR_OWNER:
            return this.groupValue.getOwner();
        case ATTR_MTIME:
            return this.groupValue.getMTime();
        case ATTR_CTIME:
            return this.groupValue.getCTime();
        }
        throw new IllegalArgumentException("Unknown group attribute: " + key);
    }

    private Object serverTypeGet(int key){
        switch(key){
        case ATTR_NAME:
            return this.serverTypeValue.getName();
        case ATTR_DESCRIPTION:
            return this.serverTypeValue.getDescription();
        case ATTR_PLUGIN:
            return this.serverTypeValue.getPlugin();
        case ATTR_ID:
            return this.serverTypeValue.getId();
        case ATTR_MTIME:
            return this.serverTypeValue.getMTime();
        case ATTR_CTIME:
            return this.serverTypeValue.getCTime();
        }
        throw new IllegalArgumentException("Unknown serverType attribute: " +
                                           key);
    }

    private Object serviceTypeGet(int key){
        switch(key){
        case ATTR_NAME:
            return this.serviceTypeValue.getName();
        case ATTR_DESCRIPTION:
            return this.serviceTypeValue.getDescription();
        case ATTR_PLUGIN:
            return this.serviceTypeValue.getPlugin();
        case ATTR_ID:
            return this.serviceTypeValue.getId();
        case ATTR_MTIME:
            return this.serviceTypeValue.getMTime();
        case ATTR_CTIME:
            return this.serviceTypeValue.getCTime();
        }
        throw new IllegalArgumentException("Unknown serviceType attribute: " +
                                           key);
    }

    private Object platformTypeGet(int key){
        switch(key){
        case ATTR_NAME:
            return this.platformTypeValue.getName();
        case ATTR_PLUGIN:
            return this.platformTypeValue.getPlugin();
        case ATTR_ID:
            return this.platformTypeValue.getId();
        case ATTR_MTIME:
            return this.platformTypeValue.getMTime();
        case ATTR_CTIME:
            return this.platformTypeValue.getCTime();
        }
        throw new IllegalArgumentException("Unknown platformType attribute: " +
                                           key);
    }

    private Object derivedMeasurementGet(int key){
        switch(key){
        case ATTR_ID:
            return this.derivedMeasurementValue.getId();
        case ATTR_INTERVAL:
            return new Long(this.derivedMeasurementValue.getInterval() / 1000);
        case ATTR_INSTANCEID:
            return this.derivedMeasurementValue.getInstanceId();
        case ATTR_MINEXPECTED:
            if (this.derivedMeasurementValue.getBaseline() == null)
                return null;
            return this.derivedMeasurementValue.getBaseline()
                       .getMinExpectedValue();
        case ATTR_MAXEXPECTED:
            if (this.derivedMeasurementValue.getBaseline() == null)
                return null;
            return this.derivedMeasurementValue.getBaseline().
                       getMaxExpectedValue();
        case ATTR_TEMPLATENAME:
            return this.derivedMeasurementValue.getTemplate().getName();
        case ATTR_TEMPLATEALIAS:
            return this.derivedMeasurementValue.getTemplate().getAlias();
        case ATTR_TEMPLATEUNITS:
            return this.derivedMeasurementValue.getTemplate().getUnits();
        case ATTR_ENABLED:
            return new Boolean(this.derivedMeasurementValue.getEnabled());
        }
        throw new IllegalArgumentException("Unknown derivedMeasurement " +
                                           "attribute: " + key);
    }

    private Object measurementGet(int key){
        DerivedMeasurementValue dAux = null;

        if(this.auxData != null && 
           this.auxData instanceof DerivedMeasurementValue)
        {
            dAux  = (DerivedMeasurementValue)this.auxData;
        }

        switch(key){
        case ATTR_TIME:
            return new Long(this.measurementValue.getTimestamp());
        case ATTR_DATA:
            if(dAux != null){
                double value;
                String units;

                value = this.measurementValue.getValue();
                units = dAux.getTemplate().getUnits();
                return UnitsConvert.convert(value, units,
                                            Locale.getDefault());
            } else {
                return this.measurementValue.toString();
            }
        }
        throw new IllegalArgumentException("Unknown measurement attribute: " +
                                           key);
    }

    private Object baselineGet(int key) {
        switch(key){
        case ATTR_ID:
            return this.baselineValue.getId();
        case ATTR_COMPUTETIME:
            return new Long(this.baselineValue.getComputeTime());
        case ATTR_USERENTERED:
            return new Boolean(this.baselineValue.getUserEntered());
        case ATTR_MEAN:
            return this.baselineValue.getMean();
        case ATTR_MINEXPECTED:
            return this.baselineValue.getMinExpectedValue();
        case ATTR_MAXEXPECTED:
            return this.baselineValue.getMaxExpectedValue();
        }
        throw new IllegalArgumentException("Unknown baseline attribute: " +
                                           key);
    }

    private Object measurementTemplateGet(int key){
        switch(key){
        case ATTR_ID:
            return this.measurementTemplateValue.getId();
        case ATTR_NAME:
            return this.measurementTemplateValue.getName();
        case ATTR_TEMPLATE:
            return this.measurementTemplateValue.getTemplate();
        case ATTR_TEMPLATEALIAS:
            return this.measurementTemplateValue.getAlias();
        case ATTR_TEMPLATEUNITS:
            return this.measurementTemplateValue.getUnits();
        case ATTR_PLUGIN:
            return this.measurementTemplateValue.getPlugin();
        }
        throw new IllegalArgumentException("Unknown measurement template " +
                                           "attribute: " + key);
    }

    private Object alertGet(int key) {
        switch(key) {
          case ATTR_ID:
            return this.alertValue.getId();
          case ATTR_CTIME:
            return new Long(this.alertValue.getCtime());
          case ATTR_ACTUAL:
            return this.alertValue.getConditionLogs();
          case ATTR_ACTION:
            return this.alertValue.getActionLogs();
        }
        throw new IllegalArgumentException("Unknown alert attribute: " + key);
    }

    private Object aiplatformGet(int key) {
        int qstat;
        long diff;
        String diffString;
        switch(key) {
        case ATTR_ID:
            return this.aiplatformValue.getId();
        case ATTR_CTIME:
            return this.aiplatformValue.getCTime();
        case ATTR_MTIME:
            return this.aiplatformValue.getMTime();
        case ATTR_QUEUESTATUS:
            qstat = this.aiplatformValue.getQueueStatus();
            return AIQueueConstants.getQueueStatusString(qstat);
        case ATTR_DIFF:
            diff = this.aiplatformValue.getDiff();
            qstat = this.aiplatformValue.getQueueStatus();
            return AIQueueConstants.getPlatformDiffString(qstat, diff);
        case ATTR_IGNORED:
            return String.valueOf(this.aiplatformValue.getIgnored());
        case ATTR_CERTDN:
            return this.aiplatformValue.getCertdn();
        case ATTR_FQDN:
            return this.aiplatformValue.getFqdn();
        case ATTR_NAME:
            return this.aiplatformValue.getName();
        }
        throw new IllegalArgumentException("Unknown aiplatform attribute: " + 
                                           key);
    }

    private Object aischeduleGet(int key) {
        switch(key) {
        case ATTR_ID:
            return this.aischeduleValue.getId();
        case ATTR_ENTITY_ID:
            return this.aischeduleValue.getEntityId();
        case ATTR_ENTITY_TYPE:
            return this.aischeduleValue.getEntityType();
        case ATTR_SUBJECT:
            return this.aischeduleValue.getSubject();
        case ATTR_NEXTFIRE:
            return new Long(this.aischeduleValue.getNextFireTime());
        case ATTR_DESCRIPTION:
            return this.aischeduleValue.getScanDesc();
        case ATTR_SCHEDULE_STRING:
            return this.aischeduleValue.getScheduleValue().
                getScheduleString();
        }
        throw new IllegalArgumentException("Unknown aischedule attribute: " + 
                                           key);
    }

    private Object aihistoryGet(int key)
    {
        switch(key) {
          case ATTR_ID:
            return this.aihistoryValue.getId();
          case ATTR_ENTITY_ID:
            return this.aihistoryValue.getEntityId();
          case ATTR_ENTITY_TYPE:
            return this.aihistoryValue.getEntityType();
          case ATTR_NAME:
            return this.aihistoryValue.getEntityName();
          case ATTR_SUBJECT:
            return this.aihistoryValue.getSubject();
          case ATTR_SCHEDULED:
            return this.aihistoryValue.getScheduled();
          case ATTR_DATE_SCHEDULED:
            return new Long(this.aihistoryValue.getDateScheduled());
          case ATTR_STARTTIME:
            return new Long(this.aihistoryValue.getStartTime());
          case ATTR_ENDTIME:
            return new Long(this.aihistoryValue.getEndTime());
          case ATTR_DURATION:
            return getDuration(this.aihistoryValue.getDuration());
          case ATTR_MESSAGE:
            return this.aihistoryValue.getMessage();
          case ATTR_STATUS:
            return this.aihistoryValue.getStatus();
          case ATTR_DESCRIPTION:
            return this.aihistoryValue.getScanDesc();
          case ATTR_MESSAGEORDESC:
            return (this.aihistoryValue.getMessage() != null) ?
                this.aihistoryValue.getMessage() :
                this.aihistoryValue.getScanDesc();
        }
        throw new IllegalArgumentException("Unknown autoinventory history " +
                                           "attribute: " + key);
    }

    private Object agentGet(int key){
        switch(key){
        case ATTR_ID:
            return this.agentValue.getId();
        case ATTR_ADDRESS:
            return this.agentValue.getAddress() + ":" +
                this.agentValue.getPort();
        case ATTR_AUTHTOKEN:
            return this.agentValue.getAuthToken();
        case ATTR_CTIME:
            return this.agentValue.getCTime();
        case ATTR_MTIME:
            return this.agentValue.getMTime();
        }
        throw new IllegalArgumentException("Unknown agent attribute: " + key);
    }

    private Object cpropKeyGet(int key) {
        switch(key) {
        case ATTR_NAME:
            return this.cpropKeyValue.getKey();
        case ATTR_DESCRIPTION:
            return this.cpropKeyValue.getDescription();
        }
        throw new IllegalArgumentException("Unknown cpropKey attribute: " + 
                                           key);
    }


    private Object groupMetricGet(int key) {
        switch(key) {
        case ATTR_ID:
            return new Integer(this.groupMetric.getId());
        case ATTR_INTERVAL:
            long interval = this.groupMetric.getInterval();
            if (interval == 0) {
                return "VARIES";
            }
            return new Long(interval / 1000);
        case ATTR_NAME:
            return this.groupMetric.getName();
        case ATTR_DESCRIPTION:
            return this.groupMetric.getDescription();
        case ATTR_CATEGORY:
            return this.groupMetric.getCategory();
        case ATTR_ACTIVE_MEMBERS:
            return new Integer(this.groupMetric.getActiveMembers());
        case ATTR_TOTAL_MEMBERS:
            return new Integer(this.groupMetric.getTotalMembers());
        }
         
        throw new IllegalArgumentException("Unknown GroupMetricsDisplay "
                                           + "attribute: " + key);
    }
}
