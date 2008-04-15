import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.appdef.shared.AIQueueManagerLocal
import org.hyperic.util.pager.PageControl
import org.hyperic.hq.appdef.server.session.AIQueueManagerEJBImpl as AIQMan
import org.hyperic.hq.appdef.shared.AIPlatformValue
import org.hyperic.hq.appdef.shared.AIQueueConstants
import org.hyperic.hq.appdef.server.session.Platform
import org.hyperic.hq.appdef.server.session.Server
import org.hyperic.hq.authz.server.session.AuthzSubject

class AutodiscoveryController extends BaseController {

    AutodiscoveryController() {
        onlyAllowSuperUsers()
        
        setXMLMethods(['list'])
    }

    def list(xmlResult, params) {

        String fqdn = params.getOne('fqdn')

        def list = AIQMan.one.getQueue(user.valueObject, true, true, 
                                       PageControl.PAGE_ALL)

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

        AIQueueManagerLocal aiMan = AIQMan.one

        def list = aiMan.getQueue(user.valueObject, true, true, 
                                  PageControl.PAGE_ALL)

        List matching = getMatchingPlatforms(list, fqdn)

        def res = new StringBuffer()
        for (plat in matching) {
            List imported = processPlatform(user, aiMan, plat)
            def numPlats = imported.findAll { it instanceof Platform }.size();
            def numServers = imported.findAll { it instanceof Server }.size();
            res.append("Processed platform '")
            .append(plat.fqdn).append("' imported ")
            .append(numPlats).append(" platforms")
            .append(", ").append(numServers).append(" servers.\n")
        }
        render(inline : res.toString())
    }

    private List processPlatform(AuthzSubject overlord,
                                 AIQueueManagerLocal aiMan,
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

        aiMan.processQueue(overlord.valueObject, platformIds, serverIds, ipIds,
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
