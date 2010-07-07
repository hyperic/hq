package org.hyperic.hq.measurement.agent.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.hyperic.hq.agent.server.AgentStartException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.measurement.agent.ScheduledMeasurement;
import org.hyperic.hq.product.MeasurementValueGetter;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.util.schedule.UnscheduledItemException;


public class ScheduleThreadTest extends TestCase {

    private static final String platformTemplate = (".system.avail:Type=Platform:Availability").toLowerCase();
    private static final String ordinaryTemplate = (".ordinary.metric:Type=Stuff:Metric").toLowerCase();
    
    private static final long SLOW_COLLECTION_DELAY = 5000;
    
    public void testSimpleStartKill() {
        
        try {
            
            ScheduleThread st = new ScheduleThread(new NullSender(), new NullValueGetter());
            Thread t = new Thread(st);
            t.start();
            
            // No measurement schedule, the thing should just sit there
            assertTrue(t.isAlive());
            assertFalse(t.isInterrupted());
            long startWait = System.currentTimeMillis();
            long waitTime = 100;
            try {
                t.join(waitTime);
            } catch (InterruptedException ie) {
                fail("Thread should not have been interrupted");
            }
            
            assertTrue(System.currentTimeMillis() - startWait >= waitTime);
            
            st.die();

            try {
                t.join();
                assertTrue(true);
            } catch (InterruptedException ie) {
                fail("Thread should not be interrupted");
            }

        } catch (AgentStartException ase) {
         
            fail("Unexpected Exception AgentStartException");

        }
    }
    
    public void testSimpleCollection() {
        
        try {

            List<ScheduledMeasurement> onePlatformAndChildren = createOnePlatformAndChildrenForScheduling("SimpleColl");
            ScheduleThread st = new ScheduleThread(new NullSender(), new NullValueGetter());
            for (ScheduledMeasurement sm : onePlatformAndChildren) {
                st.scheduleMeasurement(sm);
            }
            
            Thread t = new Thread(st);
            t.start();
            
            st.die();

            try {
                t.join();
                assertTrue(true);
            } catch (InterruptedException ie) {
                fail("Thread should not be interrupted");
            }

        } catch (AgentStartException ase) {
            
            fail("Unexpected Exception AgentStartException");

        }
    }
    
    public void testInterceptedCollection() {
        
        try {

            List<ScheduledMeasurement> onePlatformAndChildren = createOnePlatformAndChildrenForScheduling("Intercepted");
            ScheduleThread st = new NonCollectingScheduleThread(new NullSender(), new NullValueGetter());
            for (ScheduledMeasurement sm : onePlatformAndChildren) {
                st.scheduleMeasurement(sm);
            }
            
            Thread t = new Thread(st);
            t.start();
            
            try {
                Thread.sleep(60000);
            } catch (InterruptedException ie) {
                
            }
            
            st.die();

            // No real assertions here, just iteratively testing that nothing breaks.
            
            try {
                t.join();
                assertTrue(true);
            } catch (InterruptedException ie) {
                fail("Thread should not be interrupted");
            }

        } catch (AgentStartException ase) {
            
            fail("Unexpected Exception AgentStartException");

        }
    }
    
