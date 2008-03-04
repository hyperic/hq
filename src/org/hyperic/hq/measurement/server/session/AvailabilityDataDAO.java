/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.measurement.server.session;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.type.IntegerType;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.shared.AvailState;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.shared.MeasurementManagerLocal;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AvailabilityDataDAO extends HibernateDAO {
    
    private static final String logCtx = AvailabilityDataDAO.class.getName();
    private final Log _log = LogFactory.getLog(logCtx);
    
    private static final int MAX_TIMESTAMP =
        AvailabilityDataRLE.getLastTimestamp();
    private static final double AVAIL_UNKNOWN =
        MeasurementConstants.AVAIL_UNKNOWN;
    private static final double AVAIL_UP =
        MeasurementConstants.AVAIL_UP;
    private static final double AVAIL_DOWN =
        MeasurementConstants.AVAIL_DOWN;
    private static final double AVAIL_WARN =
        MeasurementConstants.AVAIL_WARN;
    private static final double AVAIL_PAUSED =
        MeasurementConstants.AVAIL_PAUSED;
    private static final String CAT_AVAILABILITY =
        MeasurementConstants.CAT_AVAILABILITY;
    private static final int PLATFORM_TYPE =
        AuthzConstants.authzPlatform.intValue();

    public AvailabilityDataDAO(DAOFactory f) {
        super(AvailabilityDataDAO.class, f);
    }

    List findLastAvail(List mids, int after) {
        String sql = "from AvailabilityDataRLE" +
                     " WHERE endtime > :endtime" +
                     " AND availabilityDataId.measurement in (:ids)" +
                     " order by endtime desc";
        return getSession()
            .createQuery(sql)
            .setInteger("endtime", after)
            .setParameterList("ids", mids, new IntegerType())
            .list();
    }

    List findLastAvail(List mids) {
        String sql = "from AvailabilityDataRLE" +
                     " WHERE endtime = :endtime" +
                     " AND availabilityDataId.measurement in (:ids)" +
                     " order by endtime desc";
        return getSession()
            .createQuery(sql)
            .setInteger("endtime", MAX_TIMESTAMP)
            .setParameterList("ids", mids, new IntegerType())
            .list();
    }

    int updateStartime(AvailabilityDataRLE avail, int startime) {
        remove(avail);
        avail.setStartime(startime);
        save(avail);
        return 1;
    }

    int updateEndtime(AvailabilityDataRLE avail, int endtime) {
        avail.setEndtime(endtime);
        save(avail);
        return 1;
    }

    AvailabilityDataRLE findAvail(AvailState state) {
        String sql = "FROM AvailabilityDataRLE" +
                     " WHERE availabilityDataId.startime = :startime" +
                     " AND availabilityDataId.measurement = :meas ";
        List list =
            getSession().createQuery(sql)
            .setInteger("startime", state.getTimestamp())
            .setInteger("meas", state.getId())
        	.list();
        if (list.size() == 0) {
            return null;
        }
        return (AvailabilityDataRLE)list.get(0);
    }
    
    List findAllAvailsAfter(AvailState state) {
        String sql = "FROM AvailabilityDataRLE" +
                     " WHERE availabilityDataId.startime > :startime" +
                     " AND availabilityDataId.measurement = :meas "+
                     "order by startime asc";
        return  getSession().createQuery(sql)
            .setInteger("startime", state.getTimestamp())
            .setInteger("meas", state.getId())
        	.list();
    }

    AvailabilityDataRLE findAvailAfter(AvailState state) {
        String sql = "FROM AvailabilityDataRLE" +
                     " WHERE availabilityDataId.startime > :startime" +
                     " AND availabilityDataId.measurement = :meas "+
                     "order by startime asc";
        List list =
            getSession().createQuery(sql)
            .setInteger("startime", state.getTimestamp())
            .setInteger("meas", state.getId())
        	.setMaxResults(1).list();
        if (list.size() == 0) {
            return null;
        }
        return (AvailabilityDataRLE)list.get(0);
    }

    void updateVal(AvailabilityDataRLE avail, double newVal) {
        avail.setAvailVal(newVal);
        save(avail);
    }

    AvailabilityDataRLE findAvailBefore(AvailState state) {
        String sql = "from AvailabilityDataRLE" +
                     " WHERE availabilityDataId.startime < :startime" +
                     " AND availabilityDataId.measurement = :meas "+
                     "order by startime desc";
        List list =
            getSession().createQuery(sql)
            .setInteger("startime", state.getTimestamp())
            .setInteger("meas", state.getId())
        	.setMaxResults(1).list();
        if (list.size() == 0) {
            return null;
        }
        return (AvailabilityDataRLE)list.get(0);
    }

    List findLastDownAvailability() {
        String sql = "from AvailabilityDataRLE where availval=?";
        return getSession()
            .createQuery(sql)
            .setDouble(0, AVAIL_DOWN)
            .list();
    }

    /**
     * @return List of Object[3].  Object[0] -> (Integer)startime.
     * Object[1] -> (Integer)endtime. Object[2] -> (Double)availVal.
     */
    List getHistoricalAvails(int mid, int start,
            int end, boolean descending) {
        String sql = new StringBuffer(512)
                    .append("SELECT rle.availabilityDataId.startime,")
                    .append(" rle.endtime, rle.availVal")
                    .append(" FROM AvailabilityDataRLE rle")
				    .append(" JOIN rle.availabilityDataId.measurement m")
				 	.append(" WHERE m.id = :mid")
				 	.append(" AND (rle.availabilityDataId.startime > :startime")
				 	.append("   OR rle.endtime > :startime)")
				 	.append(" AND (rle.availabilityDataId.startime < :endtime")
				 	.append("   OR rle.endtime < :endtime)")
				 	.append(" ORDER BY rle.availabilityDataId.startime")
				 	.append(((descending) ? " DESC" : " ASC")).toString();
        return getSession()
            .createQuery(sql)
            .setInteger("startime", start)
            .setInteger("endtime", end)
            .setInteger("mid", mid)
            .list();
    }

    /**
     * @return List of Object[3].  Object[0] -> (Integer)startime.
     * Object[1] -> (Integer)endtime. Object[2] -> (Double)availVal.
     */
    List getHistoricalAvails(Integer[] mids, int start,
            int end, boolean descending) {
        String sql = new StringBuffer(512)
                    .append("SELECT rle.availabilityDataId.startime,")
                    .append(" rle.endtime, avg(rle.availVal)")
                    .append(" FROM AvailabilityDataRLE rle")
				    .append(" JOIN rle.availabilityDataId.measurement m")
				 	.append(" WHERE m.id in (:mids)")
				 	.append(" AND (rle.availabilityDataId.startime > :startime")
				 	.append("   OR rle.endtime > :startime)")
				 	.append(" AND (rle.availabilityDataId.startime < :endtime")
				 	.append("   OR rle.endtime < :endtime)")
				 	.append(" GROUP BY rle.availabilityDataId.startime,")
				 	.append(" rle.endtime")
				 	.append(" ORDER BY rle.availabilityDataId.startime")
				 	.append(((descending) ? " DESC" : " ASC")).toString();
        return getSession()
            .createQuery(sql)
            .setInteger("startime", start)
            .setInteger("endtime", end)
            .setParameterList("mids", mids, new IntegerType())
            .list();
    }

    /**
     * @return List of Object[].  [0] = measurement template id,
     *  [1] = min(availVal), [2] = avg(availVal), [3] max(availVal)
     *  [4] = startime, [5] = endtime, [6] = availVal
     */
    List findAggregateAvailability(Integer[] tids, Integer[] iids,
            int start, int end) {
        String sql = new StringBuffer(512)
                    .append("SELECT m.template.id, min(rle.availVal),")
                    .append(" avg(rle.availVal), max(rle.availVal),")
                    .append(" rle.availabilityDataId.startime, rle.endtime,")
                    .append(" rle.availVal")
                    .append(" FROM Measurement m")
                    .append(" JOIN m.availabilityData rle")
                    .append(" WHERE m.template in (:tids)")
                    .append(" AND m.instanceId in (:iids)")
                    .append(" AND (rle.availabilityDataId.startime > :startime")
                    .append("   OR rle.endtime > :startime)")
                    .append(" AND (rle.availabilityDataId.startime < :endtime")
                    .append("   OR rle.endtime < :endtime)")
                    .append(" group by m.template.id,")
                    .append(" rle.availabilityDataId.startime, rle.availVal,")
                    .append(" rle.endtime").toString();
        return getSession()
            .createQuery(sql)
            .setInteger("startime", start)
            .setInteger("endtime", end)
            .setParameterList("tids", tids, new IntegerType())
            .setParameterList("iids", iids, new IntegerType())
            .list();
    }

    /**
     * @param resource - resource to query
     * @param startime - start time in seconds
     * @param interval - time interval in seconds
     * @return - List of availability
     */
    List findAvailabilityByResource(
        Resource resource, int startime, int interval) {
        String sql = "from AvailabilityDataRLE where resource.id = :rid";
        return getSession()
            .createQuery(sql)
            .setInteger("rid", resource.getId().intValue())
            .list();
    }

    void remove(AvailabilityDataRLE avail) {
        super.remove(avail);
    }

    AvailabilityDataRLE create(Measurement meas, int startime,
            int endtime, double availVal) {
        AvailabilityDataRLE availObj = new AvailabilityDataRLE(meas, startime,
            endtime, availVal);
        save(availObj);
        return availObj;
    }

    /**
     * @return List of all measurement ids for availability, ordered
     */
    List getAllAvailIds() {
        String sql = "SELECT m.id from Measurement m" +
				     " JOIN m.template t" +
				 	 " WHERE upper(t.alias) like '%" +
				 	 CAT_AVAILABILITY.toUpperCase() + "%' order by m.id";
        return getSession()
            .createQuery(sql)
            .list();
    }

    Measurement getAvailMeasurement(Resource resource) {
        String sql = "SELECT m from Measurement m" +
                     " JOIN m.resource r" +
				     " JOIN m.template t" +
				 	 " WHERE  r.id = " + resource.getId() +
				 	 " AND upper(t.alias) like '%" +
				 	 CAT_AVAILABILITY.toUpperCase() + "%'";
        return (Measurement)getSession()
            .createQuery(sql)
            .list().get(0);
    }

    /**
     * param List of resourceIds
     * return List of Availability Measurements which are children of the
     * resourceIds
     */
    List getAvailMeasurementChildren(List resourceIds) {
        String sql = "SELECT m from Measurement m" +
                     " JOIN m.resource.toEdges e" +
        			 " JOIN m.template t" +
        			 " WHERE e.distance > 0" +
        			 " AND e.from in (:ids)" +
				 	 " AND upper(t.alias) like '%" +
				 	 CAT_AVAILABILITY.toUpperCase() + "%'";
        return getSession()
            .createQuery(sql)
            .setParameterList("ids", resourceIds, new IntegerType())
            .list();
    }

    /**
     * @return List of down Measurements
     */
    List getDownMeasurements() {
        String sql = "SELECT m, rle.availabilityDataId.startime" +
                     " FROM Measurement m" +
				     " JOIN m.template t" +
				 	 " JOIN m.availabilityData rle" +
				 	 " WHERE rle.endtime = " + MAX_TIMESTAMP +
				 	 " AND rle.availVal = " + AVAIL_DOWN + 
				 	 " AND upper(t.alias) like '%" +
				 	 CAT_AVAILABILITY.toUpperCase() + "%'";
        return getSession()
            .createQuery(sql)
            .list();
    }

    /**
     * @return List of Object[].  Object[0] is Measurement Object[1] is Resource
     */
    List getPlatformResources() {
        String sql = "SELECT m, r FROM Measurement m" +
                     " JOIN m.resource r" +
				     " JOIN m.template t" +
				 	 " WHERE r.resourceType = " + PLATFORM_TYPE +
				 	 " AND upper(t.alias) like '%" +
				 	 CAT_AVAILABILITY.toUpperCase() + "%'";
        return getSession()
            .createQuery(sql)
            .list();
    }

    AvailabilityDataRLE create(Measurement meas, int startime,
            double availVal) {
        AvailabilityDataRLE availObj = new AvailabilityDataRLE(meas, startime,
            availVal);
        _log.debug("creating Avail: "+availObj);
        save(availObj);
        return availObj;
    }
}
