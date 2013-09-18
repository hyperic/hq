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
package org.hyperic.hq.bizapp.shared;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.login.LoginException;

import org.hyperic.hq.appdef.shared.AppdefCompatException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.uibeans.MeasurementMetadataSummary;
import org.hyperic.hq.bizapp.shared.uibeans.MeasurementSummary;
import org.hyperic.hq.bizapp.shared.uibeans.MetricConfigSummary;
import org.hyperic.hq.bizapp.shared.uibeans.MetricDisplaySummary;
import org.hyperic.hq.bizapp.shared.uibeans.ProblemMetricSummary;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceDisplaySummary;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceTypeDisplaySummary;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.management.shared.MeasurementInstruction;
import org.hyperic.hq.measurement.MeasurementCreateException;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.TemplateNotFoundException;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * Local interface for MeasurementBoss.
 */
public interface MeasurementBoss {
    
    /**
     * Get the availability average of the resources
     * @param aeIds the id of the resources
     */
    public double getAvailabilityAverage(AppdefEntityID[] aeIds, long begin, long end);

    /**
     * Get the availability average of the auto group. aid is their parent's entity id.
     * @param aid the entity id of their parent
     * @param ctype the cilde type of this auto group
     */
    public double getAGAvailabilityAverage(int sessionId, AppdefEntityID aid, AppdefEntityTypeID ctype, 
                                           long begin, long end) throws AppdefEntityNotFoundException, PermissionException, 
                                           SessionNotFoundException, SessionTimeoutException;  
    /**
     * Get Autogroup member ids
     */
    public AppdefEntityID[] getAutoGroupMemberIDs(AuthzSubject subject, AppdefEntityID[] aids, AppdefEntityTypeID ctype)
        throws AppdefEntityNotFoundException, PermissionException;

    /**
     * Update the default interval for a list of template ids
     */
    public void updateMetricDefaultInterval(int sessionId, Integer[] tids, long interval) throws SessionException;

    /**
     * Update the templates to be indicators or not
     */
    public void updateIndicatorMetrics(int sessionId, AppdefEntityTypeID aetid, Integer[] tids)
        throws TemplateNotFoundException, SessionTimeoutException, SessionNotFoundException;