    public void testCollectionCounts() {
        
        // Using createOnePlatformAndChildrenForScheduling measurements and their hard-coded intervals,
        // verify that the expected collection counts happen
        try {

            List<ScheduledMeasurement> onePlatformAndChildren = createOnePlatformAndChildrenForScheduling("Counts");
            CountingValueGetter cvg = new CountingValueGetter();
            NonCollectingScheduleThread st = new NonCollectingScheduleThread(new NullSender(), cvg);
            for (ScheduledMeasurement sm : onePlatformAndChildren) {
                st.scheduleMeasurement(sm);
            }
            
            Thread t = new Thread(st);
            t.start();
            
            // Sleep for a few collection cycles.  The idea is to see if the proportion of collections matches
            // the relative metric frequencies.
            try {
                Thread.sleep(80000);
            } catch (InterruptedException ie) {
                
            }
            
            // Now verify the sanity of the counts:
            //  Platform Availability >= (2 * other platform metrics)
            //  Platform Availability >= (2 * server metrics)
            //  Platform Availability >= (3 * service metrics)
            //  Platform non-availibility == server metrics
            //  ({Platform non-availability, server metrics} * 2) >= (service metrics * 3)
            int itemFactor = 0;
            int platformServerFactor = 0;
            int serviceFactor = 0;
            
            while (!onePlatformAndChildren.isEmpty()) {
                
                ScheduledMeasurement item = onePlatformAndChildren.remove(0);
                int itemCount = getCountFor(item, st, cvg);
                
                assertTrue("Count for " + item.getDSN() + " should not be zero", itemCount > 0);
                
                String itemDSN = item.getDSN();
                if (itemDSN.toLowerCase().endsWith(platformTemplate.toLowerCase())) {
                    itemFactor = 1;
                    platformServerFactor = 2;
                    serviceFactor = 3;
                } else if (itemDSN.indexOf("platform") >= 0 || itemDSN.indexOf("server") >= 0) {
                    itemFactor = 2;
                    platformServerFactor = 2;
                    serviceFactor = 3;
                } else if (itemDSN.indexOf("service") >= 0) {
                    itemFactor = 3;
                    platformServerFactor = 2;
                    serviceFactor = 3;
                }
                
                int leftFactor = getCountFor(item, st, cvg) * itemFactor;
                
                for (ScheduledMeasurement sm : onePlatformAndChildren) {
                    String dsn = sm.getDSN();

                    int rightFactor = 0;
                    
                    if (dsn.indexOf("platform") >= 0 || dsn.indexOf("server") >= 0) {
                        rightFactor = platformServerFactor * getCountFor(sm, st, cvg);
                    } else if (dsn.indexOf("service") >= 0) {
                        rightFactor = serviceFactor * getCountFor(sm, st, cvg);
                    }
                    
                    assertTrue("Count for " + itemDSN +
                               "=" + getCountFor(item, st, cvg) +
                               ", for " + dsn + "=" + getCountFor(sm, st, cvg),
                               leftFactor + 1 >= rightFactor);
                }
            }
            
            st.die();

            // No real assertions here, just iteratively testing that nothing breaks.
            
            try {
                t.join();
                assertTrue(true);
            } catch (InterruptedException ie) {
                fail("Thread should not be interrupted");
            }

        } catch (AgentStartException ase) {
            
            fail("Unexpected Exception AgentStartException");

        }
    }
    
    public void testWithSlowCollection() {
        
        try {

            List<ScheduledMeasurement> onePlatformAndChildren = createOnePlatformAndChildrenForScheduling("SlowBase");
            ScheduleThread st = new SlowCollectingScheduleThread(new NullSender(), new NullValueGetter());
            for (ScheduledMeasurement sm : onePlatformAndChildren) {
                st.scheduleMeasurement(sm);
            }
            
            Thread t = new Thread(st);
            t.start();
            
            // Approximate the time two collection cycles will take
            int resourceCount = onePlatformAndChildren.size();
            long approxCollectionTime = SLOW_COLLECTION_DELAY * resourceCount * 2;
            int nWorkers = 40;
            MeasurementSchedulerUnscheduler[] workers = new MeasurementSchedulerUnscheduler[nWorkers];
            for (int i = 0; i < nWorkers; ++i) {
                workers[i] = new MeasurementSchedulerUnscheduler("Worker" + i, i * 2000, st);
                workers[i].start();
            }
            
            try {
                Thread.sleep(approxCollectionTime);
            } catch (InterruptedException ie) {
                
            }
            
            for (int i = 0; i < nWorkers; ++i) {
                workers[i].terminate();
            }
            
            st.die();

            for (int i = 0; i < nWorkers; ++i) {
                assertFalse(String.valueOf(i), workers[i].isFailed());
                int opCount = workers[i].getOpCount();
                assertTrue("Worker " + i + " had " + opCount + " ops, resource count=" + resourceCount,
                           opCount > resourceCount * 5);
            }
            
            // No real assertions here, just iteratively testing that nothing breaks.
            
            try {
                t.join();
                assertTrue(true);
            } catch (InterruptedException ie) {
                fail("Thread should not be interrupted");
            }

        } catch (AgentStartException ase) {
            
            fail("Unexpected Exception AgentStartException");

        }
    }
    
    private int getCountFor(ScheduledMeasurement item, NonCollectingScheduleThread st, CountingValueGetter cvg) {
        String itemId = st.getStringFor(item);
        return cvg.getCountFor(itemId);
    }
    
    private List<ScheduledMeasurement> createOnePlatformAndChildrenForScheduling(String nameBase) {
        return createOnePlatformAndChildrenForScheduling(nameBase, 1000, true);
    }
    
