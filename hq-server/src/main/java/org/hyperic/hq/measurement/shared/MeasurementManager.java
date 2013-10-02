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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.ResourceZevent;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.InvalidConfigException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.events.MaintenanceEvent;
import org.hyperic.hq.management.shared.MeasurementInstruction;
import org.hyperic.hq.measurement.MeasurementCreateException;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.TemplateNotFoundException;
import org.hyperic.hq.measurement.ext.MeasurementEvent;
import org.hyperic.hq.measurement.monitor.LiveMeasurementException;
import org.hyperic.hq.measurement.server.session.CollectionSummary;
import org.hyperic.hq.measurement.server.session.DataPoint;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementDAO;
import org.hyperic.hq.measurement.server.session.MeasurementEnabler;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.MeasurementTemplateDAO;
import org.hyperic.hq.util.Reference;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;

/**
 * Local interface for MeasurementManager.
 */
public interface MeasurementManager {
    /**
     * Create Measurements and enqueue for scheduling after commit
     */
    public List<Measurement> createMeasurements(AuthzSubject subject, AppdefEntityID id,
                                                Integer[] templates, long[] intervals,
                                                ConfigResponse props) throws PermissionException,
        MeasurementCreateException, TemplateNotFoundException;
    
    List<Measurement> createDefaultMeasurements(AuthzSubject subject, AppdefEntityID id,
        String mtype, ConfigResponse props) throws TemplateNotFoundException, PermissionException, 
        MeasurementCreateException;
    
    void disableMeasurementsForDeletion(AuthzSubject subject, Agent agent,
             AppdefEntityID[] ids) throws PermissionException;

    /**
     * Create Measurement objects based their templates and default intervals
     * @param templates List of Integer template IDs to add
     * @param id instance ID (appdef resource) the templates are for
     * @param props Configuration data for the instance
     * @return a List of the associated Measurement objects
     */
    public List<Measurement> createMeasurements(AuthzSubject subject, AppdefEntityID id,
                                                Integer[] templates, ConfigResponse props)
        throws PermissionException, MeasurementCreateException, TemplateNotFoundException;

    public Measurement findMeasurementById(Integer mid);

    public Map<Integer,Measurement> findMeasurementsByIds(final List<Integer> mids);
    
    /**
     * Remove all measurements no longer associated with a resource.
     * @return The number of Measurement objects removed.
     */
    public int removeOrphanedMeasurements(int batchSize);

    /**
     * Look up a Measurement for a Resource and Measurement alias
     * @return a The Measurement for the Resource of the given alias.
     */
    public Measurement getMeasurement(AuthzSubject s, Resource r, String alias)
        throws MeasurementNotFoundException;

    /**
     * Get a Measurement by Id.
     */
    public Measurement getMeasurement(Integer mid);

    /**
     * Get the live measurement values for a given resource.
     * @param id The id of the resource
     */
    public void getLiveMeasurementValues(AuthzSubject subject, AppdefEntityID id)
        throws PermissionException, LiveMeasurementException, MeasurementNotFoundException;

    /**
     * Count of metrics enabled for a particular entity
     * @return a The number of metrics enabled for the given entity
     */
    public int getEnabledMetricsCount(AuthzSubject subject, AppdefEntityID id);

    public Map<Resource, List<Measurement>> findMeasurements(
                                                             AuthzSubject subject,
                                                             Map<Integer, List<Integer>> resIdsToTemplIds)
        throws PermissionException;

    public Map<Resource, List<Measurement>> findBulkMeasurements(
            AuthzSubject subject,
            Map<Integer, List<Integer>> resIdsToTemplIds,
            Map<Integer, Exception> failedResources)
                    throws PermissionException;
    
    /**
     * Find the Measurement corresponding to the given MeasurementTemplate id
     * and instance id.
     * @param tid The MeasurementTemplate id
     * @param aeid The entity id.
     * @return a Measurement value
     */
    public Measurement findMeasurement(AuthzSubject subject, Integer tid, AppdefEntityID aeid)
        throws MeasurementNotFoundException;

    /**
     * Look up a Measurement, allowing for the query to return a stale copy of
     * the Measurement (for efficiency reasons).
     * @param subject The subject.
     * @param tid The template Id.
     * @param iid The instance Id.
     * @param allowStale <code>true</code> to allow stale copies of an alert
     *        definition in the query results; <code>false</code> to never allow
     *        stale copies, potentially always forcing a sync with the database.
     * @return The Measurement
     */
    public Measurement findMeasurement(AuthzSubject subject, Integer tid, Integer iid,
                                       boolean allowStale) throws MeasurementNotFoundException;

    /**
     * Look up a list of Measurements for a template and instances
     * @return a list of Measurement's
     */
    public List<Measurement> findMeasurements(AuthzSubject subject, Integer tid,
                                              AppdefEntityID[] aeids);

    /**
     * Look up a list of Measurements for a template and instances
     * @return An array of Measurement ids.
     */
    public Integer[] findMeasurementIds(AuthzSubject subject, Integer tid, Integer[] ids);

