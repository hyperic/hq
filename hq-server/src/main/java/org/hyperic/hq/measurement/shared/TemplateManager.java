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
package org.hyperic.hq.measurement.shared;

import java.util.List;
import java.util.Map;

import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.measurement.TemplateNotFoundException;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.MeasurementTemplateSortField;
import org.hyperic.hq.measurement.server.session.MonitorableMeasurementInfo;
import org.hyperic.hq.measurement.server.session.MonitorableType;
import org.hyperic.hq.product.MeasurementInfo;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.util.pager.PageControl;

/**
 * Local interface for TemplateManager.
 */
public interface TemplateManager {
    /**
     * Get a MeasurementTemplate
     */
    public MeasurementTemplate getTemplate(Integer id);

    /**
     * Look up measurement templates for an array of template IDs
     */
    public List<MeasurementTemplate> getTemplates(List<Integer> ids);

    /**
     * Look up a measurement templates for an array of template IDs
     * @throws TemplateNotFoundException if no measurement templates are found.
     * @return a MeasurementTemplate value
     */
    public List<MeasurementTemplate> getTemplates(Integer[] ids, PageControl pc)
        throws TemplateNotFoundException;

    /**
     * Get all the templates. Must be superuser to execute.
     * @param pInfo must contain a sort field of type
     *        {@link MeasurementTemplateSortField}
     * @param defaultOn If non-null, return templates with defaultOn ==
     *        defaultOn
     * @return a list of {@link MeasurementTemplate}s
     */
    public List<MeasurementTemplate> findTemplates(AuthzSubject user, PageInfo pInfo,
                                                   Boolean defaultOn) throws PermissionException;

    /**
     * Get all templates for a given MonitorableType
     * @param pInfo must contain a sort field of type
     *        {@link MeasurementTemplateSortField}
     * @param defaultOn If non-null, return templates with defaultOn ==
     *        defaultOn
     * @return a list of {@link MeasurementTemplate}s
     */
    public List<MeasurementTemplate> findTemplatesByMonitorableType(AuthzSubject user,
                                                                    PageInfo pInfo, String type,
                                                                    Boolean defaultOn)
        throws PermissionException;

    /**
     * Look up a measurement templates for a monitorable type and category.
     * @return a MeasurementTemplate value
     */
    public List<MeasurementTemplate> findTemplates(String type, String cat, Integer[] excludeIds,
                                                   PageControl pc);

    /**
     * Look up a measurement templates for a monitorable type and filtered by
     * categories and keyword.
     * @return a MeasurementTemplate value
     */
    public List<MeasurementTemplate> findTemplates(String type, long filters, String keyword);

    /**
     * Look up a measurement template IDs for a monitorable type.
     * @return an array of ID values
     */
    public Integer[] findTemplateIds(String type);

    public List<MeasurementTemplate> findTemplatesByName(List<String> tmpNames);

    /**
     * Update the default interval for a list of meas. templates
     * @subject - the subject
     * @param templIds - a list of integer template ids
     * @param interval - the interval of collection to set to
     */
    public void updateTemplateDefaultInterval(AuthzSubject subject, Integer[] templIds,
                                              long interval);

    /**
     * Make metrics disabled by default for a list of meas. templates
     * @param templIds - a list of integer template ids
     */
    public void setTemplateEnabledByDefault(AuthzSubject subject, Integer[] templIds, boolean on);

    /**
     * Update measurement templates for a given entity. This still needs some
     * refactoring.
     * @return A map of measurement info's that are new and will need to be
     *         created.
     */
    public Map<String, MeasurementInfo> updateTemplates(String pluginName, TypeInfo ownerEntity,
                                                        MonitorableType monitorableType,
                                                        MeasurementInfo[] tmpls);

    /**
     * Add new measurement templates for a plugin. This does a batch style
     * insert
     */
    public void createTemplates(String pluginName, Map<MonitorableType,List<MonitorableMeasurementInfo>> toAdd);

    public void setDesignated(MeasurementTemplate tmpl, boolean designated);

    /**
     * Set the measurement templates to be "designated" for a monitorable type.
     */
    public void setDesignatedTemplates(String mType, Integer[] desigIds);

    public MonitorableType createMonitorableType(String pluginName, TypeInfo info);

    public Map<String, MonitorableType> getMonitorableTypesByName(String pluginName);

}