    private List<ScheduledMeasurement> createOnePlatformAndChildrenForScheduling(String nameBase, int idBase, boolean includePlatformAvail) {
        
        // If you mess with the metric intervals, testCollectionCounts() will likely break unless it is also updated.
        
        List<ScheduledMeasurement> result = new ArrayList<ScheduledMeasurement>();
        
        String platformBase = "noplugin:" + nameBase + ".platform";
        // Platform availability
        if (includePlatformAvail) {
            result.add(makeMeasurement(platformBase, idBase, AppdefEntityConstants.APPDEF_TYPE_PLATFORM, true, 10000));
            idBase += 10;
        }
        
        // Misc other platform measurement
        result.add(makeMeasurement(platformBase, idBase, AppdefEntityConstants.APPDEF_TYPE_PLATFORM, false, 20000));
        idBase += 10;
        
        // Server 1
        String serverBase1 = platformBase + ".server1.avail";
        result.add(makeMeasurement(serverBase1, idBase, AppdefEntityConstants.APPDEF_TYPE_SERVER, true, 20000));
        idBase += 10;
        String serverBase2 = platformBase + ".server1.m1";
        result.add(makeMeasurement(serverBase2, idBase, AppdefEntityConstants.APPDEF_TYPE_SERVER, false, 20000));
        idBase += 10;
        String serverBase3 = platformBase + ".server1.m2";
        result.add(makeMeasurement(serverBase3, idBase, AppdefEntityConstants.APPDEF_TYPE_SERVER, false, 20000));
        idBase += 10;
        
        // Services
        result.add(makeMeasurement(serverBase1 + ".service1", idBase, AppdefEntityConstants.APPDEF_TYPE_SERVICE, true, 30000));
        idBase += 10;
        result.add(makeMeasurement(serverBase1 + ".service2", idBase, AppdefEntityConstants.APPDEF_TYPE_SERVICE, false, 30000));
        idBase += 10;
        result.add(makeMeasurement(serverBase2 + ".service1", idBase, AppdefEntityConstants.APPDEF_TYPE_SERVICE, true, 30000));
        idBase += 10;
        result.add(makeMeasurement(serverBase2 + ".service2", idBase, AppdefEntityConstants.APPDEF_TYPE_SERVICE, false, 30000));
        idBase += 10;
        result.add(makeMeasurement(serverBase3 + ".service1", idBase, AppdefEntityConstants.APPDEF_TYPE_SERVICE, true, 30000));
        idBase += 10;
        result.add(makeMeasurement(serverBase3 + ".service2", idBase, AppdefEntityConstants.APPDEF_TYPE_SERVICE, false, 30000));
        idBase += 10;
        
        String serverBase4 = platformBase + ".server2.avail";
        result.add(makeMeasurement(serverBase4, idBase, AppdefEntityConstants.APPDEF_TYPE_SERVER, true, 30000));
        idBase += 10;
        String serverBase5 = platformBase + ".server2.m1";
        result.add(makeMeasurement(serverBase5, idBase, AppdefEntityConstants.APPDEF_TYPE_SERVER, false, 30000));
        idBase += 10;
        String serverBase6 = platformBase + ".server2.m2";
        result.add(makeMeasurement(serverBase6, idBase, AppdefEntityConstants.APPDEF_TYPE_SERVER, false, 30000));
        idBase += 10;
        
        // Services
        result.add(makeMeasurement(serverBase4 + ".service1", idBase, AppdefEntityConstants.APPDEF_TYPE_SERVICE, true, 30000));
        idBase += 10;
        result.add(makeMeasurement(serverBase4 + ".service2", idBase, AppdefEntityConstants.APPDEF_TYPE_SERVICE, false, 30000));
        idBase += 10;
        result.add(makeMeasurement(serverBase5 + ".service1", idBase, AppdefEntityConstants.APPDEF_TYPE_SERVICE, true, 30000));
        idBase += 10;
        result.add(makeMeasurement(serverBase5 + ".service2", idBase, AppdefEntityConstants.APPDEF_TYPE_SERVICE, false, 30000));
        idBase += 10;
        result.add(makeMeasurement(serverBase6 + ".service1", idBase, AppdefEntityConstants.APPDEF_TYPE_SERVICE, true, 30000));
        idBase += 10;
        result.add(makeMeasurement(serverBase6 + ".service2", idBase, AppdefEntityConstants.APPDEF_TYPE_SERVICE, false, 30000));
        idBase += 10;

        return result;
    }
    
