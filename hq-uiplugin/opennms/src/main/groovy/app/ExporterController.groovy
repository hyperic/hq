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

import java.text.SimpleDateFormat
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hibernate.PageInfo
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.authz.server.session.ResourceSortField


class ExporterController 
	extends BaseController
{
    def ExporterController() {
        setXMLMethods(['list'])
    }
    
    def list(xml, params) {
        def formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        def platforms = resourceHelper.findPlatforms(new PageInfo(ResourceSortField.NAME, true));
        def man = Bootstrap.getBean(PlatformManager.class)
        xml.'model-import'('foreign-source':'HQ', 'date-stamp':formatter.format(new Date())) {
            platforms.each { res ->   
                def p = man.findPlatformById(res.instanceId)
                node('node-label':p.fqdn, 'foreign-id':p.id) {
                     'interface'('ip-addr': p.agent.address, descr: 'agent-address',
                                 status: 1, 'snmp-primary': 'N') {
                        'monitored-service'('service-name': 'ICMP')
                        'monitored-service'('service-name': 'HypericAgent')
                    }
                }
            }
        }

       xml
    }
}
