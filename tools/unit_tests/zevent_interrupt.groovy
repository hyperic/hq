//  This test demonstrates the Zevent bus ability to interrupt 
//  listeners that take > 60 seconds


import org.hyperic.hq.zevents.ZeventManager
import org.hyperic.hq.zevents.ZeventListener
import org.hyperic.hq.measurement.server.session.MeasurementZevent
import org.hyperic.hq.product.MetricValue

def varmap = [val:false]
def x = {events ->
   println 'in event handler'
   Thread.sleep(75 * 1000)
   // should never get here
   varmap.val = true
} as ZeventListener

def z = new MeasurementZevent(10001, new MetricValue(1000, 0))

ZeventManager.instance.addListener(MeasurementZevent, x)
ZeventManager.instance.enqueueEvents([z])

Thread.sleep(80 * 1000)
assert varmap.val == false


