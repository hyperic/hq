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

import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.appdef.shared.AIQueueManager
import org.hyperic.util.pager.PageControl
import org.hyperic.hq.appdef.shared.AIPlatformValue
import org.hyperic.hq.appdef.shared.AIQueueConstants
import org.hyperic.hq.appdef.server.session.Platform
import org.hyperic.hq.appdef.server.session.Server
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.context.Bootstrap;

class AutodiscoveryController extends BaseController {

    AutodiscoveryController() {
        onlyAllowSuperUsers()
        
        setXMLMethods(['list'])
    }

    def list(xmlResult, params) {

        String fqdn = params.getOne('fqdn')

        def list = Bootstrap.getBean(AIQueueManager.class).getQueue(user, true, true, 
                                       false, PageControl.PAGE_ALL)

        List matching = getMatchingPlatforms(list, fqdn)

        xmlResult.autodiscovery {
            xmlResult.platforms {
                matching.each { plat ->
                    xmlResult.platform(name: plat.name,
                                       fqdn: plat.fqdn,
                                       type: plat.platformTypeName) {
                        xmlResult.ips {
                            plat.aIIpValues.each { ip ->
                                xmlResult.ip(address: ip.address,
                                             mac: ip.mACAddress)
                            }
                        }
                        xmlResult.servers {
                            plat.aIServerValues.each { server ->
                                xmlResult.server(name: server.name,
                                                 type: server.serverTypeName)
                            }
                        }
                    }
                }
            }
        }
        xmlResult
    }

    def approve(params) {

        String fqdn = params.getOne('fqdn')

        AIQueueManager aiMan = Bootstrap.getBean(AIQueueManager.class)

        def list = aiMan.getQueue(user, true, true, 
                                  false, PageControl.PAGE_ALL)

        List matching = getMatchingPlatforms(list, fqdn)

        def res = new StringBuffer()
        for (plat in matching) {
            try {
                List imported = processPlatform(user, aiMan, plat)
                def numPlats = imported.findAll { it instanceof Platform }.size();
                def numServers = imported.findAll { it instanceof Server }.size();
                res.append("Processed platform '")
                   .append(plat.fqdn).append("' imported ")
                   .append(numPlats).append(" platforms")
                   .append(", ").append(numServers).append(" servers.\n")
            } catch (Exception e) {
                res.append("${e.message} while importing ${plat.name}\n")
            }
        }
        render(inline : res.toString())
    }

    private List processPlatform(AuthzSubject user,
                                 AIQueueManager aiMan,
                                 AIPlatformValue plat) {
        // If a platform is a placeholder, don't attempt to approve it.
        List platformIds = []
        if (plat.queueStatus != AIQueueConstants.Q_STATUS_PLACEHOLDER) {
            platformIds.add(plat.id)
        }

        // Only approve servers that are not marked ignored
        List serverIds = plat.AIServerValues.findAll { !it.ignored }.id

        // All IP changes get auto-approved
        List ipIds = plat.AIIpValues.id

        aiMan.processQueue(user, platformIds, serverIds, ipIds,
                           AIQueueConstants.Q_DECISION_APPROVE)
    }

    private List getMatchingPlatforms(List list, String fqdn) {
        // If no FQDN is passed in, match everything.
        if (fqdn == null) {
            fqdn = "regex:.*"
        }

        log.info("Getting platforms matching " + fqdn)

        def matcher = makeFQDNMatcher(fqdn)
        def matching = list.grep {
            AIPlatformValue plat -> matcher(plat)
        }
        matching
    }

    private Closure makeFQDNMatcher(String s) {
        if (s.startsWith('regex:')) {
            def regex = ~s[6..-1]
            return { AIPlatformValue plat -> plat.fqdn ==~ regex }
        } else {
            return { AIPlatformValue plat -> plat.fqdn == s }
        }
    }
}