    /**
     * Look up a list of Measurements for a category XXX: Why is this method
     * called findMeasurements() but only returns enabled measurements if cat ==
     * null??
     * @return a List of Measurement objects.
     */
    public List<Measurement> findMeasurements(AuthzSubject subject, AppdefEntityID id, String cat,
                                              PageControl pc);

    Map<Integer,List<Measurement>> getEnabledMeasurements(List<Resource> resources);
    /**
     * Look up a list of enabled Measurements for a category
     * @return a list of {@link Measurement}
     */
    public List<Measurement> findEnabledMeasurements(AuthzSubject subject, AppdefEntityID id,
                                                     String cat);
    
    /**
     * @param aeids {@link List} of {@link AppdefEntityID}s
     * @return {@link Map} of {@link Integer} representing resourceId to
     * {@link List} of {@link Measurement}s
     * 
     */
    public Map<Integer,List<Measurement>> findEnabledMeasurements(Collection<AppdefEntityID> aeids);

    /**
     * Look up a List of designated Measurements for an entity
     * @return A List of Measurements
     */
    public List<Measurement> findDesignatedMeasurements(AppdefEntityID id);

    /**
     * Look up a list of designated Measurements for an entity for a category
     * @return A List of Measurements
     */
    public List<Measurement> findDesignatedMeasurements(AuthzSubject subject, AppdefEntityID id,
                                                        String cat);

    /**
     * Look up a list of designated Measurements for an group for a category
     * @return A List of Measurements
     */
    public List<Measurement> findDesignatedMeasurements(AuthzSubject subject, ResourceGroup g,
                                                        String cat);

    /**
     * Get an Availabilty Measurement by AppdefEntityId
     * @deprecated Use getAvailabilityMeasurement(Resource) instead.
     */
    @Deprecated
    public Measurement getAvailabilityMeasurement(AuthzSubject subject, AppdefEntityID id);

    /**
     * Get an Availability Measurement by Resource. May return null.
     */
    public Measurement getAvailabilityMeasurement(Resource r);

    /**
     * Look up a list of Measurement objects by category
     */
    public List<Measurement> findMeasurementsByCategory(String cat);

    /**
     * Look up a Map of Measurements for a Category XXX: This method needs to be
     * re-thought. It only returns a single designated metric per category even
     * though HQ supports multiple designates per category.
     * @return A List of designated Measurements keyed by AppdefEntityID
     */
    public Map<AppdefEntityID, Measurement> findDesignatedMeasurements(AuthzSubject subject,
                                                                       AppdefEntityID[] ids,
                                                                       String cat)
        throws MeasurementNotFoundException;

    /**
     * TODO: scottmf, need to do some more work to handle other authz resource
     * types other than platform, server, service, and group
     * @return {@link Map} of {@link Integer} to {@link List} of
     *         {@link Measurement}s, Integer => Resource.getId(),
     */
    public Map<Integer, List<Measurement>> getAvailMeasurements(Collection<?> resources);

    public Map<Resource, List<Measurement>> getAvailMeasurementsByResource(Collection<?> resources);

    /**
     * Look up a list of Measurement intervals for template IDs.
     * @return a map keyed by template ID and values of metric intervals There
     *         is no entry if a metric is disabled or does not exist for the
     *         given entity or entities. However, if there are multiple
     *         entities, and the intervals differ or some enabled/not enabled,
     *         then the value will be "0" to denote varying intervals.
     */
    public Map<Integer, Long> findMetricIntervals(AuthzSubject subject, AppdefEntityID[] aeids,
                                                  Integer[] tids);

    public void findAllEnabledMeasurementsAndTemplates();

    /**
     * Set the interval of Measurements based their template ID's Enable
     * Measurements and enqueue for scheduling after commit
     */
    public void enableMeasurements(AuthzSubject subject, AppdefEntityID[] aeids, Integer[] mtids,
                                   long interval) throws MeasurementNotFoundException,
        MeasurementCreateException, TemplateNotFoundException, PermissionException;

    /**
     * Enable a collection of metrics, enqueue for scheduling after commit
     */
    public void enableMeasurements(AuthzSubject subject, Integer[] mids) throws PermissionException;

    /**
     * Enable the Measurement and enqueue for scheduling after commit
     */
    public void enableMeasurement(AuthzSubject subject, Integer mId, long interval)
        throws PermissionException;

    /**
     * Enable the default on metrics for a given resource, enqueue for
     * scheduling after commit
     */
    public void enableDefaultMeasurements(AuthzSubject subj, Resource r) throws PermissionException;

    public void updateMeasurementInterval(AuthzSubject subject, Integer mId, long interval)
        throws PermissionException;

    /**
     * Disable all measurements for the given resources.
     * @param agentId The entity id to use to look up the agent connection
     * @param ids The list of entitys to unschedule
     */
    public void disableMeasurements(AuthzSubject subject, AppdefEntityID agentId,
                                    AppdefEntityID[] ids) throws PermissionException, AgentNotFoundException;

    /**
     * Disable all Measurements for a resource
     */
    public void disableMeasurements(AuthzSubject subject, AppdefEntityID id)
        throws PermissionException;

