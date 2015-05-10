package org.hyperic.hq.ui.json.action.escalation.crud;

import java.io.InputStream;
import java.util.Map;

import javax.servlet.ServletContext;

import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.json.action.JsonActionContextNG;
import org.springframework.stereotype.Component;


@Component(value = "removeActionNG")
public class RemoveActionNG extends BaseActionNG {

	
	private InputStream inputStream;

	public InputStream getInputStream() {
		return inputStream;
	}

	public String execute() throws Exception {
		JsonActionContextNG context = this.setJSONContext();
		
        ServletContext sctx = context.getServletContext();
        EventsBoss eBoss  = Bootstrap.getBean(EventsBoss.class);
        int sessId = context.getSessionId();

        Integer id    = context.getId();
        Map     map   = context.getParameterMap();
        Integer escId = Integer.valueOf(((String[])map.get("EscId"))[0]); 
        eBoss.removeAction(sessId, escId, id);

        inputStream = this.streamJSONResult(context);
		
		return null;
	}
}
