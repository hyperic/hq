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

import org.hyperic.hq.hqu.rendit.HQUPlugin
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.hqu.server.session.Attachment

class Plugin extends HQUPlugin {
    private boolean attachmentIsShown(Attachment a, Resource r, AuthzSubject u){
        // We are currently only functional for groups that contain at least
        // 1 platform
        if (r.isGroup()) {
            return r.getGroupMembers(u).find { m -> m.entityId.isPlatform() } != null
        }
        true
    }

    void initialize(File pluginDir) {
        super.initialize(pluginDir)
        addView(description:  'LiveExec',
                attachType:   'resource',
                toRoot:       false,
                platforms:    'all',
                byPlugin:     'system',
                controller:   LiveController,
                action:       'index',
                showAttachmentIf: {a, r, u -> attachmentIsShown(a, r, u)})        
    }
}
