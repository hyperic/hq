package org.hyperic.hq.ui.action.resource.common.monitor.visibility;

import java.io.InputStream;

import org.hyperic.hq.ui.json.JSONResult;
import org.hyperic.hq.ui.json.action.JsonActionContextNG;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.json.JSONObject;



@Component("jsonIndicatorChartsActionNG")
@Scope("prototype")
public class JsonIndecatorChartsActionNG extends IndicatorChartsActionNG{

	
	private InputStream inputStream;

	public InputStream getInputStream() {
		return inputStream;
	}
	
	
	@Override
	public String fresh() throws Exception {
		
		super.fresh();
		JsonActionContextNG ctx = this.setJSONContext();
		
		IndicatorDisplaySummary metric = metrics.get(metrics.size() - 1);
		JSONObject json = metric.toJSON();
		json.put("index", metrics.size() - 1);
		json.put("displaySize", indicatorViewForm.getDisplaySize());
		json.put("timeToken", indicatorViewForm.getTimeToken());

		JSONResult res = new JSONResult(json);
        ctx.setJSONResult(res);
		
        inputStream = this.streamJSONResult(ctx);
		
		return null;
	}
	
	
	

}
