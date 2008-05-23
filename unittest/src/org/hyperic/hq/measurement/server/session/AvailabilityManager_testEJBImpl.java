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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.shared.AvailabilityManagerLocal;
import org.hyperic.hq.measurement.shared.AvailabilityManager_testLocal;
import org.hyperic.hq.measurement.shared.AvailabilityManager_testUtil;
import org.hyperic.hq.product.MetricValue;

/**
 * The session bean implementing the in-container unit tests for the 
 * AuthzSubjectManager.
 * 
 * @ejb:bean name="AvailabilityManager_test"
 *      jndi-name="ejb/authz/AvailabilityManager_test"
 *      local-jndi-name="LocalAvailabilityManager_test"
 *      view-type="local"
 *      type="Stateless"
 * 
 * @ejb:util generate="physical"
 * @ejb:transaction type="NOTSUPPORTED"
 */
public class AvailabilityManager_testEJBImpl implements SessionBean {

    private static final String _logCtx =
        AvailabilityManager_testEJBImpl.class.getName();
    private final Log _log = LogFactory.getLog(_logCtx);
    private static final String AVAIL_TAB = "HQ_AVAIL_DATA_RLE";
    private static final Integer MEAS_ID = new Integer(10100);
    private final List _list = new ArrayList();
    
    /**
     * @ejb:interface-method
     */
    public void testInsertScenarios() throws Exception {
        // testing out of order scenarios
        testInsertIntoMiddle();
        testPrepend();
        testPrependWithDupValue();
        testNonOneorZeroDupPtInsertAtBegin();
        // test backfill + catchup
        testCatchup();
        stressTest1();
        stressTest2();
    }
    
    private void stressTest2() throws Exception {
        setupAvailabilityTable();
        AvailabilityManagerLocal avail = AvailabilityManagerEJBImpl.getOne();
        ArrayList list = new ArrayList();
        long now = System.currentTimeMillis();
        long baseTime = TimingVoodoo.roundDownTime(now, 60000);
        long incrTime = 60000;
        long tmpTime = baseTime;
        for (int i=0; i<1000; i++) {
            double val = 1.0;
            if ((i%5) == 0) {
                val = .5;
            }
            list.add(new DataPoint(MEAS_ID.intValue(), val, tmpTime+=incrTime));
        }
        avail.addData(list);
        list.clear();
        for (int i=0; i<1000; i++) {
            list.add(new DataPoint(MEAS_ID.intValue(), 0.0, tmpTime+=incrTime));
        }
        DataPoint pt = addData(new MetricValue(1.0, tmpTime));
        Assert.assertTrue(isAvailDataRLEValid(pt));
    }

    private void stressTest1() throws Exception {
        setupAvailabilityTable();
        long now = System.currentTimeMillis();
        long baseTime = TimingVoodoo.roundDownTime(now, 60000);
        long incrTime = 60000;
        testCatchup(baseTime, incrTime);
        DataPoint pt = testCatchup((baseTime+120000), incrTime);
        Assert.assertTrue(isAvailDataRLEValid(pt));
    }
    
    private void testCatchup() throws Exception {
        setupAvailabilityTable();
        DataPoint pt = testCatchup(System.currentTimeMillis(), 60000);
        Assert.assertTrue(isAvailDataRLEValid(pt));
    }

    private DataPoint testCatchup(long baseTime, long incrTime)
        throws Exception {
        baseTime = TimingVoodoo.roundDownTime(baseTime, 60000);

        AvailabilityManagerLocal avail = AvailabilityManagerEJBImpl.getOne();
        List list = new ArrayList();
        long tmpTime = baseTime;
        DataPoint pt;

        pt = new DataPoint(MEAS_ID, new MetricValue(1.0, baseTime));
        list.add(pt);
        pt = new DataPoint(MEAS_ID, new MetricValue(1.0, baseTime+incrTime*2));
        list.add(pt);
        for (int i=0; i<5; i++) {
            double val = 1.0;
            if ((i%2) == 0) {
                val = 0.0;
            }
            pt = new DataPoint(MEAS_ID, new MetricValue(val, tmpTime+=incrTime));
            list.add(pt);
        }
        avail.addData(list);

        list.clear();
        pt = new DataPoint(MEAS_ID, new MetricValue(1.0, tmpTime));
        list.add(pt);
        avail.addData(list);
        return pt;
    }

