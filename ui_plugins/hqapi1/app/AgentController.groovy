
import org.hyperic.hq.hqapi1.ErrorCode;

class AgentController extends ApiController {

    private Closure getAgentXML(a) {
        { doc ->
            Agent(id             : a.id,
                  address        : a.address,
                  port           : a.port,
                  version        : a.version,
                  unidirectional : a.unidirectional)
        }
    }

    private Closure getUpXML(boolean up) {
        { doc ->
            Up(up)
        }
    }

    def get(params) {
        def id = params.getOne("id")?.toInteger()
        def address = params.getOne("address")
        def port = params.getOne("port")?.toInteger()

        def failureXml
        def agent = getAgent(id, address, port)

        if (!agent) {
            failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                       "Unable to find agent with id=" + id +
                                       " address=" + address + " port=" + port)
        }

        renderXml() {
            out << AgentResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                    out << getAgentXML(agent)
                }
            }
        }
    }

    def list(params) {
        renderXml() {
            out << AgentsResponse() {
                out << getSuccessXML()
                for (agent in agentHelper.allAgents.sort {a, b -> a.address <=> b.address}) {
                    out << getAgentXML(agent)
                }
            }
        }
    }

    def ping(params) {
        def id = params.getOne('id')?.toInteger()

        def failureXml
        boolean up
        if (!id) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS)
        } else {
            try {
                def agent = getAgent(id, null, null)
                if (!agent) {
                    failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                               "Agent with id=" + id + " not found")

                } else {
                    up = agent.ping(user)
                }
            } catch (Exception e) {
                log.error("UnexpectedError: " + e.getMessage(), e);
                failureXml = getFailureXML(ErrorCode.UNEXPECTED_ERROR)
            }
        }

        renderXml() {
            out << PingAgentResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                    out << getUpXML(up)
                }
            }
        }
    }
}