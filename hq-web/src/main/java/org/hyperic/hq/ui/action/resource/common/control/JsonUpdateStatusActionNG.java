package org.hyperic.hq.ui.action.resource.common.control;

import java.io.InputStream;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.control.server.session.ControlHistory;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.json.JSONResult;
import org.hyperic.hq.ui.json.action.JsonActionContextNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.stereotype.Component;
import org.hyperic.hq.ui.Constants;
import org.json.JSONObject;

@Component(value = "jsonUpdateStatusActionNG")
public class JsonUpdateStatusActionNG extends BaseActionNG {

    private final Log log = LogFactory.getLog(JsonUpdateStatusActionNG.class);
    @Resource
    private ControlBoss controlBoss;
    
	private InputStream inputStream;
    
    
    public String execute() throws Exception {
    	
    	JsonActionContextNG ctx = this.setJSONContext();
		
        HttpSession session = request.getSession();
        log.trace("determining current status.");
        int sessionId = RequestUtils.getSessionId(request).intValue();

        AppdefEntityID appId = RequestUtils.getEntityId(request);

        Integer batchId = null;
        try {
            batchId = RequestUtils.getIntParameter(request, Constants.CONTROL_BATCH_ID_PARAM);
        }
        /* failed to get that param, that's ok, use current */
        catch (NullPointerException npe) {
        	log.error(npe,npe);
        } catch (ParameterNotFoundException pnfe) {
        	log.error(pnfe,pnfe);
        } catch (NumberFormatException nfe) {
        	log.error(nfe,nfe);
        }

        ControlHistory cValue = null;
        if (null == batchId) {
            cValue = controlBoss.getCurrentJob(sessionId, appId);
        } else {
            cValue = controlBoss.getJobByJobId(sessionId, batchId);
        }

        if (cValue == null /* no current job */) {
            cValue = controlBoss.getLastJob(sessionId, appId);
        }
        JSONObject obj = new JSONObject();
        obj.put("ctrlAction", cValue.getAction());
        obj.put("ctrlDesc", cValue.getDescription());
        obj.put("ctrlStatus", cValue.getStatus());
        obj.put("ctrlStart", cValue.getStartTime());
        obj.put("ctrlMessage", cValue.getMessage());
        obj.put("ctrlSched", cValue.getDateScheduled());
        obj.put("ctrlDuration", cValue.getDuration());
		JSONResult jsonRes = new JSONResult(obj);
		ctx.setJSONResult(jsonRes);
		
		this.inputStream = this.streamJSONResult(ctx);
    	
    	return null;
    }


	public InputStream getInputStream() {
		return inputStream;
	}
    
    
}
