/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
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
 *
 */
package org.hyperic.hq.web.dashboard;

import org.hyperic.hq.ui.Constants;

/**
 * This interface provides public string constants that map to user preferences
 * used in by the dashbaord.
 * 
 * String should be ordered alphabetical for ease of maintenance.
 * 
 * @author David Crutchfield
 * 
 */
public interface UserPreferenceKeys {
	public final static String WIDE_PORTLETS = Constants.USER_PORTLETS_SECOND;
	public final static String NARROW_PORTLETS = Constants.USER_PORTLETS_FIRST;
	public final static String WIDE_PORTLETS_NG = Constants.USER_PORTLETS_SECOND_NG;
	public final static String NARROW_PORTLETS_NG = Constants.USER_PORTLETS_FIRST_NG;
}