    /**
     * Disable all Measurements for a resource
     */
    public void disableMeasurements(AuthzSubject subject, Resource res) throws PermissionException;
    
    void disableMeasurements(AuthzSubject subject, Agent agent, AppdefEntityID[] ids, boolean isAsyncDelete) throws PermissionException;

    /**
     * XXX: not sure why all the findMeasurements require an authz if they do
     * not check the viewPermissions??
     */
    public List<Measurement> findMeasurements(AuthzSubject subject, Resource res);

    /**
     * Disable measurements for an instance Enqueues reschedule events after
     * commit
     */
    public void disableMeasurements(AuthzSubject subject, AppdefEntityID id, Integer[] tids)
        throws PermissionException;

    /**
     * Disable or enable measurements for a collection of resources
     * during a maintenance window
     */
    public List<DataPoint> enableMeasurements(AuthzSubject admin,
                                              MaintenanceEvent event,
                                              Collection<Resource> resources);

//    public void syncPluginMetrics(String plugin);

    /**
     * Gets a summary of the metrics which are scheduled for collection, across
     * all resource types and metrics.
     * @return a list of {@link CollectionSummary} beans
     */
    public List<CollectionSummary> findMetricCountSummaries();

    /**
     * Find a list of tuples (of size 4) consisting of the {@link Agent} the
     * {@link Platform} it manages the {@link Server} representing the Agent the
     * {@link Measurement} that contains the Server Offset value
     */
    public List<Object[]> findAgentOffsetTuples();

    /**
     * Get the # of metrics that each agent is collecting.
     * @return a map of {@link Agent} onto Longs indicating how many metrics
     *         that agent is collecting.
     */
    public Map<Agent, Long> findNumMetricsPerAgent();

    /**
     * Handle events from the {@link MeasurementEnabler}. This method is
     * required to place the operation within a transaction (and session)
     */
    public void handleCreateRefreshEvents(List<ResourceZevent> events);

    /**
     * Check a configuration to see if it returns DSNs which the agent can use
     * to successfully monitor an entity. This routine will attempt to get live
     * DSN values from the entity.
     * @param entity Entity to check the configuration for
     * @param config Configuration to check
     */
    public void checkConfiguration(AuthzSubject subject, AppdefEntityID entity, ConfigResponse config, boolean priority)
    throws PermissionException, InvalidConfigException, AppdefEntityNotFoundException;

    public List<Measurement> getMeasurements(Integer[] tids, Integer[] aeids);

    /**
     * Initializes the units and resource properties of a measurement event
     */
    public void buildMeasurementEvent(MeasurementEvent event);

    /**
     * Get the maximum collection interval for a scheduled metric within a
     * compatible group of resources.
     * @return The maximum collection time in milliseconds.
     */
    long getMaxCollectionInterval(ResourceGroup g, Integer templateId);

    /**
     * Return a List of Measurements that are collecting for the given template
     * ID and group.
     * @param g The group in question.
     * @param templateId The measurement template to query.
     * @return templateId A list of Measurement objects with the given template
     *         id in the group that are set to be collected.
     */
    List<Measurement> getMetricsCollecting(ResourceGroup g, Integer templateId);

    List<Measurement> getEnabledMeasurements(Integer[] tids, Integer[] aeids);

    Map<Integer, List<Measurement>> getEnabledNonAvailMeasurements(List<Resource> resources);

    /**
     * This method allows a consumer to grab the associated MeasurementTemplate
     * from a MeasurementId in a non-transactional context.  The returned MeasurementTemplate is
     * fully populated.
     * @return {@link MeasurementTemplate}s
     */
    MeasurementTemplate getTemplatesByMeasId(Integer measId);
    
    /**
     * Create Measurement objects based their templates
     * @param templates List of Integer template IDs to add
     * @param id instance ID (appdef resource) the templates are for
     * @param intervals Millisecond interval that the measurement is polled
     * @param props Configuration data for the instance
     * @param updated set true if any measurements were created or updated
     * @return a List of the associated Measurement objects
     */
    public List<Measurement> createOrUpdateMeasurements(AppdefEntityID id, Integer[] templates, long[] intervals,
        ConfigResponse props, Reference<Boolean> updated) throws MeasurementCreateException, TemplateNotFoundException;

    /**
     * Create Measurement objects for the resource based on their mesurement instructions
     * @param subject
     * @param resource
     * @param aeid AppdefEntityID of the Resource
     * @param measurementInstructions
     * @param props
     * @return
     * @throws MeasurementCreateException
     * @throws PermissionException
     */
    public List<Measurement> createOrUpdateOrDeleteMeasurements(AuthzSubject subject, Resource resource, 
            AppdefEntityID aeid, Collection<MeasurementInstruction> measurementInstructions, ConfigResponse props)
            throws MeasurementCreateException, PermissionException;    
    
	void setSrnManager(SRNManager srnManager);
	
	void setMeasurementDao(MeasurementDAO dao);
	
	void setResourceManager(ResourceManager resourceManager);
	
	void setMeasurementTemplateDao(MeasurementTemplateDAO mTemplateDao);

    Collection<MeasurementTemplate> getTemplatesByPrototype(Resource proto);
}
