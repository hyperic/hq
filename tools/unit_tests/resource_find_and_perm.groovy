import org.hyperic.hq.authz.shared.PermissionException
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl as asm
import org.hyperic.hq.hqu.rendit.helpers.ResourceHelper

def overlord = asm.one.overlordPojo
def guest = asm.one.findSubjectById(2)

def rhelp = new ResourceHelper(overlord)
def plat = rhelp.find(platform:10001)

assert plat != null, "You need at least 1 platform in inventory to run this test (10001)"

// as Overlord
assert rhelp.find(platform:10001, permCheck:false) == plat

// now as Guest
assert rhelp.find(platform:10001, user : guest, operation : 'view', permCheck: false) == plat
try {
   rhelp = new ResourceHelper(guest)
   rhelp.find(platform:10001)
   assert false, 'Invalid find did not throw exception'
} catch(PermissionException e) {
}

"Test Ran AOK"
