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

package org.hyperic.util.units;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Formatter for converting cents to dollars.
 * XXX: Use locale to make this more generic?
 */
public class CurrencyFormatter extends SimpleFormatter
{
    protected String getTagName(){
        return "cents";
    }

    protected int getUnitType(){
        return UnitsConstants.UNIT_CURRENCY;
    }

    protected int getUnitScale(){
        return UnitsConstants.SCALE_NONE;
    }

    protected NumberFormat getNumberFormat(Locale locale){
        NumberFormat res = NumberFormat.getInstance(locale);

        res.setMinimumFractionDigits(2);
        res.setMaximumFractionDigits(2);
        return res;
    }

    protected FormattedNumber formatNumber(double rawValue, NumberFormat fmt){
        String num = fmt.getCurrencyInstance(Locale.US).format(rawValue/100);
        return new FormattedNumber(num, "", " ");
    }
}
