import org.hyperic.hq.livedata.server.session.LiveDataManagerEJBImpl as ldm
import org.hyperic.hq.livedata.shared.LiveDataCommand
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl as asm
import org.hyperic.hq.appdef.shared.AppdefEntityID
import org.hyperic.util.config.ConfigResponse

def liveMan  = ldm.one
def overlord = asm.one.overlordPojo
def ent      = AppdefEntityID.newPlatformID(10001)
def cmd      = new LiveDataCommand(ent, "top", new ConfigResponse())

//liveMan.getCommands(overlord, ent)
liveMan.getData(overlord, cmd).getXMLResult()
