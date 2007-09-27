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

package org.hyperic.util.math;

/**
 * This class contains some mathematic utility methods.
 */
public final class MathUtil {

    public static final Integer NEGATIVE_ONE = new Integer(-1);
    public static final Integer ZERO = new Integer(0);

    /**
     * Find the greatest common divisor of both <code>n</code> and
     * <code>m</code>.
     *
     * @param n first number
     * @param m second number
     * @return the GCD of n and m, or 1 if both numbers are 0
     */
    public static int gcd(int n, int m) {
        n = Math.abs(n);
        m = Math.abs(m);

        // avoid infinite recursion
        if (n == 0 && m == 0) {
            return 1;
        }
        if (n == m && n >= 1) {
            return n;
        }

        return (m < n) ? gcd(n-m, n) : gcd(n, m-n);
    }

    /**
     * Find the least common multiple of <code>n</code> and
     * <code>m</code>.
     *
     * @param n first number
     * @param m second number
     * @return the LCM of n and m
     */
    public static int lcm(int n, int m) {
        return m * ( n / gcd(n, m) );
    }

    /**
     * Like StringUtil.compare, but for numbers.
     * @see org.hyperic.util.StringUtil#compare
     */
    public static boolean compare ( Number n1, Number n2 ) {
        if ( n1 == n2 ) return true;
        if ( n1 == null || n2 == null ) return false;
        return n1.equals(n2);
    }

    public static long clamp(long val, long min, long max) {
        if(val < min)
            return min;
        
        if(val > max)
            return max;
    
        return val;
    }

    /**
     * Clamp a value to a range.  If the passed value is 
     * less than the minimum, return the minimum.  If it
     * is greater than the maximum, assign the maximum.
     * else return the passed value.
     */
    public static int clamp(int val, int min, int max) {
        return (int)clamp((long)val, (long)min, (long)max);
    }
}

// EOF