    /*
     * This test will insert into the middle of two availability points Hits
     * this code path
     *  updateOutOfOrderState(List)
     *  --> merge(DataPoint)
     *      --> updateDup(DataPoint, AvailabilityDataRLE)
     *          --> insertPointOnBoundry(AvailabilityDataRLE, long, DataPoint)
     *      MATCHES THIS CONDITION ->
     *              } else if (newStartime == avail.getEndtime()) {
     *                  } else {
     *                      dao.updateVal(after, pt.getValue());
     *                  }
     */
    private void testNonOneorZeroDupPtInsertAtBegin() throws Exception {
        setupAvailabilityTable();
        long INCRTIME = 60000;
        long baseTime = System.currentTimeMillis();
        baseTime = TimingVoodoo.roundDownTime(baseTime, 60000);
        long tmpTime = baseTime;

        DataPoint pt;
        addData(new MetricValue(0.0, tmpTime+=INCRTIME));
        addData(new MetricValue(1.0, tmpTime+=INCRTIME));
        addData(new MetricValue(0.0, tmpTime+=INCRTIME));
        pt = addData(new MetricValue(1.0, tmpTime+=INCRTIME));

        // overwrite first val
        addData(new MetricValue(0.5, baseTime+INCRTIME));
        Assert.assertTrue(isAvailDataRLEValid(pt));
    }
    
    /*
     * This test will insert into the middle of two availability points Hits
     * this code path
     *  updateOutOfOrderState(List)
     *  --> merge(DataPoint)
     *  MATCHES THIS CONDITION ->
     *          } else if (before == null) {
     */
    private void testPrependWithDupValue() throws Exception {
        setupAvailabilityTable();

        long baseTime = System.currentTimeMillis();
        baseTime = TimingVoodoo.roundDownTime(baseTime, 60000);
        long tmpTime = baseTime;

        DataPoint pt;
        addData(new MetricValue(1.0, tmpTime+=120000));
        addData(new MetricValue(0.0, tmpTime+=60000));
        addData(new MetricValue(1.0, tmpTime+=60000));
        pt = addData(new MetricValue(0.0, tmpTime+=60000));

        // prepend state on to the beginning
        addData(new MetricValue(1.0, baseTime+60000));

        Assert.assertTrue(isAvailDataRLEValid(pt));
    }

    /*
     * This test will insert into the middle of two availability points Hits
     * this code path
     *  updateOutOfOrderState(List)
     *  --> merge(DataPoint)
     *  MATCHES THIS CONDITION ->
     *          } else if (before == null) {
     */
    private void testPrepend() throws Exception {
        setupAvailabilityTable();

        long baseTime = System.currentTimeMillis();
        baseTime = TimingVoodoo.roundDownTime(baseTime, 60000);
        long tmpTime = baseTime;

        DataPoint pt;
        addData(new MetricValue(0.0, tmpTime+=120000));
        addData(new MetricValue(1.0, tmpTime+=60000));
        addData(new MetricValue(0.0, tmpTime+=60000));
        pt = addData(new MetricValue(1.0, tmpTime+=60000));

        // prepend state on to the beginning
        addData(new MetricValue(1.0, baseTime+60000));

        Assert.assertTrue(isAvailDataRLEValid(pt));
    }

