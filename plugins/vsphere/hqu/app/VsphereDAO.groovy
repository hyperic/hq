import org.hyperic.dao.DAOFactory
import org.hyperic.hq.appdef.server.session.Platform
import org.hyperic.hq.hqu.rendit.helpers.ResourceHelper
import org.hyperic.hq.authz.server.session.Resource
import org.hyperic.hq.appdef.Ip

/**
 * This DAO adds custom queries required for performant operation of the
 * Vsphere HQU plugin.
 * TODO: merge into HQ.
 */
class VsphereDAO {
    public static Platform getPlatformByMAC(String macAddress) {
        def sess = DAOFactory.getDAOFactory().currentSession

        // Both the VM and the Guest will have IP entries with the given MAC
        // address.  We filter out the VM platform using certdn == null rather
        // than join with EAM_PLATFORM_TYPE for performance.
        String hql = "select distinct p from Platform p " +
                     "join p.ips ip where ip.macAddress=? and p.certdn is not null";

        //TODO: Permission check
        return (Platform)sess.createQuery(hql).
                setString(0, macAddress).
                uniqueResult()
    }
    
    static getVirtualPlatformsWithNameLike(name) {
        def sess = DAOFactory.getDAOFactory().currentSession
        def hql = "from Platform p " +
                  "where (p.platformType.name = 'VMware vSphere VM' or p.platformType.name = 'VMware vSphere Host') and p.resource.name like ?"
         
        return sess.createQuery(hql)
                   .setString(0, "%" + name + "%")
                   .list()
    }

    static getHostResourceByVMResource(user, resource) {
        def config = resource.config
        def esxHost = config["esxHost"]
        def resourceHelper = new ResourceHelper(user)
        def hosts = resourceHelper.find(byPrototype: 'VMware vSphere Host')
        for (host in hosts) {
            if (host.name == esxHost['value']) {
                return host
            }
        }
        
        return null
    }
    
    public static List getVMsByHost(user, String hostname) {
        def sess = DAOFactory.getDAOFactory().currentSession
        def resourceHelper = new ResourceHelper(user)
        
        String sql = "SELECT p.APPDEF_ID " +
                "FROM EAM_CPROP_KEY k, EAM_CPROP p " +
                "WHERE k.APPDEF_TYPE = 1 and p.keyid = k.id and p.PROPVALUE = ?"

        def idList = sess.createSQLQuery(sql).setString(0, hostname).list()
        def vms = []

        for (id in idList) {
            vms << resourceHelper.find(platform:id)
        }
        
        vms
    }

    public static Resource getVMByPlatform(Platform p) {
        def sess = DAOFactory.getDAOFactory().currentSession

        for (Ip ip : p.getIps()) {
            if (ip.getMacAddress().equals("00:00:00:00:00:00")) {
                continue; // Minor optimization
            }

            String hql = "select distinct p from Platform p " +
                "join p.ips ip where ip.macAddress=? and p.certdn is null"

            Platform platform = (Platform)sess.createQuery(hql).
                setString(0, ip.getMacAddress()).
                uniqueResult()

            if (platform != null) {
                return platform.getResource()
            }
        }
        return null
    }
}