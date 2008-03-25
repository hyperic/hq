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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.type.IntegerType;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.measurement.MeasurementConstants;

public class AvailabilityDataDAO extends HibernateDAO {
    
    private static final String logCtx = AvailabilityDataDAO.class.getName();
    private final Log _log = LogFactory.getLog(logCtx);
    
    private static final long MAX_TIMESTAMP =
        AvailabilityDataRLE.getLastTimestamp();
    private static final double AVAIL_DOWN = MeasurementConstants.AVAIL_DOWN;
    private static final String ALIAS_CLAUSE = " upper(t.alias) = '" +
    				MeasurementConstants.CAT_AVAILABILITY.toUpperCase() + "' ";
    
    public AvailabilityDataDAO(DAOFactory f) {
        super(AvailabilityDataDAO.class, f);
    }

    List findLastAvail(List mids, long after) {
        String sql = new StringBuffer()
                     .append("from AvailabilityDataRLE")
                     .append(" WHERE endtime > :endtime")
                     .append(" AND availabilityDataId.measurement in (:ids)")
                     .append(" ORDER BY endtime desc").toString();
        return getSession()
            .createQuery(sql)
            .setLong("endtime", after)
            .setParameterList("ids", mids, new IntegerType())
            .list();
    }

    List findLastAvail(List mids) {
        String sql = new StringBuffer()
			         .append("from AvailabilityDataRLE")
                     .append(" WHERE endtime = :endtime")
                     .append(" AND availabilityDataId.measurement in (:ids)")
                     .append(" ORDER BY endtime desc").toString();
        return getSession()
            .createQuery(sql)
            .setLong("endtime", MAX_TIMESTAMP)
            .setParameterList("ids", mids, new IntegerType())
            .list();
    }

    int updateStartime(AvailabilityDataRLE avail, long startime) {
        remove(avail);
        avail.setStartime(startime);
        save(avail);
        return 1;
    }

    long updateEndtime(AvailabilityDataRLE avail, long endtime) {
        avail.setEndtime(endtime);
        save(avail);
        return 1;
    }

    AvailabilityDataRLE findAvail(DataPoint state) {
        String sql = new StringBuffer()
                     .append("FROM AvailabilityDataRLE")
                     .append(" WHERE availabilityDataId.measurement = :meas")
                     .append(" AND availabilityDataId.startime = :startime")
                     .toString();
        List list =
            getSession().createQuery(sql)
            .setLong("startime", state.getTimestamp())
            .setInteger("meas", state.getMetricId().intValue())
        	.list();
        if (list.size() == 0) {
            return null;
        }
        return (AvailabilityDataRLE)list.get(0);
    }
    
    List findAllAvailsAfter(DataPoint state) {
        String sql = new StringBuffer()
                     .append("FROM AvailabilityDataRLE")
                     .append(" WHERE availabilityDataId.measurement = :meas")
                     .append(" AND availabilityDataId.startime > :startime")
                     .append(" ORDER BY startime asc").toString();
        return  getSession().createQuery(sql)
            .setLong("startime", state.getTimestamp())
            .setInteger("meas", state.getMetricId().intValue())
        	.list();
    }

