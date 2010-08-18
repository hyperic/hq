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

package org.hyperic.hq.bizapp.server.session;

import java.util.ResourceBundle;

import org.hyperic.util.HypericEnum;

public class UpdateStatusMode 
    extends HypericEnum
{
    private static final String BUNDLE = "org.hyperic.hq.bizapp.Resources";
    
    public static final UpdateStatusMode ALL = 
        new UpdateStatusMode(0, "All", "update.mode.all");
    public static final UpdateStatusMode MAJOR = 
        new UpdateStatusMode(1, "Major", "update.mode.major");
    public static final UpdateStatusMode NONE = 
        new UpdateStatusMode(2, "None", "update.mode.none");
    
    private UpdateStatusMode(int code, String desc, String localeProp) {
        super(code, desc, localeProp, ResourceBundle.getBundle(BUNDLE));
    }
    
    public static UpdateStatusMode findByCode(int code) {
        return (UpdateStatusMode)findByCode(UpdateStatusMode.class, code);
    }
}
