/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import org.hyperic.util.ArrayUtil;

public abstract class BinaryFormatter 
    implements Formatter
{
    private NumberFormat getSpecificFormatter(FormatSpecifics specifics,
                                              Locale locale)
    {
        NumberFormat res = NumberFormat.getInstance(locale);

        if(specifics.getPrecision() == FormatSpecifics.PRECISION_MAX){
            res.setMaximumFractionDigits(100);
            res.setMinimumFractionDigits(0);
        } else {
            res.setMaximumFractionDigits(1);
            res.setMinimumFractionDigits(1);
        }
        
        return res;
    }

    public FormattedNumber format(UnitNumber val, Locale locale,
                                  FormatSpecifics specifics)
    {
        NumberFormat fmt;
        BigDecimal baseVal;
        double newVal;
        int targScale;
        
        baseVal   = val.getBaseValue();
        targScale = this.findGoodLookingScale(baseVal);
        newVal    = this.getTargetValue(baseVal, targScale);

        if(specifics == null)
            fmt = UnitsUtil.getNumberFormat(new double[] {newVal}, locale);
        else
            fmt = this.getSpecificFormatter(specifics, locale);

        return this.createFormattedValue(newVal, targScale, fmt);
    }

    public FormattedNumber[] formatSame(double[] vals, int unitType,
                                        int scale, Locale locale,
                                        FormatSpecifics specifics)
    {
        FormattedNumber[] res;
        NumberFormat fmt;
        UnitNumber tmpNum;
        double[] newVals;
        double average;
        int targScale;

        res = new FormattedNumber[vals.length];

        if(vals.length == 0){
            return res;
        }

        average = ArrayUtil.average(vals);

        tmpNum = new UnitNumber(average, unitType, scale);
        targScale = this.findGoodLookingScale(tmpNum.getBaseValue());
        
        newVals = new double[vals.length];
        for(int i=0; i<vals.length; i++){
            tmpNum     = new UnitNumber(vals[i], unitType, scale);
            newVals[i] = this.getTargetValue(tmpNum.getBaseValue(), targScale);
        }
        
        if(specifics == null)
            fmt = UnitsUtil.getNumberFormat(newVals, locale);
        else
            fmt = this.getSpecificFormatter(specifics, locale);

        for(int i=0; i<vals.length; i++){
            res[i] = this.createFormattedValue(newVals[i], targScale, fmt);
        }
        return res;
    }

    protected abstract String getTagName();

    protected FormattedNumber createFormattedValue(double value, int scale,
                                                   NumberFormat fmt)
    {
        String tag;

        switch(scale){
        case UnitsConstants.SCALE_NONE:
            tag = "";
            break;
        case UnitsConstants.SCALE_KILO:
            tag = "K";
            break;
        case UnitsConstants.SCALE_MEGA:
            tag = "M";
            break;
        case UnitsConstants.SCALE_GIGA:
            tag = "G";
            break;
        case UnitsConstants.SCALE_TERA:
            tag = "T";
            break;
        case UnitsConstants.SCALE_PETA:
            tag = "P";
            break;
        default:
            throw new IllegalStateException("Unhandled scale");
        }

        return new FormattedNumber(fmt.format(value), tag + this.getTagName());
    }

    private double getTargetValue(BigDecimal baseVal, int targetScale){
        BigDecimal modifier;
        double lateModifier;

        modifier     = UnitsUtil.FACT_NONE;
        lateModifier = 1.0;

        switch(targetScale){
        case UnitsConstants.SCALE_KILO:
            lateModifier = 1 << 10;
            break;
        case UnitsConstants.SCALE_MEGA:
            lateModifier = 1 << 20;
            break;
        case UnitsConstants.SCALE_GIGA:
            modifier     = UnitsUtil.FACT_MEGA_BIN;
            lateModifier = 1 << 10;
            break;
        case UnitsConstants.SCALE_TERA:
            modifier     = UnitsUtil.FACT_GIGA_BIN;
            lateModifier = 1 << 10;
            break;
        case UnitsConstants.SCALE_PETA:
            modifier     = UnitsUtil.FACT_TERA_BIN;
            lateModifier = 1 << 10;
            break;
        }

        baseVal = baseVal.divide(modifier, BigDecimal.ROUND_HALF_EVEN);
        return baseVal.doubleValue() / lateModifier;
    }

    private int findGoodLookingScale(BigDecimal val){
        if(val.compareTo(UnitsUtil.FACT_PETA_BIN) >= 1){
            return UnitsConstants.SCALE_PETA;
        } else if(val.compareTo(UnitsUtil.FACT_TERA_BIN) >= 1){
            return UnitsConstants.SCALE_TERA;
        } else if(val.compareTo(UnitsUtil.FACT_GIGA_BIN) >= 1){
            return UnitsConstants.SCALE_GIGA;
        } else if(val.compareTo(UnitsUtil.FACT_MEGA_BIN) >= 1){
            return UnitsConstants.SCALE_MEGA;
        } else if(val.compareTo(UnitsUtil.FACT_KILO_BIN) >= 1){
            return UnitsConstants.SCALE_KILO;
        } else {
            return UnitsConstants.SCALE_NONE;
        }
    }

    public BigDecimal getBaseValue(double value, int scale){
        BigDecimal res;

        res = new BigDecimal(value);
        return res.multiply(this.getScaleCoeff(scale));
    }

    public BigDecimal getScaledValue(BigDecimal value, int targScale){
        return value.divide(this.getScaleCoeff(targScale),
                            BigDecimal.ROUND_HALF_EVEN);
    }

    private BigDecimal getScaleCoeff(int scale){
        switch(scale){
        case UnitsConstants.SCALE_NONE:
            return UnitsUtil.FACT_NONE;
        case UnitsConstants.SCALE_BIT:
            return UnitsUtil.FACT_BIT;
        case UnitsConstants.SCALE_KILO:
            return UnitsUtil.FACT_KILO_BIN;
        case UnitsConstants.SCALE_MEGA:
            return UnitsUtil.FACT_MEGA_BIN;
        case UnitsConstants.SCALE_GIGA:
            return UnitsUtil.FACT_GIGA_BIN;
        case UnitsConstants.SCALE_TERA:
            return UnitsUtil.FACT_TERA_BIN;
        case UnitsConstants.SCALE_PETA:
            return UnitsUtil.FACT_PETA_BIN;
        }

        throw new IllegalArgumentException("Value did not have binary " +
                                           "based scale");
    }

    protected abstract UnitNumber parseTag(double number, String tag, 
                                           int tagIdx, 
                                           ParseSpecifics specifics)
        throws ParseException;

    public UnitNumber parse(String val, Locale locale, 
                            ParseSpecifics specifics)
        throws ParseException
    {
        NumberFormat fmt = NumberFormat.getInstance(locale);
        double numberPart;
        int nonIdx;

        nonIdx = UnitsUtil.findNonNumberIdx(val, fmt);
        if(nonIdx == -1){
            throw new ParseException("Number had no units with it", 
                                     val.length());
        }

        if(nonIdx == 0){
            throw new ParseException("Invalid number specified", 0);
        }

        numberPart = fmt.parse(val.substring(0, nonIdx)).doubleValue();
        return this.parseTag(numberPart, 
                             val.substring(nonIdx, val.length()).trim(),
                             nonIdx, specifics);
    }
}
