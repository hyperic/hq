import org.hyperic.hq.hqapi1.ErrorCode
import org.hyperic.hq.authz.shared.PermissionException

class MaintenanceController extends ApiController {

    private Closure getMaintenanceEventXML(m) {

        { doc ->
            MaintenanceEvent(state:     m.state,
                             groupId:   m.groupId,
                             startTime: m.startTime,
                             endTime:   m.endTime) {
                State(m.state)
            }
        }
    }

    def schedule(params) {
        def groupId = params.getOne("groupId")?.toInteger()
        def start = params.getOne("start")?.toLong()
        def end = params.getOne("end")?.toLong()

        def failureXml = null

        if (groupId == null) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                       "Group id not given")
        }

        if (start == null || end == null) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                       "Maintenance window not specified")
        }

        if (end < start) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                       "End time < start time")
        }

        if (start < System.currentTimeMillis()) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                       "Start time cannot be in the past")
        }

        def result
        if (!failureXml) {
            def group = resourceHelper.findGroup(groupId)
            if (!group) {
                failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                           "Group with id " + groupId +
                                           " not found")
            } else {
                try {
                    result = group.scheduleMaintenance(user, start, end)
                } catch (UnsupportedOperationException e) {
                    failureXml = getFailureXML(ErrorCode.NOT_SUPPORTED)
                } catch (PermissionException e) {
                    failureXml = getFailureXML(ErrorCode.PERMISSION_DENIED)
                } catch (Exception e) {
                    failureXml = getFailureXML(ErrorCode.UNEXPECTED_ERROR,
                                               e.getMessage())
                }
            }
        }

        renderXml() {
            MaintenanceResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                    out << getMaintenanceEventXML(result)
                }
            }
        }
    }

    def unschedule(params) {
        def groupId = params.getOne("groupId")?.toInteger()

        def failureXml = null

        if (groupId == null) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                       "Group id not given")
        }

        if (!failureXml) {
            def group = resourceHelper.findGroup(groupId)
            if (!group) {
                failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                           "Group with id " + groupId +
                                           " not found")
            } else {
                try {
                    group.unscheduleMaintenance(user)
                } catch (UnsupportedOperationException e) {
                    failureXml = getFailureXML(ErrorCode.NOT_SUPPORTED)
                } catch (PermissionException e) {
                    failureXml = getFailureXML(ErrorCode.PERMISSION_DENIED)
                } catch (Exception e) {
                    failureXml = getFailureXML(ErrorCode.UNEXPECTED_ERROR,
                                               e.getMessage())
                }
            }
        }        

        renderXml() {
            StatusResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                }
            }
        }
    }

    def get(params) {
        def groupId = params.getOne("groupId")?.toInteger()

        def failureXml = null

        if (groupId == null) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS,
                                       "Group id not given")
        }

        def result
        if (!failureXml) {
            def group = resourceHelper.findGroup(groupId)
            if (!group) {
                failureXml = getFailureXML(ErrorCode.OBJECT_NOT_FOUND,
                                           "Group with id " + groupId +
                                           " not found")
            } else {
                try {
                    result = group.getMaintenanceEvent(user)
                } catch (UnsupportedOperationException e) {
                    failureXml = getFailureXML(ErrorCode.NOT_SUPPORTED)
                } catch (PermissionException e) {
                    failureXml = getFailureXML(ErrorCode.PERMISSION_DENIED)
                } catch (Exception e) {
                    failureXml = getFailureXML(ErrorCode.UNEXPECTED_ERROR,
                                               e.getMessage())
                }
            }
        }

        renderXml() {
            MaintenanceResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                    if (result) {
                        out << getMaintenanceEventXML(result)
                    }
                }
            }
        }
    }
}