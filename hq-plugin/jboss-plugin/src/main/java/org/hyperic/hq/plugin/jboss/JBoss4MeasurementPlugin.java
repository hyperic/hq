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

package org.hyperic.hq.plugin.jboss;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.StringUtil;

public class JBoss4MeasurementPlugin extends JBossMeasurementPlugin {

    private static final String TMPL_STATE = ":" + ATTR_STATE_MANAGEABLE;
    private static final String TMPL_STATISTIC = ":" + ATTR_STATISTIC;

    //jboss 4.0 changed the case of these attributes.
    //rather than maintain two sets of metrics in hq-plugin.xml
    //we make the adjustments here.
    public String translate(String template, ConfigResponse config) {
        if (template.indexOf(TMPL_STATE) != -1) {
            template = StringUtil.replace(template,
                                          TMPL_STATE,
                                          ":stateManageable");
        }
        else if (template.indexOf(TMPL_STATISTIC) != -1) {
            template = StringUtil.replace(template,
                                          TMPL_STATISTIC,
                                          ":statistic");
        }

        return super.translate(template, config);
    }
}
