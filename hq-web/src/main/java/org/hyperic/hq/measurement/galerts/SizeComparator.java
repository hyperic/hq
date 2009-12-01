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
 * This class is used for the less/more than part of the 
 * {@link MeasurementGtrigger} comparison when determining if a number of
 * resources has met a condition.
 */
public class SizeComparator 
    extends HypericEnum
{
    private static final String BUNDLE = "org.hyperic.hq.measurement.Resources";
    
    public static final SizeComparator LESS_THAN = 
        new SizeComparator(0, "Fewer than", "compare.lessThan", 
                           ComparisonOperator.LT);
    public static final SizeComparator MORE_THAN = 
        new SizeComparator(1, "More than", "compare.greaterThan",
                           ComparisonOperator.GT);
    
    private final ComparisonOperator _op;
    
    private SizeComparator(int code, String label, String localeProp, 
                           ComparisonOperator op) 
    {
        super(code, label, localeProp, ResourceBundle.getBundle(BUNDLE));
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
