package org.hyperic.hq.ui.action.portlet.autoDisc;

import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.appdef.shared.AIQueueConstants;
import org.hyperic.hq.ui.action.BaseValidatorFormNG;

public class AIQueueFormNG extends BaseValidatorFormNG {

	
	   private Integer[] platformsToProcess;
	    private Integer[] serversToProcess;
	    private int queueAction = AIQueueConstants.Q_DECISION_DEFER;

	    public AIQueueFormNG() {
	       super();
	    }

	    public Integer[] getPlatformsToProcess() {
	        return platformsToProcess;
	    }

	    public void setPlatformsToProcess(Integer[] platforms) {
	        platformsToProcess = platforms;
	    }

	    public Integer[] getServersToProcess() {
	        return serversToProcess;
	    }

	    public void setServersToProcess(Integer[] servers) {
	        serversToProcess = servers;
	    }

	    public void setServersToProcess(List servers) {
	        serversToProcess = new Integer[servers.size()];
	        servers.toArray(serversToProcess);
	    }

	    public int getQueueAction() {
	        return queueAction;
	    }

	    public void setQueueAction(int a) {
	        queueAction = a;
	    }

	    public void reset() {
	    	super.reset();
	        platformsToProcess = new Integer[0];
	        serversToProcess = new Integer[0];
	    }

}
