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

package org.hyperic.hq.ui.taglib.display;

import java.util.Locale;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.taglib.TagUtils;
import org.apache.struts.util.RequestUtils;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.util.units.FormattedNumber;
import org.hyperic.util.units.UnitNumber;
import org.hyperic.util.units.UnitsConstants;
import org.hyperic.util.units.UnitsFormat;

/**
 * This class is a two in one decorator/tag for use within the
 * <code>TableTag</code>; it is a <code>ColumnDecorator</code> tag
 * that converts and formats metric values for display.
 */
public class MetricDecorator extends ColumnDecorator implements Tag {

    //----------------------------------------------------static variables

    protected String locale = org.apache.struts.Globals.LOCALE_KEY;    
    protected String bundle = org.apache.struts.Globals.MESSAGES_KEY;

    protected static String MS_KEY = "metric.tag.units.s.arg";

    protected static Log log =
        LogFactory.getLog(MetricDecorator.class.getName());

    //----------------------------------------------------instance variables

    // tag attrs
    private String defaultKey;
    private String unit;

    private PageContext context;
    private Tag parent;

    //----------------------------------------------------constructors

    public MetricDecorator() {
        super();
    }

    //----------------------------------------------------public methods

    public String getDefaultKey() {
        return defaultKey;
    }

    public void setDefaultKey(String s) {
        defaultKey = s;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String s) {
        unit = s;
    }

    public String decorate(Object obj) throws Exception {
        try {
            // if the metric value is empty, converting to a Double
            // will give a value of 0.0. this makes it impossible for
            // us to distinguish further down the line whether the
            // metric was actually collected with a value of 0.0 or
            // whether it was not collected at all. therefore, we'll
            // let m be null if the metric was not collected, and
            // we'll check for null later when handling the not-avail
            // case.
            // PR: 7588
            Double m = null;
            if (obj != null) {
                String mval =
                    (String) evalAttr("metric", obj.toString(), String.class);
                if (mval != null && ! mval.equals("")) {
                    m = new Double(mval);
                }
            }

            String u = (String) evalAttr("unit", getUnit(), String.class);
            String dk = (String) evalAttr("defaultKey", getDefaultKey(),
                                          String.class);
            Locale l = RequestUtils.retrieveUserLocale(context, locale);

            StringBuffer buf = new StringBuffer();

            if ((m == null ||
                 Double.isNaN(m.doubleValue()) ||
                 Double.isInfinite(m.doubleValue())) &&
                dk != null) {
                buf.append(TagUtils.getInstance().message(context, bundle,
                                                          l.toString(), dk));
            }
            else if (u.equals("ms")) {
                // we don't care about scaling and such. we just want
                // to show every metric in seconds with millisecond
                // resolution
                String formatted = UnitsFormat.format(new UnitNumber(m.doubleValue(),
                    UnitsConstants.UNIT_DURATION, UnitsConstants.SCALE_MILLI)).toString();
                buf.append(formatted);
            }
            else {
                FormattedNumber f =
                    UnitsConvert.convert(m.doubleValue(), u, l);
                buf.append(f.getValue());
                if (f.getTag() != null && f.getTag().length() > 0)
                    buf.append(" ").append(f.getTag());
            }

            return buf.toString();
        }
        catch (JspException je) {
            log.error(je);
            throw je;
        }
        catch (Exception e) {
            log.error(e);
            throw new JspException(e);
        }
    }

    public int doStartTag() throws JspTagException {
        ColumnTag ancestorTag =
            (ColumnTag)TagSupport.findAncestorWithClass(this, ColumnTag.class);
        if (ancestorTag == null) {
            throw new JspTagException("A MetricDecorator must be used within a ColumnTag.");
        }
        ancestorTag.setDecorator(this);
        return SKIP_BODY;
    }

    public int doEndTag() {
        return EVAL_PAGE;
    }

    public Tag getParent() {
        return parent;
    }
    public void setParent(Tag t) {
        this.parent = t;
    }

    public void setPageContext(PageContext pc) {
        this.context = pc;
    }

    public void release() {
        parent = null;
        context = null;
        defaultKey = null;
        unit = null;
    }

    private Object evalAttr(String name, String value, Class type)
        throws JspException, NullAttributeException {
        return ExpressionUtil.evalNotNull("metricdecorator", name, value,
                                          type, this, context);
    }
}