    private ScheduledMeasurement makeMeasurement(String nameBase, int counterBase, int type, boolean isAvail, long interval) {
        
        String suffix = (type == AppdefEntityConstants.APPDEF_TYPE_PLATFORM && isAvail) ?
                                  platformTemplate : ordinaryTemplate;
        
        String dsn = nameBase + suffix;
        int derivedID = counterBase++;
        int dsnID = counterBase++;
        AppdefEntityID ent = new AppdefEntityID(type + ":" + counterBase++);
        String category = nameBase + "_cat";
        return new ScheduledMeasurement(dsn, interval, derivedID, dsnID, ent, category);
    }
    
    public static class NullSender implements Sender {

        public void processData(int dsnId, MetricValue data, int derivedID) {
        }
    }
    
    public static class NullValueGetter implements MeasurementValueGetter {
        
        public MetricValue getValue(String name, Metric metric)
        throws PluginException, PluginNotFoundException,
               MetricNotFoundException, MetricUnreachableException {
            return MetricValue.NONE;
        } 
    }
    
    private static class CountingValueGetter implements MeasurementValueGetter {

        private Map<String, Integer> counts;
        
        public CountingValueGetter() {
            counts = new HashMap<String, Integer>();
        }

        public MetricValue getValue(String name, Metric metric) throws PluginException,
                                                               PluginNotFoundException,
                                                               MetricNotFoundException,
                                                               MetricUnreachableException
        {
            count(metric);
            return MetricValue.NONE;
        }
        
        public void count(Metric metric) {
            String id = metric.getId();
            synchronized (counts) {
                Integer existing = counts.get(id);
                if (existing == null) {
                    counts.put(id, new Integer(1));
                } else {
                    counts.put(id, new Integer(existing.intValue() + 1));
                }
            }
        }
        
        public int getCountFor(String id) {
            int result = 0;
            synchronized (counts) {
                Integer count = counts.get(id);
                if (count != null) {
                    result = count.intValue();
                }
            }
            
            return result;
        }
    }
    
    private static class NonCollectingScheduleThread extends ScheduleThread {
        
        
        NonCollectingScheduleThread(Sender sender, MeasurementValueGetter manager) throws AgentStartException {
            super(sender, manager);
        }

        public String getStringFor(ScheduledMeasurement meas) {
            String result = null;
            ParsedTemplate templ = getParsedTemplate(meas);
            if (templ != null) {
                result = templ.metric.getId();
            }
            
            return result;
        }
    }
    
    private static class SlowCollectingScheduleThread extends ScheduleThread {
        
        SlowCollectingScheduleThread(Sender sender, MeasurementValueGetter manager) throws AgentStartException {
            super(sender, manager);
        }

        protected MetricValue getValue(ParsedTemplate templ) {
            try {
                Thread.sleep(SLOW_COLLECTION_DELAY);
            } catch (InterruptedException ie) {
                
            }
            
            return MetricValue.NONE;
        }
    }
    
    private class MeasurementSchedulerUnscheduler extends Thread {

        private List<ScheduledMeasurement> measurements;
        private int nOperations;
        private ScheduleThread st;
        private boolean terminated;
        private boolean failed;
        
        public MeasurementSchedulerUnscheduler(String base, int nBase, ScheduleThread st) {
            measurements = createOnePlatformAndChildrenForScheduling(base, nBase, false);
            this.st = st;
            nOperations = 0;
            terminated = false;
            failed = false;
        }
        
        public void terminate() {
            synchronized (this) {
                terminated = true;
            }
        }
        
        public boolean isFailed() {
            return failed;
        }
        
        public int getOpCount() {
            return nOperations;
        }
        
        public void run() {
            while (!terminated) {
                for (ScheduledMeasurement sm : measurements) {
                    st.scheduleMeasurement(sm);
                    nOperations++;

                    synchronized (this) {
                        if (terminated) {
                            break;
                        }
                    }
                }
                
                if (!terminated) {

                    for (ScheduledMeasurement sm : measurements) {
                        try {
                            st.unscheduleMeasurements(sm.getEntity());
                            nOperations++;
                        } catch (UnscheduledItemException uie) {
                            uie.printStackTrace();
                            failed = true;
                        }

                        synchronized (this) {
                            if (terminated) {
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}
