/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.ui.action.portlet.autoDisc;

import java.util.Arrays;
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
	        //servers.toArray(serversToProcess);
	        for (int cnt=0; cnt < servers.size() ; cnt++){
	        	Object element = servers.get(cnt);
	        	if (element instanceof String ) {
	        		serversToProcess[cnt] = Integer.valueOf( (String) element) ;
	        	} else {
	        		serversToProcess[cnt] = (Integer) element;
	        	}
	        }
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

		@Override
		public String toString() {
			String outcome =  "AIQueueFormNG [";
			if (platformsToProcess != null) {
				outcome += "platformsToProcess ";
				outcome += Arrays.toString(platformsToProcess);
			}
			if (serversToProcess != null) {
				outcome += "serversToProcess ";
				outcome += Arrays.toString(serversToProcess);
			}
			outcome += "queueAction ";
			outcome += queueAction;
			outcome += "]";		
			return outcome;
		}
	    
	    

}
