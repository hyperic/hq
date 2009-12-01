import org.hyperic.hq.hqu.rendit.HQUPlugin

class Plugin extends HQUPlugin {
    Plugin() {
        addAdminView(true, '/console/index.hqu', 'Groovy Console')
    }
}
