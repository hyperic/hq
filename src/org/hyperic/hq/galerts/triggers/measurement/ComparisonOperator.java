package org.hyperic.hq.galerts.triggers.measurement;

import java.util.List;

import org.hyperic.util.HypericEnum;

/**
 * This class is used to compare 2 floats (metric values) for the
 * {@link MeasurementGtrigger}.  
 */
public class ComparisonOperator 
    extends HypericEnum
{
    public static final ComparisonOperator LT = 
        new ComparisonOperator(0, "<", new BinaryComparison() {
            public boolean isTrue(Number one, Number two) {
                return one.doubleValue() < two.doubleValue();
            }
        });
    public static final ComparisonOperator LE = 
        new ComparisonOperator(1, "<=", new BinaryComparison() {
            public boolean isTrue(Number one, Number two) {
                return one.doubleValue() <= two.doubleValue();
            }
        });
    public static final ComparisonOperator GT = 
        new ComparisonOperator(2, ">", new BinaryComparison() {
            public boolean isTrue(Number one, Number two) {
                return one.doubleValue() > two.doubleValue();
            }
        });
    public static final ComparisonOperator GE = 
        new ComparisonOperator(3, ">=", new BinaryComparison() {
            public boolean isTrue(Number one, Number two) {
                return one.doubleValue() >= two.doubleValue();
            }
        });
    public static final ComparisonOperator EQ = 
        new ComparisonOperator(4, "==", new BinaryComparison() {
            public boolean isTrue(Number one, Number two) {
                return one.equals(two);
            }
        });
    public static final ComparisonOperator NE = 
        new ComparisonOperator(5, "!=", new BinaryComparison() {
            public boolean isTrue(Number one, Number two) {
                return !one.equals(two);
            }
        });

    private final BinaryComparison _compar;
    
    private interface BinaryComparison {
        public boolean isTrue(Number one, Number two /* Hah! */);
    }

    private ComparisonOperator(int code, String label,
                               BinaryComparison compar) 
    {
        super(code, label);
        _compar = compar;
    }
    
    /**
     * Returns true if (one COMPAR two) == true.  COMPAR is dependent on
     * the instance of the object. 
     */
    public boolean isTrue(Number one, Number two) {
        return _compar.isTrue(one, two);
    }
    
    public static List getAll() {
        return HypericEnum.getAll(ComparisonOperator.class);
    }
    
    public static ComparisonOperator findByCode(int code) {
        return (ComparisonOperator)
            HypericEnum.findByCode(ComparisonOperator.class, code);
    }
}
