package org.hyperic.hq.ui.action.resource.common.monitor.visibility;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.bizapp.shared.uibeans.MetricDisplaySummary;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.json.JSONException;
import org.json.JSONObject;

public class IndicatorDisplaySummary
extends MetricDisplaySummary implements Serializable {
public static final String DELIMITER = ",";

private AppdefEntityID entityId = null;
private AppdefEntityTypeID childType = null;
private List<HighLowMetricValue> highLowMetrics = new ArrayList<HighLowMetricValue>();

/** Constructor using a summary with data
 * @param summary the actual data
 */
public IndicatorDisplaySummary(MetricDisplaySummary summary, List<HighLowMetricValue> data) {
   this(summary);
   this.highLowMetrics = data;
}

/**
 * Constructor using a summary with data
 * @param summary the actual data
 */
public IndicatorDisplaySummary(MetricDisplaySummary summary) {
    super();
    super.setAlertCount(summary.getAlertCount());
    super.setOobCount(summary.getOobCount());
    super.setBeginTimeFrame(summary.getBeginTimeFrame());
    super.setCollectionType(summary.getCollectionType());
    super.setDesignated(summary.getDesignated());
    super.setDisplayUnits(summary.getDisplayUnits());
    super.setEndTimeFrame(summary.getEndTimeFrame());
    super.setLabel(summary.getLabel());
    super.setMetrics(summary.getMetrics());
    super.setMetricSource(summary.getMetricSource());
    super.setShowNumberCollecting(summary.getShowNumberCollecting());
    super.setTemplateCat(summary.getTemplateCat());
    super.setTemplateId(summary.getTemplateId());
    super.setUnits(summary.getUnits());
    super.setAvailDown(summary.getAvailDown());
    super.setAvailUnknown(summary.getAvailUnknown());
    super.setAvailUp(summary.getAvailUp());
}

protected IndicatorDisplaySummary(String token) {
    StringTokenizer st = new StringTokenizer(token, DELIMITER);
    boolean autogroup = st.countTokens() > 2;

    this.entityId = new AppdefEntityID(st.nextToken());
    this.setTemplateId(new Integer(st.nextToken()));

    if (autogroup) {
        this.childType = new AppdefEntityTypeID(st.nextToken());
    }
}

public AppdefEntityTypeID getChildType() {
    return childType;
}

public void setChildType(AppdefEntityTypeID childType) {
    this.childType = childType;
}

public AppdefEntityID getEntityId() {
    return entityId;
}

public void setEntityId(AppdefEntityID entityId) {
    this.entityId = entityId;
}

public int getUnitUnits() {
    return UnitsConvert.getUnitForUnit(getUnits());
}

public int getUnitScale() {
    return UnitsConvert.getScaleForUnit(getUnits());
}

public List<HighLowMetricValue> getHighLowMetrics() {
    return highLowMetrics;
}

public String toString() {
    StringBuffer strBuf = new StringBuffer(getEntityId().getAppdefKey()).append(DELIMITER).append(
        getTemplateId());

    if (getChildType() != null) {
        strBuf.append(DELIMITER).append(getChildType().getAppdefKey());
    }

    return strBuf.toString();
}

public JSONObject toJSON() {
    JSONObject json = new JSONObject();
    try {                
        json.put("entityId", getEntityId().toString());
        json.put("entityType", getEntityId().getType());
        
        if (getChildType() != null) {
            json.put("ctype", getChildType().toString());
        }
        
        json.put("metricId", getTemplateId());
        json.put("metricLabel", getLabel());
        json.put("metricSource", getMetricSource());
        json.put("minMetric", getMinMetric().getValueFmt().toString());
        json.put("avgMetric", getAvgMetric().getValueFmt().toString());
        json.put("maxMetric", getMaxMetric().getValueFmt().toString());
        json.put("unitUnits", getUnitUnits());
        json.put("unitScale", getUnitScale());
   } catch (JSONException e) {
        throw new SystemException(e);
    }
    return json;
}
}
