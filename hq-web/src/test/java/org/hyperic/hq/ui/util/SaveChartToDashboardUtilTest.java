package org.hyperic.hq.ui.util;

import junit.framework.TestCase;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.ui.util.SaveChartToDashboardUtil;

/**
 * Unit test of the {@link SaveChartToDashboardUtil}
 *
 */
public class SaveChartToDashboardUtilTest extends TestCase {
        
    public void testGetAppdefEntityIDFromChartUrl() {                
        String expected = "1:10246";
        
        // add urls to test here
        String[] urls = 
            new String[] {
                "/resource/common/monitor/Visibility.do?mode=chartSingleMetricSingleResource&type=1&rid=10246&m=10520&showLowRange=true&showAverage=true&showEvents=true&showValues=true&showLow=true&showBaseline=true&threshold=&showHighRange=true&showPeak=true",
                "/resource/common/monitor/Visibility.do?type=1&ctype=3%3A10006&rid=10246&ctype=3%3A10006&showHighRange=true&showLow=true&showBaseline=true&showEvents=true&m=10512&showLowRange=true&showAverage=true&showValues=true&showPeak=true&mode=chartSingleMetricMultiResource&threshold=",
                "/resource/common/monitor/Visibility.do?ctype=3%3A10111&type=1&rid=10246&m=11086&showLowRange=true&showAverage=true&showEvents=true&showValues=true&ctype=3%3A10111&mode=chartSingleMetricMultiResource&showLow=true&showBaseline=true&threshold=&showHighRange=true&showPeak=true",
                "/resource/common/monitor/Visibility.do?rid=10246&ctype=3%3A10111&type=1&m=11086&showLowRange=true&showAverage=true&showEvents=true&showValues=true&ctype=3%3A10111&mode=chartSingleMetricMultiResource&showLow=true&showBaseline=true&threshold=&showHighRange=true&showPeak=true",
                "/resource/common/monitor/Visibility.do?ctype=3%3A10111&rid=10246&type=1&m=11086&showLowRange=true&showAverage=true&showEvents=true&showValues=true&ctype=3%3A10111&mode=chartSingleMetricMultiResource&showLow=true&showBaseline=true&threshold=&showHighRange=true&showPeak=true"
            };
                       
        for (int u=0; u<urls.length; u++) 
        {                       
            AppdefEntityID aeid = SaveChartToDashboardUtil
                                        .getAppdefEntityIDFromChartUrl(urls[u]);
                
            assertNotNull("Could not extract the AppdefEntityID from the url: " + urls[u],
                           aeid);
            
            assertTrue("Incorrect AppdefEntityID [" + aeid + "] was extracted from the url: " + urls[u],
                        aeid.toString().equals(expected));
        }
    }
}