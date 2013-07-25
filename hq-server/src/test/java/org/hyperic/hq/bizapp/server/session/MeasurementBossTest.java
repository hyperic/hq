package org.hyperic.hq.bizapp.server.session;

import junit.framework.TestCase;

import org.hyperic.hq.measurement.MeasurementConstants;

public class MeasurementBossTest
    extends TestCase { 
    
    private MeasurementBossImpl measurementBoss;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        measurementBoss = new MeasurementBossImpl(null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null);
    }

    public void testGetAvailabilityForGroups() throws Exception{
        double allGreen[] = {MeasurementConstants.AVAIL_UP, MeasurementConstants.AVAIL_UP, MeasurementConstants.AVAIL_UP};
        assertEquals("Should be UP/GREEN", MeasurementConstants.AVAIL_UP, measurementBoss.getCalculatedGroupAvailability(allGreen));
        double allRed[] = {MeasurementConstants.AVAIL_DOWN, MeasurementConstants.AVAIL_DOWN, MeasurementConstants.AVAIL_DOWN};
        assertEquals("Should be DOWN/RED", MeasurementConstants.AVAIL_DOWN, measurementBoss.getCalculatedGroupAvailability(allRed));
        double allYellow[] = {MeasurementConstants.AVAIL_WARN, MeasurementConstants.AVAIL_WARN, MeasurementConstants.AVAIL_WARN};
        assertEquals("Should be WARN/YELLOW", MeasurementConstants.AVAIL_WARN, measurementBoss.getCalculatedGroupAvailability(allYellow));
        double allGrey[] = {MeasurementConstants.AVAIL_UNKNOWN, MeasurementConstants.AVAIL_UNKNOWN, MeasurementConstants.AVAIL_UNKNOWN};
        assertEquals("Should be UNKNOWN/GREY", MeasurementConstants.AVAIL_UNKNOWN, measurementBoss.getCalculatedGroupAvailability(allGrey));
        double allOrange[] = {MeasurementConstants.AVAIL_PAUSED, MeasurementConstants.AVAIL_PAUSED, MeasurementConstants.AVAIL_PAUSED};
        assertEquals("Should be PAUSED/ORANGE", MeasurementConstants.AVAIL_PAUSED, measurementBoss.getCalculatedGroupAvailability(allOrange));
        double allBlack[] = {MeasurementConstants.AVAIL_POWERED_OFF, MeasurementConstants.AVAIL_POWERED_OFF, MeasurementConstants.AVAIL_POWERED_OFF};
        assertEquals("Should be OFF/BLACK", MeasurementConstants.AVAIL_POWERED_OFF, measurementBoss.getCalculatedGroupAvailability(allBlack));
        double yellowRedGreenGrey[] = {MeasurementConstants.AVAIL_WARN, MeasurementConstants.AVAIL_DOWN, MeasurementConstants.AVAIL_UP, MeasurementConstants.AVAIL_UNKNOWN};
        assertEquals("Should be WARN/YELLOW", MeasurementConstants.AVAIL_WARN, measurementBoss.getCalculatedGroupAvailability(yellowRedGreenGrey));
        double yellowGreyBlack[] = {MeasurementConstants.AVAIL_WARN, MeasurementConstants.AVAIL_UNKNOWN, MeasurementConstants.AVAIL_POWERED_OFF};
        assertEquals("Should be WARN/YELLOW", MeasurementConstants.AVAIL_WARN, measurementBoss.getCalculatedGroupAvailability(yellowGreyBlack));
        double yellowGreenOrange[] = {MeasurementConstants.AVAIL_WARN, MeasurementConstants.AVAIL_UP, MeasurementConstants.AVAIL_PAUSED};
        assertEquals("Should be WARN/YELLOW", MeasurementConstants.AVAIL_WARN, measurementBoss.getCalculatedGroupAvailability(yellowGreenOrange));
        double yellowBlackOrange[] = {MeasurementConstants.AVAIL_WARN, MeasurementConstants.AVAIL_POWERED_OFF, MeasurementConstants.AVAIL_PAUSED};
        assertEquals("Should be WARN/YELLOW", MeasurementConstants.AVAIL_WARN, measurementBoss.getCalculatedGroupAvailability(yellowBlackOrange));
        double redGreenGreyOrangeBlack[] = {MeasurementConstants.AVAIL_DOWN, MeasurementConstants.AVAIL_UP, MeasurementConstants.AVAIL_PAUSED, MeasurementConstants.AVAIL_POWERED_OFF};
        assertEquals("Should be WARN/YELLOW", MeasurementConstants.AVAIL_WARN, measurementBoss.getCalculatedGroupAvailability(redGreenGreyOrangeBlack));
        double redGreyBlackOrange[] = {MeasurementConstants.AVAIL_DOWN, MeasurementConstants.AVAIL_UNKNOWN, MeasurementConstants.AVAIL_POWERED_OFF, MeasurementConstants.AVAIL_PAUSED};
        assertEquals("Should be DOWN/RED", MeasurementConstants.AVAIL_DOWN, measurementBoss.getCalculatedGroupAvailability(redGreyBlackOrange));
        double greenGreyBlackOrange[] = {MeasurementConstants.AVAIL_UP, MeasurementConstants.AVAIL_UNKNOWN, MeasurementConstants.AVAIL_POWERED_OFF, MeasurementConstants.AVAIL_PAUSED};
        assertEquals("Should be UNKNOWN/GREY", MeasurementConstants.AVAIL_UNKNOWN, measurementBoss.getCalculatedGroupAvailability(greenGreyBlackOrange));
        double greenBlackOrange[] = {MeasurementConstants.AVAIL_UP, MeasurementConstants.AVAIL_POWERED_OFF, MeasurementConstants.AVAIL_PAUSED};
        assertEquals("Should be UP/GREEN", MeasurementConstants.AVAIL_UP, measurementBoss.getCalculatedGroupAvailability(greenBlackOrange));
    }

}
