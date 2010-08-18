/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
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

package org.hyperic.hq.measurement.server.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.shared.TemplateManager;
import org.hyperic.hq.product.MeasurementInfo;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
/**
 * Integration test of the {@link TemplateManagerImpl}
 * @author jhickey
 *
 */
@DirtiesContext
public class TemplateManagerTest
    extends BaseInfrastructureTest {

    @Autowired
    private TemplateManager templateManager;

    @Autowired
    private CategoryDAO categoryDAO;

    @Test
    public void testCreateTemplates() {
        MonitorableType monitorableType = templateManager.createMonitorableType("tomcat",
            new ServerTypeInfo("tomcat server", "A server of tomcat", "1.0"));
        flushSession();
        
        MeasurementInfo measurementInfo = new MeasurementInfo();
        Map<String, Object> measAttributes = new HashMap<String, Object>();
        measAttributes.put(MeasurementInfo.ATTR_ALIAS, "queueSize");
        measAttributes.put(MeasurementInfo.ATTR_CATEGORY, MeasurementConstants.CAT_UTILIZATION);
        measAttributes.put(MeasurementInfo.ATTR_COLLECTION_TYPE, "dynamic");
        measAttributes.put(MeasurementInfo.ATTR_DEFAULTON, "true");
        measAttributes.put(MeasurementInfo.ATTR_INDICATOR, "true");
        measAttributes.put(MeasurementInfo.ATTR_INTERVAL, "1234");
        measAttributes.put(MeasurementInfo.ATTR_NAME, "Queue Size");
        measAttributes.put(MeasurementInfo.ATTR_TEMPLATE, "service:queueSize");
        measAttributes.put(MeasurementInfo.ATTR_UNITS, "messages");
        measurementInfo.setAttributes(measAttributes);

        MeasurementInfo measurementInfo2 = new MeasurementInfo();
        Map<String, Object> measAttributes2 = new HashMap<String, Object>();
        measAttributes2.put(MeasurementInfo.ATTR_ALIAS, "responseTime");
        measAttributes2.put(MeasurementInfo.ATTR_CATEGORY, MeasurementConstants.CAT_PERFORMANCE);
        measAttributes2.put(MeasurementInfo.ATTR_COLLECTION_TYPE, "dynamic");
        measAttributes2.put(MeasurementInfo.ATTR_DEFAULTON, "true");
        measAttributes2.put(MeasurementInfo.ATTR_INDICATOR, "true");
        measAttributes2.put(MeasurementInfo.ATTR_INTERVAL, "6789");
        measAttributes2.put(MeasurementInfo.ATTR_NAME, "Response Time");
        measAttributes2.put(MeasurementInfo.ATTR_TEMPLATE, "service:responseTime");
        measAttributes2.put(MeasurementInfo.ATTR_UNITS, "ms");
        measurementInfo2.setAttributes(measAttributes2);

        MonitorableMeasurementInfo meas1 = new MonitorableMeasurementInfo(monitorableType,
            measurementInfo);
        MonitorableMeasurementInfo meas2 = new MonitorableMeasurementInfo(monitorableType,
            measurementInfo2);
        List<MonitorableMeasurementInfo> measInfos = new ArrayList<MonitorableMeasurementInfo>();
        measInfos.add(meas1);
        measInfos.add(meas2);
        Map<MonitorableType,List<MonitorableMeasurementInfo>> typeToInfos = new HashMap<MonitorableType,List<MonitorableMeasurementInfo>>();
        typeToInfos.put(monitorableType, measInfos);
        
        templateManager.createTemplates("tomcat", typeToInfos);

        MeasurementTemplate expQueueSize = new MeasurementTemplate("Queue Size", "queueSize",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", monitorableType, categoryDAO
                .findByName(MeasurementConstants.CAT_UTILIZATION), "tomcat");

        MeasurementTemplate expRT = new MeasurementTemplate("Response Time", "responseTime", "ms",
            MeasurementConstants.COLL_TYPE_DYNAMIC, true, 6789, true, "service:responseTime",
            monitorableType, categoryDAO.findByName(MeasurementConstants.CAT_PERFORMANCE), "tomcat");

        Integer[] templateIds = templateManager.findTemplateIds(monitorableType.getName());
        MeasurementTemplate template = templateManager.getTemplate(templateIds[0]);
        if ("queueSize".equals(template.getAlias())) {
            validateTemplate(expQueueSize, template);
        } else if ("responseTime".equals(template.getAlias())) {
            validateTemplate(expRT, template);
        } else {
            fail("Unexpected template alias " + template.getAlias());
        }
        assertEquals(2, templateIds.length);
    }

    private void validateTemplate(MeasurementTemplate expected, MeasurementTemplate actual) {
        assertEquals(expected.getAlias(), actual.getAlias());
        assertEquals(expected.getCategory(), actual.getCategory());
        assertEquals(expected.getCollectionType(), actual.getCollectionType());
        assertEquals(expected.getDefaultInterval(), actual.getDefaultInterval());
        assertEquals(expected.getMonitorableType(), actual.getMonitorableType());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getPlugin(), actual.getPlugin());
        assertEquals(expected.getTemplate(), actual.getTemplate());
        assertEquals(expected.getUnits(), actual.getUnits());
        assertNotNull(actual.getCtime());
        assertNotNull(actual.getMtime());
    }

}
