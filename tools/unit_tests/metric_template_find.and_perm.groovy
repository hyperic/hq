import org.hyperic.hq.authz.shared.PermissionException
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl as asm
import org.hyperic.hq.hqu.rendit.helpers.MetricHelper

def overlord = asm.one.overlordPojo
def guest = asm.one.findSubjectById(2)

def mhelp = new MetricHelper(overlord)

try {
    mhelp.find(all: 'templates', user: guest)
    assert false, "Guest find of templates didn't throw an exception"
} catch(PermissionException e) {
}

try {
    mhelp.find(all: 'templates', user: guest, permCheck: true)
    assert false, "Guest find of templates didn't throw an exception"
} catch(PermissionException e) {
}


mhelp.find(all: 'templates', user: guest, permCheck: false)

mhelp.find(all: 'templates')