    public List<MeasurementTemplate> findMeasurementTemplates(int sessionId, AppdefEntityTypeID typeId,
                                                              String category, PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException;

    public List<MeasurementTemplate> findMeasurementTemplates(int sessionId, AppdefEntityID aeid)
        throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException, PermissionException;

    /**
     * Retrieve list of measurement templates applicable to a monitorable type
     * @param mtype the monitorableType
     * @return a List of MeasurementTemplateValue objects
     */
    public List<MeasurementTemplate> findMeasurementTemplates(int sessionId, String mtype, PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException;

    /**
     * Retrieve list of measurement templates given specific IDs
     */
    public List<MeasurementTemplate> findMeasurementTemplates(String user, Integer[] ids, PageControl pc)
        throws LoginException, ApplicationException, ConfigPropertyException;

    /**
     * Retrieve list of measurement templates given specific IDs
     * @return a List of MeasurementTemplateValue objects
     */
    public List<MeasurementTemplate> findMeasurementTemplates(int sessionId, Integer[] ids, PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException, TemplateNotFoundException;

    /**
     * Retrieve a measurement template given specific ID
     */
    public MeasurementTemplate getMeasurementTemplate(int sessionId, Integer id) throws SessionNotFoundException,
        SessionTimeoutException, TemplateNotFoundException;

    /**
     * Get the the availability metric template for the given autogroup
     * @return The availabililty metric template.
     */
    public MeasurementTemplate getAvailabilityMetricTemplate(int sessionId, AppdefEntityID aid, AppdefEntityTypeID ctype)
        throws SessionNotFoundException, SessionTimeoutException, MeasurementNotFoundException,
        AppdefEntityNotFoundException, PermissionException;

    /**
     * Get the the availability metric template for the given resource
     * @return template of availabililty metric
     */
    public MeasurementTemplate getAvailabilityMetricTemplate(int sessionId, AppdefEntityID aeid)
        throws MeasurementNotFoundException, SessionNotFoundException, SessionTimeoutException,
        AppdefEntityNotFoundException, PermissionException;

    /**
     * Get the the designated measurement template for the given resource and
     * corresponding category.
     * @return Array of Measurement IDs
     */
    public List<MeasurementTemplate> getDesignatedTemplates(int sessionId, AppdefEntityID id, Set<String> cats)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException, PermissionException;

    /**
     * Get the the designated measurement template for the autogroup given a
     * type and corresponding category.
     * @param ctype the AppdefEntityTypeID of the AG members
     * @return Array of Measuremnt ids
     */
    public List<MeasurementTemplate> getAGDesignatedTemplates(int sessionId, AppdefEntityID[] aids,
                                                              AppdefEntityTypeID ctype, Set<String> cats)
        throws SessionNotFoundException, SessionTimeoutException, MeasurementNotFoundException,
        AppdefEntityNotFoundException, PermissionException;

    /**
     * Update the measurements - set the interval
     * @param id the resource ID
     * @param tids the array of template ID's
     * @param interval the new interval value
     */
    public void updateMeasurements(int sessionId, AppdefEntityID id, Integer[] tids, long interval)
        throws MeasurementNotFoundException, SessionTimeoutException, SessionNotFoundException,
        TemplateNotFoundException, AppdefEntityNotFoundException, GroupNotCompatibleException,
        MeasurementCreateException, ConfigFetchException, PermissionException, EncodingException;

    /**
     * Update measurements for the members of an autogroup
     * @param parentid - the parent resource of the autogroup
     * @param ctype - the type of child resource
     * @param tids - template ids to update
     * @param interval - the interval to set
     */
    public void updateAGMeasurements(int sessionId, AppdefEntityID parentid, AppdefEntityTypeID ctype, Integer[] tids,
                                     long interval) throws MeasurementNotFoundException, SessionTimeoutException,
        SessionNotFoundException, TemplateNotFoundException, AppdefEntityNotFoundException,
        GroupNotCompatibleException, MeasurementCreateException, ConfigFetchException, PermissionException,
        EncodingException;
    
    /**
     * Update resource measurements according to measurement instructions 
     * @throws SessionTimeoutException
     * @throws SessionNotFoundException
     * @throws ConfigFetchException
     * @throws EncodingException
     * @throws PermissionException
     * @throws TemplateNotFoundException
     * @throws AppdefEntityNotFoundException
     * @throws MeasurementCreateException
     */
    public void createMeasurements(AuthzSubject subject, Resource resource, Collection<MeasurementInstruction> measurementInstructions) 
            throws SessionTimeoutException, SessionNotFoundException, ConfigFetchException,
            EncodingException, PermissionException, TemplateNotFoundException,
            AppdefEntityNotFoundException, MeasurementCreateException;    

    /**
     * Disable all measurements for an instance
     * @param id the resource's ID
     */
    public void disableMeasurements(int sessionId, AppdefEntityID id) throws SessionTimeoutException,
        SessionNotFoundException, PermissionException;

    /**
     * Disable all measurements for a resource
     * @param id the resource's ID
     * @param tids the array of measurement ID's
     */
    public void disableMeasurements(int sessionId, AppdefEntityID id, Integer[] tids) throws SessionException,
        AppdefEntityNotFoundException, GroupNotCompatibleException, PermissionException;

    /**
     * Disable all measurements for a resource
     * @param tids the array of measurement ID's
     */
    public void disableAGMeasurements(int sessionId, AppdefEntityID parentId, AppdefEntityTypeID childType,
                                      Integer[] tids) throws SessionTimeoutException, SessionNotFoundException,
        AppdefEntityNotFoundException, GroupNotCompatibleException, PermissionException;

    /**
     * Find a measurement using measurement id
     * @param id measurement id
     */
    public Measurement getMeasurement(int sessionID, Integer id) throws SessionTimeoutException,
        SessionNotFoundException, MeasurementNotFoundException;

    /**
     * Get the last metric values for the given template IDs.
     * @param tids The template IDs to get
     */
    public MetricValue[] getLastMetricValue(int sessionId, AppdefEntityID aeid, Integer[] tids)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException;

    /**
     * Get the last metric data for the array of measurement ids.
     * @param measurements The List of MeasurementIds to get metrics for
     * @param interval The allowable time in ms to go back looking for data.
     */
    public MetricValue[] getLastMetricValue(int sessionId, List<Integer> measurementIds, long interval);

    /**
     * Get the last indicator metric values
     */
    public Map<Integer, MetricValue> getLastIndicatorValues(Integer sessionId, AppdefEntityID aeid);

    /**
     * Retrieve a Measurement for a specific instance
     */
    public Measurement findMeasurement(int sessionId, Integer tid, AppdefEntityID id) throws SessionNotFoundException,
        SessionTimeoutException, PermissionException, MeasurementNotFoundException, AppdefEntityNotFoundException;

    /**
     * Retrieve List of measurements for a specific instance
     * @return List of Measurement objects
     */
    public List findMeasurements(int sessionId, AppdefEntityID id, PageControl pc) throws SessionNotFoundException,
        SessionTimeoutException, AppdefEntityNotFoundException, GroupNotCompatibleException, PermissionException;

    /**
     * Retrieve list of measurements for a specific template and entities
     * @param tid the template ID
     * @param entIds the array of entity IDs
     * @return a List of Measurement objects
     */
    public List<Measurement> findMeasurements(int sessionId, Integer tid, AppdefEntityID[] entIds)
        throws SessionTimeoutException, SessionNotFoundException, MeasurementNotFoundException,
        AppdefEntityNotFoundException, PermissionException;

    /**
     * Get the enabled measurements for an auto group
     * @param parentId - the parent resource appdefEntityID
     * @param childType - the type of child in the autogroup
     * @return a PageList of Measurement objects
     */
    public List<MetricConfigSummary> findEnabledAGMeasurements(int sessionId, AppdefEntityID parentId,
                                                               AppdefEntityTypeID childType, String cat, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException,
        GroupNotCompatibleException, PermissionException;

    /**
     * Retrieve list of measurements for a specific instance and category
     * @return a PageList of Measurement objects
     */
    public PageList<MetricConfigSummary> findEnabledMeasurements(int sessionId, AppdefEntityID id, String cat,
                                                                 PageControl pc) throws SessionNotFoundException,
        SessionTimeoutException, AppdefEntityNotFoundException, GroupNotCompatibleException, PermissionException;

    /**
     * Dumps data for a specific measurement
     * @return a PageList of MetricValue objects
     */
    public PageList<HighLowMetricValue> findMeasurementData(int sessionId, Measurement m, long begin, long end,
                                                            PageControl pc);

    /**
     * Dumps data for a specific measurement template for an instance based on
     * an interval
     * @param tid the template ID
     * @param aid the AppdefEntityID
     * @param begin the beginning of the time range
     * @param end the end of the time range
     * @param interval the time interval at which the data should be calculated
     * @param returnNulls whether or not nulls should be inserted for no data
     * @return a PageList of MetricValue objects
     */
    public PageList<HighLowMetricValue> findMeasurementData(int sessionId, Integer tid, AppdefEntityID aid, long begin,
                                                            long end, long interval, boolean returnNulls, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException, PermissionException,
        MeasurementNotFoundException;

    /**
     * Dumps data for a specific measurement template for an auto-group based on
     * an interval.
     * @param tid the measurement template id
     * @param aid the entity id
     * @param ctype the auto-group child type
     * @param begin start of interval
     * @param end end of interval
     * @param interval the interval
     * @param returnNulls whether or not to return nulls
     * @return a PageList of MetricValue objects
     */
    public PageList<HighLowMetricValue> findMeasurementData(int sessionId, Integer tid, AppdefEntityID aid,
                                                            AppdefEntityTypeID ctype, long begin, long end,
                                                            long interval, boolean returnNulls, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException, PermissionException,
        MeasurementNotFoundException;

    /**
     * Dumps data for a specific measurement template for an auto-group based on
     * an interval.
     * @param tid the measurement template id
     * @param begin start of interval
     * @param end end of interval
     * @param interval the interval
     * @param returnNulls whether or not to return nulls associated with the
     *        platform
     * @return a PageList of MetricValue objects
     */
    public PageList<HighLowMetricValue> findMeasurementData(int sessionId, Integer tid, List<AppdefEntityID> entIds,
                                                            long begin, long end, long interval, boolean returnNulls,
                                                            PageControl pc) throws SessionNotFoundException,
        SessionTimeoutException, AppdefEntityNotFoundException, PermissionException;

    /**
     * Dumps data for a specific measurement template for an instance based on
     * an interval
     * @param aid the AppdefEntityID
     * @param begin the beginning of the time range
     * @param end the end of the time range
     * @param interval the time interval at which the data should be calculated
     * @param returnNulls whether or not nulls should be inserted for no data
     * @return a PageList of MetricValue objects
     */
    public PageList<HighLowMetricValue> findMeasurementData(String user, AppdefEntityID aid, MeasurementTemplate tmpl,
                                                            long begin, long end, long interval, boolean returnNulls,
                                                            PageControl pc) throws LoginException,
        ApplicationException, ConfigPropertyException;

    /**
     * Dumps data for a specific measurement template for an instance based on
     * an interval
     * @param aid the AppdefEntityID
     * @param tmpl the complete MeasurementTemplate value object
     * @param begin the beginning of the time range
     * @param end the end of the time range
     * @param interval the time interval at which the data should be calculated
     * @param returnNulls whether or not nulls should be inserted for no data
     * @return a PageList of MetricValue objects
     */
    public PageList<HighLowMetricValue> findMeasurementData(int sessionId, AppdefEntityID aid,
                                                            MeasurementTemplate tmpl, long begin, long end,
                                                            long interval, boolean returnNulls, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException, PermissionException,
        MeasurementNotFoundException;

    /**
     * Dumps data for a specific measurement template for an auto-group based on
     * an interval.
     * @param ctype the auto-group child type
     * @param begin start of interval
     * @param end end of interval
     * @param interval the interval
     * @param returnNulls whether or not to return nulls
     * @return a PageList of MetricValue objects
     * @throws ConfigPropertyException
     * @throws ApplicationException
     * @throws LoginException
     */
    public PageList<HighLowMetricValue> findAGMeasurementData(String user, AppdefEntityID[] aids,
                                                              MeasurementTemplate tmpl, AppdefEntityTypeID ctype,
                                                              long begin, long end, long interval, boolean returnNulls,
                                                              PageControl pc) throws LoginException,
        ApplicationException, ConfigPropertyException;

    /**
     * Dumps data for a specific measurement template for an auto-group based on
     * an interval.
     * @param ctype the auto-group child type
     * @param begin start of interval
     * @param end end of interval
     * @param interval the interval
     * @param returnNulls whether or not to return nulls
     * @return a PageList of MetricValue objects
     */
    public PageList<HighLowMetricValue> findAGMeasurementData(int sessionId, AppdefEntityID[] aids,
                                                              MeasurementTemplate tmpl, AppdefEntityTypeID ctype,
                                                              long begin, long end, long interval, boolean returnNulls,
                                                              PageControl pc) throws SessionNotFoundException,
        SessionTimeoutException, AppdefEntityNotFoundException, PermissionException, MeasurementNotFoundException;

    /**
     * Returns metadata for particular measurement
     */
    public List<MeasurementMetadataSummary> findMetricMetadata(int sessionId, AppdefEntityID aid,
                                                               AppdefEntityTypeID ctype, Integer tid)
        throws SessionNotFoundException, SessionTimeoutException, GroupNotCompatibleException,
        AppdefEntityNotFoundException, ApplicationNotFoundException, TemplateNotFoundException, PermissionException;

    List<ProblemMetricSummary> findAllMetrics(int sessionId, AppdefEntityID aeid, long begin, long end)
        throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException, PermissionException,
        AppdefCompatException, InvalidAppdefTypeException;

    List<ProblemMetricSummary> findAllMetrics(int sessionId, AppdefEntityID[] aeids, long begin, long end)
        throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException, PermissionException,
        AppdefCompatException, InvalidAppdefTypeException;

    double[] getAvailability(AuthzSubject subject, AppdefEntityID[] ids) throws AppdefEntityNotFoundException,
        PermissionException;

    List<AppdefEntityID> getAGMemberIds(AuthzSubject subject, AppdefEntityID parentAid, AppdefEntityTypeID ctype)
        throws AppdefEntityNotFoundException, PermissionException;

    public MetricDisplaySummary getMetricDisplaySummary(MeasurementTemplate tmpl, Long interval, long begin, long end,
                                                        double[] data, int totalConfigured);

    Map<String, Set<MetricDisplaySummary>> findMetrics(int sessionId, AppdefEntityID entId, long begin, long end,
                                                       PageControl pc) throws SessionTimeoutException,
        SessionNotFoundException, InvalidAppdefTypeException, PermissionException, AppdefEntityNotFoundException,
        AppdefCompatException;

    /**
     * Method findMetrics. When the entId is a server, return all of the metrics
     * that are instances of the measurement templates for the server's type. In
     * this case, the MetricDisplaySummary's attributes to show the number
     * collecting doesn't make sense; showNumberCollecting should false for each
     * bean.
     * <p>
     * When the entId is a platform, return all of the metrics that are
     * instances of the measurement templates for the platform's type. In this
     * case, the MetricDisplaySummary's attributes to show the number collecting
     * doesn't make sense; showNumberCollecting should false for each bean.
     * </p>
     * <p>
     * When the entId is compatible group of servers or platforms, return all of
     * the metrics for the type. Each MetricDisplaySummary actually represents
     * the metrics summarized for all of the group members (cumulative/averaged
     * as appropriate), showNumberCollecting should be true and the
     * numberCollecting as well as the total number of members assigned in each
     * bean.
     * </p>
     * @return Map keyed on the category (String), values are List's of
     *         MetricDisplaySummary beans
     * @see MetricDisplaySummary
     */
    public MetricDisplaySummary findMetric(int sessionId, AppdefEntityID aeid, AppdefEntityTypeID ctype, Integer tid,
                                           long begin, long end) throws SessionTimeoutException,
        SessionNotFoundException, PermissionException, AppdefEntityNotFoundException, AppdefCompatException,
        MeasurementNotFoundException;

    /**
     * Method findMetrics. When the entId is a server, return all of the metrics
     * that are instances of the measurement templates for the server's type. In
     * this case, the MetricDisplaySummary's attributes to show the number
     * collecting doesn't make sense; showNumberCollecting should false for each
     * bean.
     * <p>
     * When the entId is a platform, return all of the metrics that are
     * instances of the measurement templates for the platform's type. In this
     * case, the MetricDisplaySummary's attributes to show the number collecting
     * doesn't make sense; showNumberCollecting should false for each bean.
     * </p>
     * <p>
     * When the entId is compatible group of servers or platforms, return all of
     * the metrics for the type. Each MetricDisplaySummary actually represents
     * the metrics summarized for all of the group members (cumulative/averaged
     * as appropriate), showNumberCollecting should be true and the
     * numberCollecting as well as the total number of members assigned in each
     * bean.
     * </p>
     * @return Map keyed on the category (String), values are List's of
     *         MetricDisplaySummary beans
     * @see MetricDisplaySummary
     */
    public MetricDisplaySummary findMetric(int sessionId, List resources, Integer tid, long begin, long end)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException, AppdefEntityNotFoundException,
        AppdefCompatException, MeasurementNotFoundException;

    /**
     * Prunes from the list of passed-in AppdefEntityValue array those resources
     * that are not collecting the metric corresponding to the given template
     * id.
     * @param resources the resources
     * @param tid the metric template id
     * @return an array of resources
     */
    public AppdefResourceValue[] pruneResourcesNotCollecting(int sessionId, AppdefResourceValue[] resources, Integer tid)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException,
        MeasurementNotFoundException, PermissionException;

    /**
     * Method findResourceMetricSummary. For metric comparisons, the
     * ResourceMetricDisplaySummary beans are returned as a map where the keys
     * are the MeasurementTemplateValue (or MeasurementTemplateLiteValue?)
     * objects associated with the given resource's types, the values are Lists
     * of ResourceMetricDisplaySummary The context that the user will be
     * populating the input resource list from should always be like resource
     * types. If for some reason that's not the case, this method will take a
     * garbage in/garbage out approach (as opposed to enforcing like types) --
     * comparing apples and oranges may be performed but if the user ends up
     * with measurement templates for which there is only one resource to
     * compare, that should indicate some other problem i.e. the application is
     * presenting dissimilar objects as available for comparison. The list of
     * resources can be any concrete AppdefResourceValue (i.e. a platform,
     * server or service), composite AppdefResourceValues (i.e. applications,
     * groups) are inappropriate for this signature. Used for screen 0.3
     * @param begin the commencement of the timeframe of interest
     * @param end the end of the timeframe of interest
     * @return Map of measure templates and resource metric lists
     */
    public Map<MeasurementTemplate, List<MetricDisplaySummary>> findResourceMetricSummary(int sessionId,
                                                                                          AppdefEntityID[] entIds,
                                                                                          long begin, long end)
        throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException,
        MeasurementNotFoundException, PermissionException;

    /**
     * Return a MetricSummary bean for each of the metrics (template) for the
     * entities in the given time frame
     * @param begin the beginning time frame
     * @param end the ending time frame
     * @return a list of ResourceTypeDisplaySummary beans
     * @throws AppdefCompatException
     */
    public Map<String, Set<MetricDisplaySummary>> findMetrics(int sessionId, AppdefEntityID[] entIds, long filters,
                                                              String keyword, long begin, long end,
                                                              boolean showNoCollect) throws SessionTimeoutException,
        SessionNotFoundException, InvalidAppdefTypeException, PermissionException, AppdefEntityNotFoundException,
        AppdefCompatException;

    /**
     * Method findMetrics. When the entId is a server, return all of the metrics
     * that are instances of the measurement templates for the server's type. In
     * this case, the MetricDisplaySummary's attributes to show the number
     * collecting doesn't make sense; showNumberCollecting should false for each
     * bean.
     * <p>
     * When the entId is a platform, return all of the metrics that are
     * instances of the measurement templates for the platform's type. In this
     * case, the MetricDisplaySummary's attributes to show the number collecting
     * doesn't make sense; showNumberCollecting should false for each bean.
     * </p>
     * <p>
     * When the entId is compatible group of servers or platforms, return all of
     * the metrics for the type. Each MetricDisplaySummary actually represents
     * the metrics summarized for all of the group members (cumulative/averaged
     * as appropriate), showNumberCollecting should be true and the
     * numberCollecting as well as the total number of members assigned in each
     * bean.
     * </p>
     * @return Map keyed on the category (String), values are List's of
     *         MetricDisplaySummary beans
     * @throws AppdefCompatException
     * @see MetricDisplaySummary
     */
    public Map<String, Set<MetricDisplaySummary>> findMetrics(int sessionId, AppdefEntityID entId, List<Integer> mtids,
                                                              long begin, long end) throws SessionTimeoutException,
        SessionNotFoundException, PermissionException, AppdefEntityNotFoundException, AppdefCompatException;

    /**
     * Return a MetricSummary bean for each of the servers of a specific type.
     * @param begin the beginning time frame
     * @param end the ending time frame
     * @return a list of ResourceTypeDisplaySummary beans
     * @throws AppdefCompatException
     */
    public Map<String, Set<MetricDisplaySummary>> findAGPlatformMetricsByType(int sessionId,
                                                                              AppdefEntityTypeID platTypeId,
                                                                              long begin, long end, boolean showAll)
        throws SessionTimeoutException, SessionNotFoundException, InvalidAppdefTypeException,
        AppdefEntityNotFoundException, PermissionException, AppdefCompatException;

    /**
     * Return a Metric summary bean for each of the services of a specific type
     * <p>
     * The map returned has keys for the measurement categories (see
     * MeasurementConstants) and values that are Lists of MetricDisplaySummary
     * beans.
     * </p>
     * <p>
     * This is used to access metrics for entity's internal and deployed
     * services. The metrics returned are only applicable from within the given
     * timeframe of interest.
     * </p>
     * <p>
     * Appropriate entities include
     * <ul>
     * <li>applications (2.1.2.2-3)
     * <li>servers (2.3.2.1-4 - internal/deplyed tabs)
     * <li>services (2.5.2.2 - internal/deplyed tabs)
     * </ul>
     * @param begin the beginning time frame
     * @param end the ending time frame
     * @return a list of CurrentHealthDisplaySummary beans
     * @throws AppdefCompatException
     */
    public Map<String, Set<MetricDisplaySummary>> findAGMetricsByType(int sessionId, AppdefEntityID[] entIds,
                                                                      AppdefEntityTypeID typeId, long filters,
                                                                      String keyword, long begin, long end,
                                                                      boolean showAll) throws SessionTimeoutException,
        SessionNotFoundException, InvalidAppdefTypeException, PermissionException, AppdefEntityNotFoundException,
        AppdefCompatException;

    /**
     * Return a MeasurementSummary bean for the resource's associated resources
     * specified by type
     * @param entId the entity ID
     * @param appdefType the type (server, service, etc) of the specified
     *        resource type
     * @param typeId the specified resource type ID
     * @return a MeasurementSummary bean
     */
    public MeasurementSummary getSummarizedResourceAvailability(int sessionId, AppdefEntityID entId, int appdefType,
                                                                Integer typeId) throws AppdefEntityNotFoundException,
        PermissionException, SessionNotFoundException, SessionTimeoutException, InvalidOptionException;

    /**
     * Method findSummarizedServerCurrentHealth.
     * <p>
     * Return a ResourceTypeDisplaySummary bean for each of the platform's
     * deployed servers. Each bean represents a type of server and the
     * measurement data summarized for that type.
     * </p>
     * <p>
     * see screen 2.2.2
     * </p>
     * @return List of ResourceTypeDisplaySummary beans
     */
    public List<ResourceTypeDisplaySummary> findSummarizedServerCurrentHealth(int sessionId, AppdefEntityID entId)
        throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException, PermissionException;

    /**
     * Method findSummarizedServiceCurrentHealth.
     * <p>
     * This is used for the lists of service types for the Current Health view
     * for
     * <ul>
     * <li>applications (2.1.2)
     * <li>servers (2.3.2.1-4)
     * <li>services (2.5.2.2)
     * </ul>
     * </p>
     * <p>
     * If <code>internal</code> is <i>true</i>, only the <i>internal</i>
     * services will be returned, the <i>deployed</i> ones if it's <i>false</i>.
     * If <code>internal</code> is <i>null</i>, then both deployed <i>and</i>
     * internal services will be returned.
     * </p>
     * @param entId the appdef entity with child services
     * @return List a list of ResourceTypeDisplaySummary beans
     */
    public List<ResourceTypeDisplaySummary> findSummarizedPlatformServiceCurrentHealth(int sessionId,
                                                                                       AppdefEntityID entId)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException, AppdefEntityNotFoundException;

    public List<ResourceTypeDisplaySummary> findSummarizedServiceCurrentHealth(int sessionId, AppdefEntityID entId)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException, AppdefEntityNotFoundException;

    /**
     * Method findGroupCurrentHealth.
     * <p>
     * Return a ResourceDisplaySummary bean for each of the group's member
     * resources. Each bean represents a resource and the measurement data
     * summarized for that type.
     * </p>
     * <p>
     * see screen 2.2.2
     * </p>
     * @return List of ResourceDisplaySummary beans
     */
    public List<ResourceDisplaySummary> findGroupCurrentHealth(int sessionId, Integer id)
        throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException, PermissionException;

    /**
     * Return a ResourceDisplaySummary bean for each of the resource's virtual
     * resources. Each bean represents a resource and the measurement data
     * summarized for that resource. </p>
     * <p>
     * see screen 2.2.2
     * </p>
     * @return List of ResourceDisplaySummary beans
     */
    public List<ResourceDisplaySummary> findVirtualsCurrentHealth(int sessionId, AppdefEntityID entId)
        throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException,
        GroupNotCompatibleException, PermissionException;

    /**
     * Method findResourcesCurrentHealth. The size of the list of
     * ResourceDisplaySummary beans returned will be equivalent to the size of
     * the entity ID's passed in. Called by RSS feed so it does not require
     * valid session ID.
     * @throws ApplicationException if user is not found
     * @throws LoginException if user account has been disabled
     * @return PageList of ResourceDisplaySummary beans
     */
    public List<ResourceDisplaySummary> findResourcesCurrentHealth(String user, AppdefEntityID[] entIds)
        throws LoginException, ApplicationException, PermissionException, AppdefEntityNotFoundException,
        SessionNotFoundException, SessionTimeoutException;

    /**
     * Method findResourcesCurrentHealth. The size of the list of
     * ResourceDisplaySummary beans returned will be equivalent to the size of
     * the entity ID's passed in.
     * @return PageList of ResourceDisplaySummary beans
     */
    public List<ResourceDisplaySummary> findResourcesCurrentHealth(int sessionId, AppdefEntityID[] entIds)
        throws AppdefEntityNotFoundException, PermissionException, SessionNotFoundException, SessionTimeoutException;

    /**
     * Find the current health of the entity's host(s)
     * @return PageList of ResourceDisplaySummary beans
     */
    public List<ResourceDisplaySummary> findHostsCurrentHealth(int sessionId, AppdefEntityID entId, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException, AppdefEntityNotFoundException;

    /**
     * Method findPlatformsCurrentHealth. The population of the list of
     * ResourceDisplaySummary beans returned will vary depending on the entId's
     * type. When the entId is a server, the returned list should have just one
     * ResourceDisplaySummary with a PlatformValue in it, the one that
     * represents the host that the server resides on. When the entId is a
     * compatible group of platforms, the returned list will have as many
     * elements as there are individual PlatformValue's to represent all of the
     * hosts.
     * @return PageList of ResourceDisplaySummary beans
     */
    public PageList<ResourceDisplaySummary> findPlatformsCurrentHealth(int sessionId, AppdefEntityID entId,
                                                                       PageControl pc) throws SessionTimeoutException,
        SessionNotFoundException, AppdefEntityNotFoundException, PermissionException;

    /**
     * Method findAGPlatformsCurrentHealthByType For autogroup of platforms. If
     * the entId is a platform, the deployed servers view shows the current
     * health of servers.
     * @return a list of ResourceDisplaySummary beans
     */
    public List<ResourceDisplaySummary> findAGPlatformsCurrentHealthByType(int sessionId, Integer platTypeId)
        throws SessionTimeoutException, SessionNotFoundException, InvalidAppdefTypeException, PermissionException,
        AppdefEntityNotFoundException;

    /**
     * Method findServersCurrentHealth For the screens that rely on this API,
     * the entId is either an application, a service or a group. The population
     * of the list varies with the type of appdef entity input. This is used for
     * all of the application monitoring screens; they all show a list with
     * current health data for each server that participates in supplying
     * services for an application. So if the entity is an application, the list
     * is populated with servers that host the services on which the application
     * relies. The timeframe is not used in this context, the list of servers is
     * always the current list. The timeframe shall still be sent but it will be
     * bounded be the current time and current time - default time window. (see
     * 2.1.2 - 2.1.2.1-3) If the entId is a platform, the deployed servers view
     * shows the current health of servers in the timeframe that the metrics are
     * shown for. So if the entity is application, expect to populate the list
     * based on the presence of metrics in the timeframe of interest. (see
     * 2.2.2.3, it shows deployed servers... I'll give you a dollar if you can
     * come up with a reason why we'd want internal servers. We aren't managing
     * cron or syslog, dude.) This is also used for a services' current health
     * page in which case the appdef entity is a service.
     * @param entId the platform's or application's ID
     * @return a list of ResourceDisplaySummary beans
     */
    public PageList<ResourceDisplaySummary> findServersCurrentHealth(int sessionId, AppdefEntityID entId, PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException, InvalidAppdefTypeException,
        AppdefEntityNotFoundException, PermissionException;

    /**
     * Method findServersCurrentHealth For platform's autogroup of servers. If
     * the entId is a platform, the deployed servers view shows the current
     * health of servers.
     * @return a list of ResourceDisplaySummary beans
     */
    public List<ResourceDisplaySummary> findAGServersCurrentHealthByType(int sessionId, AppdefEntityID[] entIds,
                                                                         Integer serverTypeId)
        throws SessionTimeoutException, SessionNotFoundException, InvalidAppdefTypeException,
        AppdefEntityNotFoundException, PermissionException;

    /**
     * Return a ResourceDisplaySummary bean for each of the resource's services.
     * The only applicable resource is currently a compatible group (of
     * services...)
     * @return a list of ResourceDisplaySummary beans
     */
    public List<ResourceDisplaySummary> findAGServicesCurrentHealthByType(int sessionId, AppdefEntityID[] entIds,
                                                                          Integer serviceTypeId)
        throws SessionTimeoutException, SessionNotFoundException, InvalidAppdefTypeException,
        AppdefEntityNotFoundException, PermissionException;

    /**
     * Get Availability measurement for a given entitiy
     */
    public double getAvailability(AuthzSubject subj, AppdefEntityID id) throws AppdefEntityNotFoundException,
        PermissionException;

    /**
     * Get the availability of the resource
     * @param id the Appdef entity ID
     */
    public double getAvailability(int sessionId, AppdefEntityID id) throws SessionTimeoutException,
        SessionNotFoundException, AppdefEntityNotFoundException, PermissionException;

    /**
     * Get the availability of autogroup resources
     * @return a MetricValue for the availability
     */
    public double getAGAvailability(int sessionId, AppdefEntityID[] aids, AppdefEntityTypeID ctype)
        throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException, PermissionException;

    /**
     * Returns a list of problem metrics for an autogroup, return a summarized
     * list of UI beans
     * @throws SessionTimeoutException
     * @throws SessionNotFoundException
     * @throws AppdefEntityNotFoundException
     * @throws PermissionException
     * @throws InvalidAppdefTypeException
     * @throws AppdefCompatException
     */
    public List<ProblemMetricSummary> findAllMetrics(int sessionId, AppdefEntityID aeid, AppdefEntityTypeID ctype,
                                                     long begin, long end) throws SessionTimeoutException,
        SessionNotFoundException, AppdefEntityNotFoundException, PermissionException, AppdefCompatException,
        InvalidAppdefTypeException;

    /**
     * Returns a list of problem metrics for a resource, and the selected
     * children and hosts of that resource. Return a summarized list of UI beans
     */
    public List findAllMetrics(int sessionId, AppdefEntityID aeid, AppdefEntityID[] hosts,
                               AppdefEntityTypeID[] children, AppdefEntityID[] members, long begin, long end)
        throws SessionTimeoutException, SessionNotFoundException, AppdefEntityNotFoundException, PermissionException,
        AppdefCompatException, InvalidAppdefTypeException;

    /**
     * Returns a list of problem metrics for a resource, and the selected
     * children and hosts of that resource. Return a summarized list of UI beans
     */
    public List findAllMetrics(int sessionId, AppdefEntityID aeid, AppdefEntityID[] hosts,
                               AppdefEntityTypeID[] children, long begin, long end) throws SessionTimeoutException,
        SessionNotFoundException, AppdefEntityNotFoundException, PermissionException, AppdefCompatException,
        InvalidAppdefTypeException;

    /**
     * Get the availability metric for a given resource
     */
    public Measurement findAvailabilityMetric(int sessionId, AppdefEntityID id) throws SessionTimeoutException,
        SessionNotFoundException;

}
