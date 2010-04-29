/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.measurement.galerts;

import java.util.List;
import java.util.ResourceBundle;

import org.hyperic.util.HypericEnum;

/**
 * This class is used to compare 2 floats (metric values) for the
 * {@link MeasurementGtrigger}.  
 */
public class ComparisonOperator 
    extends HypericEnum
{
    private static final String BUNDLE = "org.hyperic.hq.measurement.Resources";
    
    public static final ComparisonOperator LT = 
        new ComparisonOperator(0, "<", "compare.lt", new BinaryComparison() {
            public boolean isTrue(Number one, Number two) {
                return one.doubleValue() < two.doubleValue();
            }
        });
    public static final ComparisonOperator LE = 
        new ComparisonOperator(1, "<=", "compare.le", new BinaryComparison() {
            public boolean isTrue(Number one, Number two) {
                return one.doubleValue() <= two.doubleValue();
            }
        });
    public static final ComparisonOperator GT = 
        new ComparisonOperator(2, ">", "compare.gt", new BinaryComparison() {
            public boolean isTrue(Number one, Number two) {
                return one.doubleValue() > two.doubleValue();
            }
        });
    public static final ComparisonOperator GE = 
        new ComparisonOperator(3, ">=", "compare.ge", new BinaryComparison() {
            public boolean isTrue(Number one, Number two) {
                return one.doubleValue() >= two.doubleValue();
            }
        });
    public static final ComparisonOperator EQ = 
        new ComparisonOperator(4, "==", "compare.eq", new BinaryComparison() {
            public boolean isTrue(Number one, Number two) {
                return one.equals(two);
            }
        });
    public static final ComparisonOperator NE = 
        new ComparisonOperator(5, "!=", "compare.ne", new BinaryComparison() {
            public boolean isTrue(Number one, Number two) {
                return !one.equals(two);
            }
        });

    private final BinaryComparison _compar;
    
    private interface BinaryComparison {
        public boolean isTrue(Number one, Number two /* Hah! */);
    }

    private ComparisonOperator(int code, String label, String localeProp,
                               BinaryComparison compar) 
    {
        super(code, label, localeProp, ResourceBundle.getBundle(BUNDLE));
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
