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

package org.hyperic.util;

import java.text.NumberFormat;
import java.text.ParseException;

/**
 * Utilities for dealing with number parsing / formatting.  This class
 * is mainly meant to be used by java beans and other classes that
 * must do number formatting but cannot throw any exceptions during
 * the process.
 *
 */

public final class NumberUtil {
    /**
     * Constant value representing "not a number".
     */
    public static final Number NaN = new Double(Double.NaN);

    // static class, privatize ctor
    private NumberUtil() {}

    /**
     * Parse the <code>input</code> string (using the default locale)
     * and return its numeric representation.  If the
     * <code>input</code> string is null, empty or cannot be parsed,
     * return <code>{@link NaN}<code>.
     */
    public static Number stringAsNumber(String input) {
        return _parse( input, NumberFormat.getNumberInstance() );
    }

    /**
     * Format the <code>input</code> number as a string (using the
     * default locale).  If the <code>input</code> number is
     * <code>{@link NaN}</code>, return null.
     */
    public static String numberAsString(double input) {
        return _format( input, NumberFormat.getNumberInstance() );
    }

    /**
     * Parse the <code>input</code> string (using the default locale)
     * and return its numeric percentage representation.  If the
     * <code>input</code> string is null, empty or cannot be parsed,
     * return <code>{@link NaN}<code>.
     */
    public static Number stringAsPercentage(String input) {
        return _parse( input, NumberFormat.getPercentInstance() );
    }

    /**
     * Format the <code>input</code> percentage as a string (using the
     * default locale).  If the <code>input</code> percentage is
     * <code>{@link NaN}</code>, return null.
     */
    public static String percentageAsString(double input) {
        return _format( input, NumberFormat.getPercentInstance() );
    }

    //------------------------------------------------------------------
    //-- private helpers
    //------------------------------------------------------------------
    private static Number _parse(String input, NumberFormat fmt) {
        if (null != input && input.trim().length() > 0) {
            try {
                return fmt.parse(input);
            } catch (ParseException e) {
                return NaN;
            }
        } else {
            return NaN;
        }
    }

    private static String _format(double input, NumberFormat fmt) {
        if (NaN.doubleValue() != input) {
            return fmt.format(input);
        } else {
            return null;
        }
    }
}

// EOF