    /*
     * This test will insert into the middle of two availability points Hits
     * this code path
     *  updateOutOfOrderState(List)
     *  --> merge(DataPoint)
     *      --> insertAvail(AvailabilityDataRLE, AvailabilityDataRLE, DataPoint)
     *      MATCHES THIS CONDITION ->
     *              } else if (state.getValue() == after.getAvailVal() &&
     *              state.getValue() != before.getAvailVal()) {
     */
    private void testInsertIntoMiddle() throws Exception {
        setupAvailabilityTable();
        int INCRTIME = 240000;

        long baseTime = System.currentTimeMillis();
        baseTime = TimingVoodoo.roundDownTime(baseTime, 60000);
        long tmpTime = baseTime;

        List list = new ArrayList();

        addData(new MetricValue(0.0, tmpTime+=INCRTIME));
        addData(new MetricValue(1.0, tmpTime+=INCRTIME));
        addData(new MetricValue(0.0, tmpTime+=INCRTIME));
        addData(new MetricValue(1.0, tmpTime+=INCRTIME));

        // insert into the middle
        list.clear();
        long middleTime = baseTime+(INCRTIME*3)+(INCRTIME/2);
        DataPoint pt = addData(new MetricValue(1.0, middleTime));

        Assert.assertTrue(isAvailDataRLEValid(pt));
    }
    
    private DataPoint addData(MetricValue mVal) {
        AvailabilityManagerLocal avail = AvailabilityManagerEJBImpl.getOne();
        _list.clear();
        DataPoint pt = new DataPoint(MEAS_ID, mVal);
        _list.add(pt);
        avail.addData(_list);
        return pt;
    }
    
    private boolean isAvailDataRLEValid(DataPoint lastPt) {
        AvailabilityDataDAO dao = getAvailabilityDataDAO();
        boolean descending = false;
        long start = 0l;
        long end = AvailabilityDataRLE.getLastTimestamp();
        Integer[] mids = new Integer[1];
        mids[0] = MEAS_ID;
        List avails = dao.getHistoricalAvails(mids, start, end, descending);
        AvailabilityDataRLE last = null;
        for (Iterator it=avails.iterator(); it.hasNext(); ) {
            AvailabilityDataRLE avail = (AvailabilityDataRLE)it.next();
            if (last == null) {
                last = avail;
                continue;
            }
            if (last.getAvailVal() == avail.getAvailVal()) {
                _log.error("consecutive availpoints have the same value");
                return false;
            } else if (last.getEndtime() != avail.getStartime()) {
                _log.error("there are gaps in the availability table");
            }
            last = avail;
        }
        AvailabilityCache cache = AvailabilityCache.getInstance();
        if (((DataPoint)cache.get(MEAS_ID)).getValue() != lastPt.getValue()) {
            _log.error("last avail data point does not match cache");
            return false;
        }
        return true;
    }
    
    private void setupAvailabilityTable() throws Exception {
        AvailabilityCache cache = AvailabilityCache.getInstance();
        cache.clear();
        AvailabilityDataDAO dao = getAvailabilityDataDAO();
        boolean descending = false;
        long start = 0l;
        long end = AvailabilityDataRLE.getLastTimestamp();
        Integer[] mids = new Integer[1];
        mids[0] = MEAS_ID;
        List avails = dao.getHistoricalAvails(mids, start, end, descending);
        for (Iterator it=avails.iterator(); it.hasNext(); ) {
            AvailabilityDataRLE avail = (AvailabilityDataRLE)it.next();
            dao.remove(avail);
        }
        _log.info("deleted " + avails.size() + " rows from " + AVAIL_TAB +
                  " with measurement Id = " + MEAS_ID);
    }

    private AvailabilityDataDAO getAvailabilityDataDAO() {
        return new AvailabilityDataDAO(DAOFactory.getDAOFactory());
    }
    
    public static AvailabilityManager_testLocal getOne() {
        try {
            return AvailabilityManager_testUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public void ejbCreate() throws CreateException {}
    public void ejbActivate() throws EJBException, RemoteException {}
    public void ejbPassivate() throws EJBException, RemoteException {}
    public void ejbRemove() throws EJBException, RemoteException {}
    public void setSessionContext(SessionContext arg0) {}
}