    AvailabilityDataRLE findAvailAfter(DataPoint state) {
        String sql = new StringBuffer()
                     .append("FROM AvailabilityDataRLE")
                     .append(" WHERE availabilityDataId.measurement = :meas")
                     .append(" AND availabilityDataId.startime > :startime")
                     .append(" ORDER BY startime asc").toString();
        List list =
            getSession().createQuery(sql)
            .setLong("startime", state.getTimestamp())
            .setInteger("meas", state.getMetricId().intValue())
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

    AvailabilityDataRLE findAvailBefore(DataPoint state) {
        String sql = new StringBuffer()
                     .append("FROM AvailabilityDataRLE")
                     .append(" WHERE availabilityDataId.measurement = :meas")
                     .append(" AND availabilityDataId.startime < :startime")
                     .append(" ORDER BY startime desc").toString();
        List list =
            getSession().createQuery(sql)
            .setLong("startime", state.getTimestamp())
            .setInteger("meas", state.getMetricId().intValue())
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
     * @return List of AvailabilityDataRLE objs
     */
    List getHistoricalAvails(Measurement m, long start,
                             long end, boolean descending) {
        String sql = new StringBuffer()
                    .append("SELECT rle")
                    .append(" FROM AvailabilityDataRLE rle")
				    .append(" JOIN rle.availabilityDataId.measurement m")
				 	.append(" WHERE m.id = :m")
				 	.append(" AND (rle.availabilityDataId.startime > :startime")
				 	.append("   OR rle.endtime > :startime)")
				 	.append(" AND (rle.availabilityDataId.startime < :endtime")
				 	.append("   OR rle.endtime < :endtime)")
				 	.append(" ORDER BY rle.availabilityDataId.measurement,")
				 	.append(" rle.availabilityDataId.startime")
				 	.append(((descending) ? " DESC" : " ASC")).toString();
        return getSession()
            .createQuery(sql)
            .setLong("startime", start)
            .setLong("endtime", end)
            .setParameter("m", m)
            .list();
    }

    /**
     * @return List of AvailabilityDataRLE objs
     */
    List getHistoricalAvails(Integer[] mids, long start,
                             long end, boolean descending) {
        String sql = new StringBuffer()
                    .append("SELECT rle")
                    .append(" FROM AvailabilityDataRLE rle")
				    .append(" JOIN rle.availabilityDataId.measurement m")
				 	.append(" WHERE m.id in (:mids)")
				 	.append(" AND (rle.availabilityDataId.startime > :startime")
				 	.append("   OR rle.endtime > :startime)")
				 	.append(" AND (rle.availabilityDataId.startime < :endtime")
				 	.append("   OR rle.endtime < :endtime)")
				 	.append(" ORDER BY rle.availabilityDataId.measurement,")
				 	.append(" rle.availabilityDataId.startime")
				 	.append(((descending) ? " DESC" : " ASC")).toString();
        return getSession()
            .createQuery(sql)
            .setLong("startime", start)
            .setLong("endtime", end)
            .setParameterList("mids", mids, new IntegerType())
            .list();
    }

    /**
     * @return List of Object[].  [0] = measurement template id,
     *  [1] = min(availVal), [2] = avg(availVal), [3] max(availVal)
     *  [4] = startime, [5] = endtime, [6] = availVal, [7] mid count
     */
    List findAggregateAvailability(Integer[] tids, Integer[] iids,
                                   long start, long end) {
        String sql = new StringBuffer()
                    .append("SELECT m.template.id, min(rle.availVal),")
                    .append(" avg(rle.availVal), max(rle.availVal),")
                    .append(" rle.availabilityDataId.startime, rle.endtime,")
                    .append(" rle.availVal, count(m.id)")
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
            .setLong("startime", start)
            .setLong("endtime", end)
            .setParameterList("tids", tids, new IntegerType())
            .setParameterList("iids", iids, new IntegerType())
            .list();
    }

    void remove(AvailabilityDataRLE avail) {
        super.remove(avail);
    }

    AvailabilityDataRLE create(Measurement meas, long startime,
                               long endtime, double availVal) {
        AvailabilityDataRLE availObj =
            new AvailabilityDataRLE(meas, startime, endtime, availVal);
        save(availObj);
        return availObj;
    }

    /**
     * @return List of down Measurements
     */
    List getDownMeasurements() {
        String sql = new StringBuffer()
                     .append("SELECT rle FROM AvailabilityDataRLE rle")
                     .append(" JOIN rle.availabilityDataId.measurement m")
				     .append(" JOIN m.template t")
				 	 .append(" WHERE rle.endtime = " + MAX_TIMESTAMP)
				 	 .append(" AND m.resource is not null ")
				 	 .append(" AND rle.availVal = " + AVAIL_DOWN)
				 	 .append(" AND " + ALIAS_CLAUSE).toString();
        return getSession()
            .createQuery(sql)
            .list();
    }

    AvailabilityDataRLE create(Measurement meas, long startime, double availVal)
    {
        AvailabilityDataRLE availObj = new AvailabilityDataRLE(meas, startime,
                                                               availVal);
        _log.debug("creating Avail: "+availObj);
        save(availObj);
        return availObj;
    }
}
