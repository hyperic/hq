/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2004-2011], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 */

package org.hyperic.hq.product.shared;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings("serial")
public class PluginDeployException extends Exception {
    
    public final Map<Integer, String> params;

    public PluginDeployException(String msg, Exception e, String ... params) {
        super(msg, e);
        this.params = getParams(params);
    }

    public PluginDeployException(String msg, String ... params) {
        super(msg);
        this.params = getParams(params);
    }
    
    private Map<Integer, String> getParams(String[] params) {
        if (params.length == 0) {
            return Collections.emptyMap();
        }
        final Map<Integer, String> rtn = new TreeMap<Integer, String>();
        int i=0;
        for (final String param : params) {
            rtn.put(i++, param);
        }
        return rtn;
    }

    public Map<Integer, String> getParameters() {
        return Collections.unmodifiableMap(params);
    }

}
