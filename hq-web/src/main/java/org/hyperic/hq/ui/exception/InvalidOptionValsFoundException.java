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

/*
 * Created on Jun 19, 2003
 *
 */
package org.hyperic.hq.ui.exception;

import org.hyperic.hq.common.ApplicationException;
import org.hyperic.util.config.ConfigResponse;


/**
 * This class is throw in the UI if an InvalidConfigOptionValueException
 * is thrown while iterating through a list of ConfigOptions while
 * building up the ConfigOptions.
 * 
 *
 */
public class InvalidOptionValsFoundException extends ApplicationException {

    private ConfigResponse res;
    /**
     * 
     */
    public InvalidOptionValsFoundException() {
        super();
    }

    /**
     * @param s
     */
    public InvalidOptionValsFoundException(String s) {
        super(s);
    }

    /**
     * @param s
     */
    public InvalidOptionValsFoundException(String s, ConfigResponse res) {
        super(s);
        this.res = res;
    }

    /**
     * @param t
     */
    public InvalidOptionValsFoundException(Throwable t) {
        super(t);
    }

    /**
     * @param s
     * @param t
     */
    public InvalidOptionValsFoundException(String s, Throwable t) {
        super(s, t);
    }

    public ConfigResponse getConfigResponse()
    {
        return res;
    }
}
