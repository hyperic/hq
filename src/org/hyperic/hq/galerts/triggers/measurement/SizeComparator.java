package org.hyperic.hq.galerts.triggers.measurement;

import java.util.List;

import org.hyperic.util.HypericEnum;

/**
 * This class is used for the less/more than part of the 
 * {@link MeasurementGtrigger} comparison when determining if a number of
 * resources has met a condition.
 */
public class SizeComparator 
    extends HypericEnum
{
    public static final SizeComparator LESS_THAN = 
        new SizeComparator(0, "Less Than", ComparisonOperator.LT);
    public static final SizeComparator MORE_THAN = 
        new SizeComparator(1, "More Than", ComparisonOperator.GT);
    
    private final ComparisonOperator _op;
    
    private SizeComparator(int code, String label, ComparisonOperator op) {
        super(code, label);
        _op = op;
    }
   
    /**
     * Returns true if one is greater than or less than 2 (depending on the
     * instance of {@link SizeComparator}
     */
    public boolean isTrue(int one, int two) {
        return _op.isTrue(new Integer(one), new Integer(two));
    }
                          
    public static List getAll() {
        return HypericEnum.getAll(SizeComparator.class);
    }
    
    public static SizeComparator findByCode(int code) {
        return (SizeComparator)HypericEnum.findByCode(SizeComparator.class, 
                                                      code);
    }
}
